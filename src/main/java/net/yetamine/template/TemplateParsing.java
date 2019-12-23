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

/**
 * Adapts a {@link TemplateParser} to a generic parser with the given callback.
 *
 * @param <R>
 *            the type of the callback's results
 */
public final class TemplateParsing<R> implements Parser<R> {

    /** Parent parser. */
    private final TemplateParser parser;
    /** Bound parsing callback. */
    private final TemplateCallback<? extends R> callback;

    /**
     * Creates a new instance.
     *
     * @param parent
     *            the parent parser. It must not be {@code null}.
     * @param visitor
     *            the parsing callback. It must not be {@code null}.
     */
    public TemplateParsing(TemplateParser parent, TemplateCallback<? extends R> visitor) {
        callback = Objects.requireNonNull(visitor);
        parser = Objects.requireNonNull(parent);
    }

    /**
     * @see net.yetamine.template.Parser#next()
     */
    @Override
    public R next() {
        return parser.next(callback);
    }

    /**
     * @see net.yetamine.template.Parser#done()
     */
    @Override
    public boolean done() {
        return parser.done();
    }

    /**
     * @see net.yetamine.template.Parser#position()
     */
    @Override
    public int position() {
        return parser.position();
    }

    /**
     * @see net.yetamine.template.Parser#input()
     */
    @Override
    public String input() {
        return parser.input();
    }
}
