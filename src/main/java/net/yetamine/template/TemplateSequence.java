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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates an immutable sequence of templates.
 */
public final class TemplateSequence implements Template {

    /** Empty array for faster allocation. */
    private static final Template[] EMPTY = new Template[0];

    /** Fragments of the seqence. */
    private final Template[] fragments;

    /**
     * Creates a new instance.
     *
     * @param sequence
     *            the sequence to represent. It must not be {@code null}, it
     *            must not contain {@code null} elements and the caller must
     *            never modify it.
     */
    private TemplateSequence(Template... sequence) {
        fragments = sequence;
    }

    /**
     * Returns an instance representing the given sequence of templates.
     *
     * @param templates
     *            the sequence to represent. It must not be {@code null} and it
     *            must not contain {@code null} elements.
     *
     * @return an instance representing the given sequence of templates
     */
    public static Template composition(Template... templates) {
        return composed(checked(templates.clone()));
    }

    /**
     * Returns an instance representing the given sequence of templates.
     *
     * @param templates
     *            the sequence to represent. It must not be {@code null} and it
     *            must not contain {@code null} elements.
     *
     * @return an instance representing the given sequence of templates
     */
    public static Template composition(Collection<? extends Template> templates) {
        return composed(checked(templates.toArray(EMPTY)));
    }

    /**
     * Returns an instance representing the composition of parsed templates.
     *
     * @param parser
     *            the source of the templates. It must not be {@code null}.
     *
     * @return an instance representing the composition of parsed templates
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    public static Template composition(TemplateParser parser) {
        final Template head = parser.next();

        if (head == null) {
            assert parser.done();
            return TemplateLiteral.empty();
        }

        final Template next = parser.next();

        if (next == null) {
            assert parser.done();
            return head;
        }

        // Well, there were at least two fragments
        final Collection<Template> fragments = new ArrayList<>();
        fragments.add(head);
        fragments.add(next);
        // Parse the remaining ones if any
        for (Template current; (current = parser.next()) != null;) {
            fragments.add(current);
        }

        assert parser.done();
        return new TemplateSequence(fragments.toArray(EMPTY));
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Stream.of(fragments).map(Object::toString).collect(Collectors.joining());
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof TemplateSequence) && Arrays.equals(fragments, ((TemplateSequence) obj).fragments);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(fragments);
    }

    /**
     * @see net.yetamine.template.Template#apply(java.util.function.Function)
     */
    public String apply(Function<? super String, String> resolver) {
        Objects.requireNonNull(resolver);
        return Stream.of(fragments).map(fragment -> fragment.apply(resolver)).collect(Collectors.joining());
    }

    /**
     * Checks the sequence to represent.
     *
     * @param templates
     *            the sequence to represent. It must not be {@code null} and it
     *            must not contain {@code null} elements.
     *
     * @return the argument
     */
    private static Template[] checked(Template[] templates) {
        for (Template template : templates) {
            Objects.requireNonNull(template);
        }

        return templates;
    }

    /**
     * Returns a combination of the given sequence.
     *
     * @param templates
     *            the sequence to represent. It must not be {@code null}, it
     *            must not contain {@code null} elements and the caller must
     *            never modify it.
     *
     * @return a combination of the given sequence
     */
    private static Template composed(Template[] templates) {
        switch (templates.length) {
            case 0:
                return TemplateLiteral.empty();

            case 1:
                return templates[0];

            default:
                return new TemplateSequence(templates);
        }
    }
}
