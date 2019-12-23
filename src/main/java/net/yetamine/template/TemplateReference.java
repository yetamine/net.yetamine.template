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

/**
 * Represents a template reference.
 */
public final class TemplateReference implements Template, Symbol {

    /** Represented reference. */
    private final String value;
    /** Reference definition. */
    private final String definition;

    /**
     * Creates a new instance.
     *
     * @param def
     *            the definition of the reference. It must not be {@code null}.
     * @param ref
     *            the represented reference to be passed to the resolver
     */
    private TemplateReference(String def, String ref) {
        definition = Objects.requireNonNull(def);
        value = ref;
    }

    /**
     * Creates a new instance.
     *
     * @param def
     *            the definition of the reference. It must not be {@code null}.
     * @param ref
     *            the represented reference to be passed to the resolver
     *
     * @return the new instance
     */
    public static TemplateReference from(String def, String ref) {
        return new TemplateReference(def, ref);
    }

    /**
     * Creates a new instance.
     *
     * @param def
     *            the definition of the reference. It must not be {@code null}.
     * @param ref
     *            the represented reference to be passed to the resolver
     *
     * @return the new instance
     */
    public static Template instance(String def, String ref) {
        return from(def, ref);
    }

    /**
     * @see Template#toString()
     * @see Symbol#toString()
     */
    @Override
    public String toString() {
        return definition;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof TemplateReference) {
            final TemplateReference o = (TemplateReference) obj;
            return definition.equals(o.definition) && Objects.equals(value, o.value);
        }

        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    /**
     * @see net.yetamine.template.Template#apply(java.util.function.Function)
     */
    @Override
    public String apply(Function<? super String, String> resolver) {
        final String result = resolver.apply(value);
        return (result != null) ? result : definition;
    }

    /**
     * @see net.yetamine.template.Symbol#value()
     */
    @Override
    public String value() {
        return value;
    }

    /**
     * @see net.yetamine.template.Symbol#constant()
     */
    @Override
    public boolean constant() {
        return false;
    }

    /**
     * Returns the reference content.
     *
     * @return the reference content
     *
     * @deprecated Use {@link #value()} inherited from {@link Symbol}.
     */
    @Deprecated
    public String reference() {
        return value();
    }
}
