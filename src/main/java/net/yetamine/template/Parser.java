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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    /**
     * Returns a stream coupled with this parser.
     *
     * <p>
     * The stream may throw {@link TemplateSyntaxException} if could not parse
     * the template.
     *
     * @return a stream coupled with this parser
     */
    default Stream<R> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ParserIterator<>(this), 0), false);
    }

    /**
     * Passes all results of {@link #next()} until {@link #done()} to the given
     * consumer.
     *
     * <p>
     * This method is a shortcut for {@code stream().forEachOrdered(consumer)}
     * and the default implementation should be more efficient.
     *
     * @param consumer
     *            the consumer to invoke. It must not be {@code null}.
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    default void forEach(Consumer<? super R> consumer) {
        new ParserIterator<>(this).forEachRemaining(consumer);
    }
}

/**
 * Default {@link Iterator} adapter for {@link Parser}.
 *
 * <p>
 * This class is not public intentionally, because {@link Parser} is not meant
 * as an iterator, although it has similar interface. There is a difference in
 * perceiving {@link Parser#done()} and {@link Parser#next()} semantics. On the
 * other hand, it makes sense to turn the parser into a stream of its results.
 * The {@link Iterator} adaptation exists merely for adapting to {@link Stream}
 * as there is no better way in Java 8.
 *
 * @param <R>
 *            the type of the result
 */
final class ParserIterator<R> implements Iterator<R> {

    /** Source parser. */
    private final Parser<? extends R> parser;

    /**
     * Creates a new instance.
     *
     * @param source
     *            the source parser. It must not be {@code null}.
     */
    public ParserIterator(Parser<? extends R> source) {
        parser = Objects.requireNonNull(source);
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return !parser.done();
    }

    /**
     * @see java.util.Iterator#next()
     */
    @Override
    public R next() {
        if (parser.done()) {
            throw new NoSuchElementException();
        }

        return parser.next();
    }
}
