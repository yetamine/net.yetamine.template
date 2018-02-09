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
 * Represents a template constant.
 */
public final class TemplateConstant implements Template {

    /** Represented value. */
    private final String value;
    /** Template definition. */
    private final String definition;

    /**
     * Creates a new instance.
     *
     * @param def
     *            the definition of the template. It must not be {@code null}.
     * @param val
     *            the constant value. It must not be {@code null}.
     */
    private TemplateConstant(String def, String val) {
        definition = Objects.requireNonNull(def);
        value = Objects.requireNonNull(val);
    }

    /**
     * Creates a new instance.
     *
     * @param def
     *            the definition of the template. It must not be {@code null}.
     * @param val
     *            the constant value. It must not be {@code null}.
     *
     * @return the new instance
     */
    public static TemplateConstant from(String def, String val) {
        return new TemplateConstant(def, val);
    }

    /**
     * Creates a new instance.
     *
     * @param def
     *            the definition of the template. It must not be {@code null}.
     * @param val
     *            the constant value. It must not be {@code null}.
     *
     * @return the new instance
     */
    public static Template instance(String def, String val) {
        return from(def, val);
    }

    /**
     * @see java.lang.Object#toString()
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

        if (obj instanceof TemplateConstant) {
            final TemplateConstant o = (TemplateConstant) obj;
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
    public String apply(Function<? super String, String> resolver) {
        return value;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public String value() {
        return value;
    }
}
