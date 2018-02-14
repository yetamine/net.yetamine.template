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
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Default implementation of {@link TemplateFormat} interface.
 *
 * <p>
 * This implementation supports two kinds of templates:
 *
 * <ul>
 * <li>The standard form uses placeholders with a pair of brackets where the
 * opening bracket can be escaped. The closing bracket can't be escaped, but a
 * placeholder can contain any characters, except for the sequence equal to the
 * closing bracket</li>
 *
 * <li>The reduced form uses placeholders without brackets where a placeholder
 * starts with an opening sequence, which can be escaped, and ends at the point
 * that depends on some condition. The implementation uses {@link IntPredicate}
 * for testing characters and stops at the first character that does not pass
 * the predicate.</li>
 * </ul>
 *
 * <p>
 * The nature of the format excludes {@link TemplateSyntaxException} as any
 * input can be parsed as a valid template. A custom {@link TemplateCallback}
 * implementation used with the parser may still throw such an exception when
 * some fragment does not satisfy additional constraints.
 */
public final class Interpolation implements TemplateFormat {

    /** Standard default interpolation instance. */
    private static final TemplateFormat STANDARD = new Interpolation("${", "}", "$");
    /** Reduced default interpolation instance. */
    private static final TemplateFormat REDUCED = new Interpolation("$", Interpolation::isReducedSymbolCharacter, "$");

    /** Definition of the closing boundary. */
    private final TokenScanner<? extends Symbol> scanner;
    /** Placeholder opening. */
    private final String opening;
    /** Placeholder escaping. */
    private final String escaping;
    /** Placeholder closing. */
    private final String closing;

    /**
     * Scanner for finding the end of a closing sequence.
     */
    @FunctionalInterface
    private interface ClosingScanner {

        /**
         * Finds the end of a closing sequence.
         *
         * @param input
         *            the input to scan. It must not be {@code null}.
         * @param offset
         *            the offset where to scan from. It must not be negative and
         *            it must not be greater than the length of the input.
         *
         * @return the index where the closing sequence ends, or -1 if not found
         */
        int find(String input, int offset);
    }

    /**
     * Creates a new instance.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderClosing
     *            the placeholder closing. It must be a non-empty string.
     * @param placeholderEscaping
     *            the placeholder escaping sequence. It must not contain the
     *            placeholder opening, except for the case of being empty or
     *            equal to it.
     */
    private Interpolation(String placeholderOpening, String placeholderClosing, String placeholderEscaping) {
        Objects.requireNonNull(placeholderClosing); // Not covered by the checks executed by SymbolScanner
        final ClosingScanner closingScanner = (input, offset) -> input.indexOf(placeholderClosing, offset);
        scanner = new SymbolScanner(placeholderOpening, placeholderClosing, closingScanner, placeholderEscaping);
        escaping = placeholderEscaping;
        opening = placeholderOpening;
        closing = placeholderClosing;
    }

    /**
     * Creates a new instance.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderCharacters
     *            the placeholder closing predicate that tests if a character
     *            belongs to a placeholder. It must not be {@code null}.
     * @param placeholderEscaping
     *            the placeholder escaping sequence. It must not contain the
     *            placeholder opening, except for the case of being empty or
     *            equal to it.
     */
    private Interpolation(String placeholderOpening, IntPredicate placeholderCharacters, String placeholderEscaping) {
        Objects.requireNonNull(placeholderCharacters); // Not covered by the checks executed by SymbolScanner
        final ClosingScanner closingScanner = (input, offset) -> {
            return reducedClosingScanner(input, offset, placeholderOpening, placeholderEscaping, placeholderCharacters);
        };

        scanner = new SymbolScanner(placeholderOpening, null, closingScanner, placeholderEscaping);
        escaping = placeholderEscaping;
        opening = placeholderOpening;
        closing = null;
    }

    /**
     * Creates a new instance.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderCharacters
     *            the placeholder closing predicate that tests if a character
     *            belongs to a placeholder. It must not be {@code null}.
     * @param placeholderEscaping
     *            the placeholder escaping sequence. It must not contain the
     *            placeholder opening, except for the case of being empty or
     *            equal to it.
     *
     * @return the new instance
     */
    public static TemplateFormat with(String placeholderOpening, IntPredicate placeholderCharacters, String placeholderEscaping) {
        if (placeholderEscaping.isEmpty()) {
            throw new IllegalArgumentException("Empty escaping sequence not permitted.");
        }

        return new Interpolation(placeholderOpening, placeholderCharacters, placeholderEscaping);
    }

