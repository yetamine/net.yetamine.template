/*
 * Copyright 2018 Yetamine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.yetamine.template;

import java.util.Objects;
import java.util.Optional;

/**
 * Generic {@link TemplateParser} which uses a {@link TokenScanner} to find
 * symbols in a template representation and assembles the template from the
 * found tokens and non-symbolic literals remaining between the tokens.
 */
public final class TokenParser implements TemplateParser {

    /** Scanner for the symbols. */
    private final TokenScanner<? extends Symbol> scanner;
    /** Input to parse with the scanner. */
    private final String input;

    /**
     * Position to resume parsing on the subsequent {@link #next()} invocation.
     * Always {@code 0 <= position <= input.length()}.
     */
    private int position;

    /* Implementation notes:
     *
     * Both symbolOpen and symbolDone bounds the definition in the template,
     * while symbol holds the symbol to be emitted and constant tells whether the
     * value represents a constant or an unresolved symbolic reference. It allows
     * using the same mechanism for various types of constants and works even for
     * same opening and closing sequences.
     *
     * Example 1:
     *
     * "Some ${reference} in a string."
     *       ^           ^
     *       1           2
     *
     * 1 ~ symbolOpen
     * 2 ~ symbolDone
     *
     * symbol = [value="reference", constant = false].
     *
     *
     * Example 2:
     *
     * "Some $${constant} in a string."
     *       ^  ^
     *       1  2
     *
     * Both 1 and 2 remains as above, symbol = [value="${", constant=true]
     */

    /** Recent symbol. */
    private Symbol symbol;
    /** Position of {@link #symbol}. */
    private int symbolOpen;
    /** Position after {@link #symbol}. */
    private int symbolDone;

    /** Parsing done. */
    private boolean done;

    /**
     * Creates a new instance.
     *
     * @param symbolScanner
     *            the scanner for finding symbols. It must not be {@code null}.
     * @param template
     *            the template to parse. It must not be {@code null}.
     */
    public TokenParser(TokenScanner<? extends Symbol> symbolScanner, String template) {
        scanner = Objects.requireNonNull(symbolScanner);
        input = Objects.requireNonNull(template);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("TemplateParser[input=%s, position=%d, scanner=%s]", input, position, scanner);
    }

    /**
     * @see net.yetamine.template.TemplateParser#next(net.yetamine.template.TemplateCallback)
     */
    public <R> R next(TemplateCallback<? extends R> callback) {
        Objects.requireNonNull(callback); // Actually not necessary
        assert ((0 <= position) && (position <= input.length()));
        assert ((0 <= symbolOpen) && (symbolOpen <= symbolDone) && (symbolDone <= input.length()));

        if (done) {
            return callback.none();
        }

        if (position == input.length()) { // Reached the end, but not indicated 'done' yet
            final R result = input.isEmpty() ? callback.literal(input) : callback.none();
            done = true; // Do not emit anything anymore
            return result;
        }

        if (position == symbolDone) { // Behind a symbol, finding the next one
            symbol = Optional.ofNullable(scanner.find(input, position)).map(token -> {
                symbolOpen = token.from();
                symbolDone = token.to();
                return token.value();

            }).orElseGet(() -> {
                symbolOpen = input.length();
                symbolDone = symbolOpen;
                return null;
            });
        }

        if (position == symbolOpen) { // At the beginning of a symbol
            assert (symbol != null);
            final String val = symbol.value();
            final String def = symbol.toString();
            final R result = symbol.constant() ? callback.constant(def, val) : callback.reference(def, val);
            position = symbolDone;
            return result;
        }

        assert (position < symbolOpen); // Emit a non-empty literal up to the next symbol
        final R result = callback.literal(input.substring(position, symbolOpen));
        position = symbolOpen;
        return result;
    }

    /**
     * @see net.yetamine.template.Parser#done()
     */
    public boolean done() {
        return done;
    }

    /**
     * @see net.yetamine.template.Parser#position()
     */
    public int position() {
        return position;
    }

    /**
     * @see net.yetamine.template.Parser#input()
     */
    public String input() {
        return input;
    }
}
