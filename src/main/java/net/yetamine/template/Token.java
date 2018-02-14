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

import java.util.Objects;

/**
 * Represents a token in a parsed text.
 *
 * @param <T>
 *            the type of the value
 */
public final class Token<T> {

    /** Token payload. */
    private final T value;
    /** Starting position. */
    private final int from;
    /** Ending position. */
    private final int to;

    /**
     * Creates a new instance.
     *
     * @param payload
     *            the payload for the token
     * @param positionFrom
     *            the position where the token starts. It must not be negative.
     * @param positionTo
     *            the position where the token ends. It must not be lesser than
     *            the starting position.
     */
    public Token(T payload, int positionFrom, int positionTo) {
        if ((positionFrom < 0) || (positionTo < positionFrom)) {
            final String f = "Given bounds do not satisfy condition 0 <= %d <= %d.";
            throw new IllegalArgumentException(String.format(f, positionFrom, positionTo));
        }

        from = positionFrom;
        to = positionTo;
        value = payload;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Token[position=[%d, %d), value=%s]", from, to, value);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Token<?>) {
            final Token<?> o = (Token<?>) obj;
            return ((from == o.from) && (to == o.to) && Objects.equals(value, o.value));
        }

        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(from, to, value);
    }

    /**
     * Returns the offset where the token starts in the input that the token was
     * produced from.
     *
     * <p>
     * Always {@link #from()} &lt;= {@link #to()}.
     *
     * @return the offset where the token starts
     */
    public int from() {
        return from;
    }

    /**
     * Returns the offset where the token ends in the the input that the token
     * was produced from.
     *
     * <p>
     * Always {@link #from()} &lt;= {@link #to()}.
     *
     * @return the offset where the token ends
     */
    public int to() {
        return to;
    }

    /**
     * Returns the difference {@link #to()} - {@link #from()}, i.e., the number
     * of characters that the token spans over in the input that the token was
     * produced from.
     *
     * @return the length of the token source
     */
    public int length() {
        return to - from;
    }

    /**
     * Returns the value.
     *
     * @return the value, which may be {@code null} for meaningless or sentinel
     *         tokens
     */
    public T value() {
        return value;
    }
}
