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
import java.util.function.Predicate;

/**
 * Tools for fast template testing.
 */
public final class TemplateTesting {

    /**
     * Prevents creating instances of this class.
     */
    private TemplateTesting() {
        throw new AssertionError();
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
    public static boolean isValid(TemplateFormat format, String input) {
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
    public static Predicate<String> isValid(TemplateFormat format) {
        Objects.requireNonNull(format);
        return template -> isValid(format, template);
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
