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

/**
 * Encapsulates a parsing strategy and the state during parsing a template.
 *
 * <p>
 * The parsing travels through the input, or its part limited by given bounds,
 * and {@code 0 <= position() <= input().length()} must hold true always. The
 * result of {@link #done()} usually reflects the position and the limit, but
 * some parsing algorithms may have input bounded yet with some terminating
 * sequence and therefore may finish before reading the whole input.
 */
public interface TemplateParser extends Parser<Template> {

    /**
     * Defines a callback for the parser that shall react on parsing events.
     *
     * @param <R>
     *            the type of the callback result
     */
    interface Callback<R> {

        /**
         * Handles an occurrence of an insignificant input part.
         *
         * @param value
         *            the part of the input that has been skipped. It must not
         *            be {@code null}.
         *
         * @return the result to pass
         */
        R skipped(String value);

        /**
         * Handles an occurrence of a literal.
         *
         * @param value
         *            the literal value. It must not be {@code null}.
         *
         * @return the result to pass
         */
        R literal(String value);

        /**
         * Handles an occurrence of a constant.
         *
         * @param definition
         *            the definition of the constant. It must not be
         *            {@code null}.
         * @param value
         *            the value of the constant. It must not be {@code null}.
         *
         * @return the result to pass
         */
        R constant(String definition, String value);

        /**
         * Handles an occurrence of a reference.
         *
         * @param definition
         *            the definition of the constant. It must not be
         *            {@code null}.
         * @param reference
         *            the reference. It must not be {@code null}.
         *
         * @return the result to pass
         */
        R reference(String definition, String reference);

        /**
         * Indicates the parsing has been finished.
         *
         * @return the result to pass
         */
        R done();
    }

    /**
     * Parses the next part of the input.
     *
     * <p>
     * This method tries to recognize the next fragment of the template and
     * invokes the given callback. If the callback returns, {@link #position()}
     * and {@link #done()} are updated according to the fragment and the result
     * is returned.
     *
     * @param <R>
     *            the type of the result
     * @param callback
     *            the callback to invoke. It must not be {@code null}.
     *
     * @return this instance
     */
    <R> R next(Callback<? extends R> callback);

    /**
     * Parses the next part of the input with the default callback that produces
     * appropriate {@link Template} instances using the default implementations,
     * except for <i>done</i> event, which returns {@code null}.
     *
     * @return the next template fragment, or {@code null} on {@link #done()}
     */
    default Template next() {
        return next(TemplateFactoryCallback.instance());
    }

    /**
     * Returns an adapter that provides a parser interface bound to the given
     * callback.
     *
     * @param <R>
     *            the type of the callback results
     * @param callback
     *            the callback. It must not be {@code null}.
     *
     * @return the adapter
     */
    default <R> Parser<R> with(Callback<? extends R> callback) {
        return new TemplateParsing<>(this, callback);
    }
}
