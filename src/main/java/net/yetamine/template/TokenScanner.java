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
 * Scanner for finding a token in an input string.
 *
 * <p>
 * Implementations of this interface should be immutable, unless a particular
 * use explicitly allows a mutable or thread-unsafe implementation. The state
 * when scanning for the next token should rather be completely confined in the
 * {@link #find(String, int)} method otherwise.
 *
 * @param <T>
 *            the type of the token value
 */
@FunctionalInterface
public interface TokenScanner<T> {

    /**
     * Finds the next token.
     *
     * @param input
     *            the input to scan. It must not be {@code null}.
     * @param offset
     *            the offset where to scan from. It must not be negative and it
     *            must not be greater than the length of the input.
     *
     * @return the token, or {@code null} if none found in the given input part
     */
    Token<T> find(String input, int offset);
}
