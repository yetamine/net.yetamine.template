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
 *
 * @param <R>
 *            the type of the result
 */
public interface Parser<R> {

    /**
     * Parses the next part of the input.
     *
     * <p>
     * This method consumes the next part of the input and updates both
     * {@link #position()} and {@link #done()} accordingly. It may
     *
     * @return the result of the parsing
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    R next();

    /**
     * Indicates whether the parsing has been finished.
     *
     * @return {@code true} iff parsing has been finished
     */
    boolean done();

    /**
     * Returns the position where {@link #next()} shall continue with parsing.
     *
     * @return the position where {@link #next()} shall continue with parsing
     */
    int position();

    /**
     * Returns the input to parse.
     *
     * @return the input to parse
     */
    String input();
}
