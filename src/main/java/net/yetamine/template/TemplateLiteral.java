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
 * Represents a template literal.
 */
public final class TemplateLiteral implements Template {

    /** Empty literal. */
    private static final TemplateLiteral EMPTY = new TemplateLiteral("");

    /** Represented value. */
    private final String value;

    /**
     * Creates a new instance.
     *
     * @param val
     *            the value to represent. It must not be {@code null}.
     */
    private TemplateLiteral(String val) {
        value = Objects.requireNonNull(val);
    }

    /**
     * Creates a new instance.
     *
     * @param val
     *            the value to represent. It must not be {@code null}.
     *
     * @return the value representation
     */
    public static TemplateLiteral from(String val) {
        return val.isEmpty() ? EMPTY : new TemplateLiteral(val);
    }

    /**
     * Creates a new instance.
     *
     * @param val
     *            the value to represent. It must not be {@code null}.
     *
     * @return the value representation
     */
    public static Template of(String val) {
        return from(val);
    }

    /**
     * Returns an empty instance.
     *
     * @return an empty instance
     */
    public static Template empty() {
        return EMPTY;
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
        return ((obj == this) || ((obj instanceof TemplateLiteral) && value.equals(((TemplateLiteral) obj).value)));
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return value.hashCode();
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
