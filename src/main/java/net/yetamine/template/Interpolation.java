/*
 * Copyright 2017 Yetamine
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

/**
 * Default implementation of {@link TemplateFormat} interface.
 *
 * <p>
 * This implementation allows templates with references surrounded with a pair
 * of non-empty brackets, where the opening bracket occurrence can be escaped.
 * The nature of the format excludes {@link TemplateSyntaxException} as any
 * input can be parsed as a valid template. A custom {@link TemplateCallback}
 * implementation used with the parser may still throw such an exception when
 * some fragment does not satisfy additional constraints.
 */
public final class Interpolation implements TemplateFormat {

    /** Standard default interpolation instance. */
    private static final TemplateFormat STANDARD = new Interpolation("${", "}", "$");

    /** Placeholder opening. */
    final String opening;
    /** Placeholder closing. */
    final String closing;
    /** Placeholder escaping. */
    final String escaping;

    /**
     * Creates a new instance.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderClosing
     *            the placeholder closing. It must be a non-empty string.
     * @param placeholderEscaping
     *            the placeholder escaping sequence. It must be a non-empty
     *            string that does not contain the placeholder opening.
     */
    private Interpolation(String placeholderOpening, String placeholderClosing, String placeholderEscaping) {
        if (placeholderOpening.isEmpty() || placeholderClosing.isEmpty()) {
            throw new IllegalArgumentException("Both opening and closing brackets must not be empty.");
        }

        if (placeholderEscaping.contains(placeholderOpening)) {
            final String f = "Escaping sequence '%s' must not contain the opening bracket '%s'.";
            throw new IllegalArgumentException(String.format(f, placeholderEscaping, placeholderOpening));
        }

        if (placeholderEscaping.isEmpty()) {
            throw new IllegalArgumentException("Escaping sequence must not be empty.");
        }

        escaping = placeholderEscaping;
        closing = placeholderClosing;
        opening = placeholderOpening;
    }

    /**
     * Creates a new instance.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderClosing
     *            the placeholder closing. It must be a non-empty string.
     * @param placeholderEscaping
     *            the placeholder escaping sequence. It must be a non-empty
     *            string that does not contain the placeholder opening.
     *
     * @return the new instance
     */
    public static TemplateFormat with(String placeholderOpening, String placeholderClosing, String placeholderEscaping) {
        return new Interpolation(placeholderOpening, placeholderClosing, placeholderEscaping);
    }

    /**
     * Returns the standard format with <code>${</code> and <code>}</code>
     * brackets for placeholders and {@code $} as the escaping sequence.
     *
     * @return the standard format
     */
    public static TemplateFormat standard() {
        return STANDARD;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Interpolation[opening=%s, closing=%s, escaping=%s]", opening, closing, escaping);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Interpolation) {
            final Interpolation o = (Interpolation) obj;
            return opening.equals(o.opening) && closing.equals(o.closing) && escaping.equals(o.escaping);
        }

        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(opening, closing, escaping);
    }

    /**
     * @see net.yetamine.template.TemplateFormat#constant(java.lang.String)
     */
    public String constant(String string) {
        int next = string.indexOf(opening);
        if (next == -1) {
            return string;
        }

        final StringBuilder result = new StringBuilder();
        for (int last = 0; last < string.length();) {
            result.append(string.substring(last, next));
            result.append(escaping).append(opening);
            last = next + opening.length();
            next = string.indexOf(opening, last);

            if (next == -1) { // No more opening sequences
                result.append(string.substring(last));
                break;
            }
        }

        return result.toString();
    }

    /**
     * @see net.yetamine.template.TemplateFormat#parser(java.lang.String)
     */
    public TemplateParser parser(String template) {
        return new ParserImplementation(template);
    }

    /**
     * Implements {@link TemplateParser}.
     */
    private final class ParserImplementation implements TemplateParser {

        /** Input to parse. */
        private final String input;

        /**
         * Position to resume parsing on the subsequent {@link #next()}
         * invocation. Always {@code 0 <= position <= input.length()}.
         */
        private int position;

        /* Implementation notes:
         *
         * Both definitionOpen and definitionDone bounds the definition in the template,
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
         * 1 ~ definitionOpening
         * 2 ~ definitionClosing
         *
         * Here, symbol = "reference" and constant = false.
         *
         *
         * Example 2:
         *
         * "Some $${constant} in a string."
         *       ^  ^
         *       1  2
         *
         * Both 1 and 2 remains as above, symbol = "${" (i.e., the escaping sequence
         * has been dropped here from the symbol definition) and constant = true. If
         * using the same opening and closing sequence, nothing changes.
         */

        /** Recent symbol. */
        private String symbol;
        /** Position of {@link #symbol}. */
        private int definitionOpen;
        /** Position after {@link #symbol}. */
        private int definitionDone;
        /** Indicates that {@link #symbol} is a constant */
        private boolean constant;

        /** Parsing done. */
        private boolean done;

        /**
         * @param template
         */
        public ParserImplementation(String template) {
            input = Objects.requireNonNull(template);
        }

        /**
         * @see net.yetamine.template.TemplateParser#next(net.yetamine.template.TemplateCallback)
         */
        public <R> R next(TemplateCallback<? extends R> callback) {
            Objects.requireNonNull(callback); // Actually not necessary
            assert ((0 <= position) && (position <= input.length()));
            assert ((0 <= definitionOpen) && (definitionOpen <= definitionDone) && (definitionDone <= input.length()));

            if (done) {
                return callback.none();
            }

            if (position == input.length()) { // Reached the end, but not indicated 'done' yet
                final R result = input.isEmpty() ? callback.literal(input) : callback.none();
                done = true; // Do not emit anything anymore
                return result;
            }

            if (position == definitionDone) { // Behind a symbol, finding the next one
                search(position);
            }

            if (position == definitionOpen) { // At the beginning of a symbol
                final String definition = input.substring(definitionOpen, definitionDone);
                assert (symbol != null); // Should not be when the symbol has been defined here

                final R result = constant                               // @formatter:break
                        ? callback.constant(definition, symbol)         // @formatter:break
                        : callback.reference(definition, symbol);

                position = definitionDone;
                return result;
            }

            assert (position < definitionOpen); // Emit a non-empty literal up to the next symbol
            final R result = callback.literal(input.substring(position, definitionOpen));
            position = definitionOpen;
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

        /**
         * Finds the next symbol and sets symbol parsing state accordingly.
         *
         * @param from
         *            the index to start the search from
         */
        private void search(int from) {
            definitionOpen = input.indexOf(opening, from);

            if (definitionOpen == -1) { // No opening sequence, no symbol at all
                definitionOpen = input.length();
                definitionDone = definitionOpen;
                constant = false;
                symbol = null;
                return;
            }

            // Check the presence of the escaping sequence
            final int offset = definitionOpen - escaping.length();
            if ((from <= offset) && input.regionMatches(offset, escaping, 0, escaping.length())) {
                // Escaping and opening sequences reduce to a constant with the opening sequence
                definitionDone = definitionOpen + opening.length();
                definitionOpen = offset;
                symbol = opening;
                constant = true;
                return;
            }

            // Check the presence of a subsequent closing sequence
            final int afterOpening = definitionOpen + opening.length();
            definitionDone = input.indexOf(closing, afterOpening);
            if (definitionDone == -1) { // Missing closing sequence, make a constant again
                definitionDone = afterOpening;
                symbol = opening;
                constant = true;
                return;
            }

            // A regular reference, extract the reference from the symbol
            symbol = input.substring(afterOpening, definitionDone);
            definitionDone += closing.length(); // Include the closing in the definition
            constant = false;
        }
    }
}
