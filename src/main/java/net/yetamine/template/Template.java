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

import java.util.function.Function;

/**
 * Represents a template.
 *
 * <p>
 * This interface provides a possibility to define various custom template
 * types. However, any construction of template instances should prefer an
 * existing implementation, like {@link TemplateLiteral}, which could help
 * decomposing a template representation in some cases.
 *
 * <p>
 * Implementations of this interface should be immutable.
 */
public interface Template {

    /**
     * Applies the given resolver to return the resolved value of this template.
     *
     * <p>
     * When resolving a template, the resolver gets each reference occurring in
     * the template and it should resolve the references to definite values, or
     * return {@code null} when a reference definition should be retained in the
     * resolved result. However, a resolver may throw an exception, which should
     * be relayed to the caller, and {@link TemplateResolvingException} might be
     * preferred to support the documented behavior. References should not be
     * {@code null}.
     *
     * @param resolver
     *            the function to resolve unresolved references in the template.
     *            It must not be {@code null}.
     *
     * @return the resolved value of this template
     *
     * @throws TemplateResolvingException
     *             if the resolving fails
     */
    String apply(Function<? super String, String> resolver);

    /**
     * Returns the definition of this template.
     *
     * <p>
     * Proper overriding this method allows to reconstruct the string form of
     * the represented template.
     *
     * @see java.lang.Object#toString()
     */
    String toString();
}
