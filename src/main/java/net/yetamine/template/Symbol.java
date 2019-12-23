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

/**
 * Represents a template symbol, i.e., a part of a template that is not a
 * literal, although it carries a value like a reference for resolving or a
 * constant.
 */
public interface Symbol {

    /**
     * Returns the definition of this symbol as it appears in a template.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    String toString();

    /**
     * Returns the value that the symbol represents and that differs usually
     * from the {@link #toString()} representation.
     *
     * @return the value that the symbol represents
     */
    String value();

    /**
     * Indicates whether the value is constant or needs resolving.
     *
     * @return {@code true} iff the value can be taken as the result without
     *         further interpretation
     */
    boolean constant();
}