    /**
     * Creates a new instance.
     *
     * <p>
     * Unlike {@link #with(String, IntPredicate, String)}, this method provides
     * an instance with no escaping capability, hence {@link #constant(String)}
     * returns its argument without any escaping. Use such a setup carefully,
     * because no real constants exist.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderCharacters
     *            the placeholder closing predicate that tests if a character
     *            belongs to a placeholder. It must not be {@code null}.
     *
     * @return the new instance
     */
    public static TemplateFormat with(String placeholderOpening, IntPredicate placeholderCharacters) {
        return new Interpolation(placeholderOpening, placeholderCharacters, "");
    }

    /**
     * Creates a new instance.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderClosing
     *            the placeholder closing. It must be a non-empty string.
     * @param placeholderEscaping
     *            the placeholder escaping sequence. It must not contain the
     *            placeholder opening, except for the case of being empty or
     *            equal to it.
     *
     * @return the new instance
     */
    public static TemplateFormat with(String placeholderOpening, String placeholderClosing, String placeholderEscaping) {
        if (placeholderEscaping.isEmpty()) {
            throw new IllegalArgumentException("Empty escaping sequence not permitted.");
        }

        return new Interpolation(placeholderOpening, placeholderClosing, placeholderEscaping);
    }

    /**
     * Creates a new instance.
     *
     * <p>
     * Unlike {@link #with(String, String, String)}, this method provides an
     * instance with no escaping capability, hence {@link #constant(String)}
     * returns its argument without any escaping. Use such a setup carefully,
     * because no real constants exist.
     *
     * @param placeholderOpening
     *            the placeholder opening. It must be a non-empty string.
     * @param placeholderClosing
     *            the placeholder closing. It must be a non-empty string.
     *
     * @return the new instance
     */
    public static TemplateFormat with(String placeholderOpening, String placeholderClosing) {
        return new Interpolation(placeholderOpening, placeholderClosing, "");
    }

    /**
     * Returns the standard format with <code>${</code> and <code>}</code>
     * brackets for placeholders and <code>$</code> as the escaping sequence.
     *
     * @return the standard format with brackets
     */
    public static TemplateFormat standard() {
        return STANDARD;
    }

    /**
     * Returns the reduced format with <code>$</code> for opening a placeholder
     * which ends on the opening sequence, at the end of the input or when any
     * character outside {@code [A-Za-z0-9_]} set is encountered. Doubling the
     * placeholder opening, e.g., <code>$$not_a_placeholder</code>, escapes the
     * placeholder.
     *
     * @return the reduced format without brackets
     */
    public static TemplateFormat reduced() {
        return REDUCED;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return (closing != null)
                ? String.format("Interpolation[opening=%s, escaping=%s, closing=%s]", opening, closing, escaping)
                : String.format("Interpolation[opening=%s, escaping=%s]", opening, escaping);
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
            if (!opening.equals(o.opening) || !escaping.equals(o.escaping) || !Objects.equals(closing, o.closing)) {
                return false;
            }

            // Avoid comparing lambdas (which should be typical), unless we have to
            return ((closing != null) || scanner.equals(o.scanner));
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
        if (escaping.isEmpty()) { // Honor the contract
            throw new UnsupportedOperationException();
        }

        return escape(string);
    }

    /**
     * @see net.yetamine.template.TemplateFormat#reproduction(java.lang.String)
     */
    public Optional<String> reproduction(String string) {
        return escaping.isEmpty() ? Optional.empty() : Optional.of(escape(string));
    }

    /**
     * @see net.yetamine.template.TemplateFormat#parser(java.lang.String)
     */
    public TemplateParser parser(String template) {
        return new TokenParser(scanner, template);
    }

