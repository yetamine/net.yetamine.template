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
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a template source text together with the information whether the
 * source should be treated as a template literal or as a template which needs
 * further processing before using.
 *
 * <p>
 * This class itself carries the information about the source text, but does not
 * enforce desired use and provides no information about the template format. It
 * should be derived from the context.
 */
public final class Source {

    /** Source text. */
    private final String value;
    /** Indication whether the {@link #value} should be treated as a literal. */
    private final boolean literal;

    /**
     * Creates a new instance.
     *
     * @param val
     *            the value to encapsulate. It must not be {@code null}.
     * @param lit
     *            {@code true} if the value is a literal
     */
    private Source(String val, boolean lit) {
        value = Objects.requireNonNull(val);
        literal = lit;
    }

    /**
     * Creates a new instance for a literal.
     *
     * @param literal
     *            the literal to encapsulate. It must not be {@code null}.
     *
     * @return the new instance
     */
    public static Source literal(String literal) {
        return new Source(literal, true);
    }

    /**
     * Creates a new instance for a template.
     *
     * @param template
     *            the template to encapsulate. It must not be {@code null}.
     *
     * @return the new instance
     */
    public static Source template(String template) {
        return new Source(template, false);
    }

    /**
     * Parses the given text according to the provided template format and
     * depending on the template symbols returns the appropriate instance.
     *
     * @param text
     *            the text to parse. It must not be {@code null}.
     * @param format
     *            the format to use. It must not be {@code null}.
     *
     * @return the new instance
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    public static Source parse(String text, TemplateFormat format) {
        return new Source(text, isLiteral(format, text));
    }

    /**
     * Tries to {@link #parse(String, TemplateFormat)} the given text and
     * returns the result if successful, while it returns an empty result rather
     * than throwing an exception on failure.
     *
     * @param text
     *            the text to parse. It must not be {@code null}.
     * @param format
     *            the format to use. It must not be {@code null}.
     *
     * @return the result of parsing, or an empty result if the parsing failed
     */
    public static Optional<Source> parseable(String text, TemplateFormat format) {
        try {
            return Optional.of(parse(text, format));
        } catch (TemplateSyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Source) {
            final Source o = (Source) obj;
            return (o.literal == literal) && value.equals(o.value);
        }

        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(literal, value);
    }

    /**
     * Returns the literal if {@link #isLiteral()}.
     *
     * @return the literal, or {@code null} if not representing a literal
     */
    public String literal() {
        return isLiteral() ? value : null;
    }

    /**
     * Indicates whether this instance represents a literal.
     *
     * @return {@code true} iff representing a literal
     */
    public boolean isLiteral() {
        return literal;
    }

    /**
     * Returns the literal.
     *
     * @return the literal if representing the literal
     */
    public Optional<String> asLiteral() {
        return isLiteral() ? Optional.of(value) : Optional.empty();
    }

    /**
     * Returns the template if {@link #isTemplate()}.
     *
     * @return the template, or {@code null} if not representing a template
     */
    public String template() {
        return isLiteral() ? null : value;
    }

    /**
     * Indicates whether this instance represents a template.
     *
     * @return {@code true} iff representing a template
     */
    public boolean isTemplate() {
        return !literal;
    }

    /**
     * Returns the template.
     *
     * @return the template if representing the template
     */
    public Optional<String> asTemplate() {
        return isLiteral() ? Optional.empty() : Optional.of(value);
    }

    /**
     * Returns the template parsed with the given format.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     *
     * @return the template
     *
     * @throws TemplateSyntaxException
     *             if parsing the template failed
     */
    public Template toTemplate(TemplateFormat format) {
        return literal ? TemplateLiteral.of(value) : format.parse(value);
    }

    /**
     * Returns the template parsed with the given format.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     *
     * @return the template
     *
     * @throws TemplateSyntaxException
     *             if parsing the template failed
     */
    public TemplateDefinition toDefinition(TemplateFormat format) {
        return TemplateDefinition.parsed(toTemplate(format), format);
    }

    /**
     * Returns the given template fragments parsed with {@link TemplateFactory}.
     *
     * <p>
     * This method does not throw {@link TemplateSyntaxException} when the
     * template could not be parsed, but the returned stream may throw it. The
     * decomposition of a template allows to find, e.g., all references as the
     * standard template factory creates well-defined standard implementations.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     * @param template
     *            the template to parse. It must not be {@code null}.
     *
     * @return the stream of placeholders
     */
    public static Stream<Template> decomposition(TemplateFormat format, String template) {
        return format.parser(template).with(TemplateFactory.instance()).stream(); // Ensure using the standard classes
    }

    /**
     * Tests if the given input is a literal according to the specified template
     * format.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     * @param input
     *            the input to test. It must not be {@code null}.
     *
     * @return {@code true} if the input is a literal (or composed of literals
     *         only)
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    public static boolean isLiteral(TemplateFormat format, String input) {
        // Optimized variant of decomposition() that does not create intermediate representations
        return format.parser(input).with(LiteralScanner.instance()).stream().allMatch(Boolean.TRUE::equals);
    }

    /**
     * Returns a predicate to tests if an input is a literal according to the
     * specified template format.
     *
     * <p>
     * The returned predicate may throw {@link TemplateSyntaxException} if its
     * input could not be parsed.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     *
     * @return the predicate for testing inputs
     */
    public static Predicate<String> isLiteral(TemplateFormat format) {
        Objects.requireNonNull(format);
        return template -> isLiteral(format, template);
    }

    /**
     * Tests if the given input is valid according to the specified template
     * format.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     * @param input
     *            the input to test. It must not be {@code null}.
     *
     * @return {@code true} if the input can be parsed
     */
    public static boolean isParseable(TemplateFormat format, String input) {
        try {
            return isLiteral(format, input) | true;
        } catch (TemplateSyntaxException e) {
            return false;
        }
    }

    /**
     * Returns a predicate to tests if an input is valid according to the
     * specified template format.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     *
     * @return the predicate for testing inputs
     */
    public static Predicate<String> isParseable(TemplateFormat format) {
        Objects.requireNonNull(format);
        return template -> isParseable(format, template);
    }

    /**
     * Detects if the given template contains any symbols.
     */
    private static final class LiteralScanner implements TemplateCallback<Boolean> {

        /** Sole instance of this class. */
        private static final LiteralScanner INSTANCE = new LiteralScanner();

        /**
         * Creates a new instance.
         */
        private LiteralScanner() {
            // Default constructor
        }

        /**
         * Returns an instance.
         *
         * @return an instance
         */
        public static LiteralScanner instance() {
            return INSTANCE;
        }

        /**
         * @see net.yetamine.template.TemplateCallback#skipped(java.lang.String)
         */
        public Boolean skipped(String value) {
            return Boolean.FALSE;
        }

        /**
         * @see net.yetamine.template.TemplateCallback#literal(java.lang.String)
         */
        public Boolean literal(String value) {
            return Boolean.TRUE;
        }

        /**
         * @see net.yetamine.template.TemplateCallback#constant(java.lang.String,
         *      java.lang.String)
         */
        public Boolean constant(String definition, String value) {
            return Boolean.FALSE;
        }

        /**
         * @see net.yetamine.template.TemplateCallback#reference(java.lang.String,
         *      java.lang.String)
         */
        public Boolean reference(String definition, String reference) {
            return Boolean.FALSE;
        }

        /**
         * @see net.yetamine.template.TemplateCallback#none()
         */
        public Boolean none() {
            return Boolean.TRUE;
        }
    }
}
