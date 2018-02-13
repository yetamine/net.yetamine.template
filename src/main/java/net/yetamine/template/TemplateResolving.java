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
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Encapsulates a resolution strategy which combines a template format and a
 * resolver, so that it can resolve any template definition according to the
 * bound template format and resolver.
 */
public interface TemplateResolving extends UnaryOperator<String> {

    /* Future considerations:
     *
     * Replacing
     *   Function<? super String, String> resolver()
     * with
     *   UnaryOperator<String> resolver()
     * might make the signatures a bit cleaner and perhaps make some use cases
     * easier.
     *
     * On the other hand
     *   TemplateResolving using(TemplateFormat format, Function<? super String, String> resolver)
     * needs rectification as well in such a case. Overloading it with
     * UnaryOperator<String> makes no sense, because the compiler gets
     * confused in such situations, and changing the parameter prevents
     * using more general functions easily. A compromise would turn the
     * Function into UnaryOperator if necessary at some overhead. Would
     * that be acceptable?
     */

    /**
     * Resolves the given template.
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     * @throws TemplateResolvingException
     *             if the resolving fails
     *
     * @see java.util.function.Function#apply(java.lang.Object)
     * @see TemplateFormat#resolve(String, Function)
     */
    default String apply(String template) {
        return format().resolve(template, resolver());
    }

    /**
     * Returns the bound template format.
     *
     * @return the bound template format
     */
    TemplateFormat format();

    /**
     * Returns the bound resolver.
     *
     * @return the bound resolver
     */
    Function<? super String, String> resolver();

    /**
     * Provides a default binding of the given template format and resolver.
     *
     * @param format
     *            the template format. It must not be {@code null}.
     * @param resolver
     *            the resolver. It must not be {@code null}.
     *
     * @return a default binding
     */
    static TemplateResolving using(TemplateFormat format, Function<? super String, String> resolver) {
        return new TemplateResolvingImplementation(format, resolver);
    }
}

/**
 * Default implementation of {@link TemplateResolving}.
 */
final class TemplateResolvingImplementation implements TemplateResolving {

    /** Template format. */
    private final TemplateFormat format;
    /** Template symbol resolver. */
    private final Function<? super String, String> resolver;

    /**
     * Creates a new instance.
     *
     * @param templateFormat
     *            the template format. It must not be {@code null}.
     * @param symbolResolver
     *            the resolver. It must not be {@code null}.
     */
    public TemplateResolvingImplementation(TemplateFormat templateFormat, Function<? super String, String> symbolResolver) {
        resolver = Objects.requireNonNull(symbolResolver);
        format = Objects.requireNonNull(templateFormat);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("TemplateResolving[format=%s, resolver=%s]", format, resolver);
    }

    /**
     * @see net.yetamine.template.TemplateResolving#format()
     */
    public TemplateFormat format() {
        return format;
    }

    /**
     * @see net.yetamine.template.TemplateResolving#resolver()
     */
    public Function<? super String, String> resolver() {
        return resolver;
    }
}