    /**
     * Formats the given string as a constant under the assumption that
     * {@link #escaping} provides a non-empty sequence.
     *
     * @param string
     *            the input to represent as a constant. It must not be
     *            {@code null}.
     *
     * @return a constant representing the given string
     */
    private String escape(String string) {
        assert !escaping.isEmpty(); // Otherwise it returns a copy of the input

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
     * Implements a closing sequence scanner that uses the given character
     * predicate to determine the result.
     *
     * @param input
     *            the input to scan. It must not be {@code null}.
     * @param offset
     *            the offset where to scan from. It must not be negative and it
     *            must not be greater than the length of the input.
     *
     * @param opening
     *            the opening sequence. It must be a non-empty string.
     * @param escaping
     *            the escaping sequence. It must be a non-empty string or
     *            {@code null} if none given.
     * @param closing
     *            the closing sequence predicate. It must not be {@code null}.
     *
     * @return the index where the closing sequence ends, or -1 if not found
     */
    private static int reducedClosingScanner(String input, int offset, String opening, String escaping, IntPredicate closing) {
        for (int i = offset; i < input.length(); i++) {
            if (!closing.test(input.charAt(i))) {
                return i;
            }

            final int openingLength = opening.length();
            if (input.regionMatches(i, opening, 0, openingLength)) {
                return i;
            }

            if (escaping == null) {
                continue;
            }

            // @formatter:off
            final int escapingLength = escaping.length();
            if ((input.regionMatches(i, escaping, 0, escapingLength) && input.regionMatches(i + escapingLength, opening, 0, openingLength))) {
                return i;
            }
            // @formatter:on
        }

        return input.length();
    }

    /**
     * Tests if the given character is a part of a token.
     *
     * @param character
     *            the character to test
     *
     * @return {@code true} if the character is in the set {@code [A-Za-z0-9_]}
     */
    private static boolean isReducedSymbolCharacter(int character) {
        // @formatter:off
        return ((('a' <= character) && (character <= 'z'))
                || (('A' <= character) && (character <= 'Z'))
                || (('0' <= character) && (character <= '9'))
                || (character == '_')
               );
        // @formatter:on
    }

    /**
     * Implementation of {@link TokenScanner} that combines fixed placeholder
     * parts with a {@link ClosingScanner} to extract symbols from the input.
     */
    private static final class SymbolScanner implements TokenScanner<Symbol> {

        /** Placeholder opening. */
        private final String opening;
        /** Placeholder escaping. */
        private final String escaping;
        /** Length of the closing sequence. */
        private final int closingLength;
        /** Closing sequence scanner. */
        private final ClosingScanner closingScanner;
        /** Precomputed escape for the opening sequence. */
        private final Symbol escaped;

        /**
         * Creates a new instance.
         *
         * @param symbolOpening
         *            the opening sequence. It must be a non-empty string.
         * @param symbolClosing
         *            the closing sequence
         * @param referenceClosingScanner
         *            the scanner for finding the end of a symbol
         * @param symbolEscaping
         *            the escaping sequence. It must not contain the opening
         *            sequence, except for the case of being empty or equal to
         *            it.
         */
        public SymbolScanner(String symbolOpening, String symbolClosing, ClosingScanner referenceClosingScanner, String symbolEscaping) {
            if (symbolOpening.isEmpty()) {
                throw new IllegalArgumentException("Opening sequence must not be empty.");
            }

            closingLength = (symbolClosing != null) ? symbolClosing.length() : 0;
            closingScanner = Objects.requireNonNull(referenceClosingScanner);
            opening = symbolOpening;
            escaping = escaping(symbolEscaping, opening);
            escaped = TemplateConstant.from(symbolEscaping + opening, opening); // Precomputed escaped opening
        }

        /**
         * @see net.yetamine.template.TokenScanner#find(java.lang.String, int)
         */
        public Token<Symbol> find(String input, int offset) {
            final int openingIndex = input.indexOf(opening, offset);
            if (openingIndex == -1) { // No symbol found
                return null;
            }

            if (escaping == null) {
                // When escaping equals opening, check it forward
                final int metaLength = opening.length();
                if (input.regionMatches(openingIndex + metaLength, opening, 0, metaLength)) {
                    final int afterSymbol = openingIndex + 2 * metaLength;
                    return new Token<>(escaped, openingIndex, afterSymbol);
                }
            } else if (!escaping.isEmpty()) {
                // When a non-empty escaping, check it backward
                final int symbolIndex = openingIndex - escaping.length();
                if ((offset <= symbolIndex) && input.regionMatches(symbolIndex, escaping, 0, escaping.length())) {
                    final int afterSymbol = openingIndex + opening.length();
                    return new Token<>(escaped, symbolIndex, afterSymbol);
                }
            }

            // Check the presence of a subsequent closing sequence
            final int afterOpening = openingIndex + opening.length();
            final int closingIndex = closingScanner.find(input, afterOpening);
            if (closingIndex == -1) { // Missing closing sequence, make a constant again
                return new Token<>(escaped, openingIndex, afterOpening);
            }

            // Regular reference
            final String value = input.substring(afterOpening, closingIndex);
            final int afterSymbol = closingIndex + closingLength; // Skip the closing sequence
            final String definition = input.substring(openingIndex, afterSymbol);
            return new Token<>(TemplateReference.from(definition, value), openingIndex, afterSymbol);
        }

        /**
         * Checks the escaping sequence validity and returns the effective
         * escaping sequence.
         *
         * @param escaping
         *            the given escaping sequence. It must not be {@code null}.
         * @param opening
         *            the opening sequence. It must not be {@code null}.
         *
         * @return the given escaping, or {@code null} if equal to the opening
         *         sequence
         */
        private static String escaping(String escaping, String opening) {
            if (escaping.equals(opening)) {
                return null;
            }

            if (escaping.contains(opening)) {
                final String f = "Escaping sequence '%s' must not contain the opening sequence '%s'.";
                throw new IllegalArgumentException(String.format(f, escaping, opening));
            }

            return escaping;
        }
    }
}
