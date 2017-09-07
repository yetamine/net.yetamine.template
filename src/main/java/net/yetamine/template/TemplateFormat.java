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
import java.util.Optional;
import java.util.function.Function;

/**
 * Defines a template format.
 *
 * <p>
 * Implementations of this interface should be immutable.
 */
public interface TemplateFormat {

    /**
     * Formats the given string as a constant.
     *
     * <p>
     * This method effectively performs escaping of the given input, so that
     * parsing it according to this format provides a template constant that
     * resolves to the same value as the original string.
     *
     * <p>
     * Implementation should strive for a canonical form of the result that
     * should be safe with the respect to further extending, e.g., adding a
     * literal should not produce any references.
     *
     * @param string
     *            the input to represent as a constant. It must not be
     *            {@code null}.
     *
     * @return a constant representing the given string
     *
     * @throws UnsupportedOperationException
     *             if the format does not support escaping
     */
    String constant(String string);

    /**
     * Formats the given string as a constant if possible.
     *
     * <p>
     * The default implementation employs {@link #constant(String)} to compute
     * the result and when {@link UnsupportedOperationException} is thrown, it
     * returns an empty {@link Optional} instead. While this approach maintains
     * the backward compatibility, it is not efficient and real implementations
     * should rather stick to a more efficient approach.
     *
     * @param string
     *            the input to represent as a constant. It must not be
     *            {@code null}.
     *
     * @return a constant representing the given string, or an empty
     *         {@link Optional} if the constant could not be created
     */
    default Optional<String> reproduction(String string) {
        try {
            return Optional.of(constant(string));
        } catch (UnsupportedOperationException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns an object that can parse the template according to this format.
     *
     * @param template
     *            the input to parse. It must not be {@code null}.
     *
     * @return a parsing process encapsulation
     */
    TemplateParser parser(String template);

    /**
     * Parses a template.
     *
     * <p>
     * The result of the parsing should be minimal representation of the given
     * template. For instance, when the template consists of a single literal,
     * the result of the parsing should return a single object representing the
     * literal, preferably {@link TemplateLiteral} in this example.
     *
     * @param template
     *            the input to parse. It must not be {@code null}.
     *
     * @return the template representation
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    default Template parse(String template) {
        return TemplateSequence.composition(parser(template));
    }

    /**
     * Resolves a template with the given resolver.
     *
     * <p>
     * This method must behave like {@code parse(template).apply(resolver)},
     * which the default implementation actually invokes, but implementations
     * may provide own solutions that behave more efficiently in their cases.
     *
     * @param template
     *            the template to resolve. It must not be {@code null}.
     * @param resolver
     *            the resolver to apply for unresolved symbols in the template.
     *            It must not be {@code null}.
     *
     * @return the resolved value of the template
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     * @throws TemplateResolvingException
     *             if the resolving fails
     */
    default String resolve(String template, Function<? super String, String> resolver) {
        final TemplateCallback<String> callback = new ResolvingCallback(resolver);
        final Parser<String> parser = parser(template).with(callback);
        final String head = parser.next();

        if (head == null) {
            assert parser.done();
            return "";
        }

        final String next = parser.next();

        if (next == null) {
            assert parser.done();
            return head;
        }

        // Well, there were at least two fragments
        final StringBuilder result = new StringBuilder(template.length()).append(head).append(next);
        for (String current; (current = parser.next()) != null;) {
            result.append(current);
        }

        assert parser.done();
        return result.toString();
    }

    /**
     * Returns a resolution strategy that can resolve any template definition
     * according this template format with the given resolver.
     *
     * @param resolver
     *            the resolver to bind. It must not be {@code null}.
     *
     * @return a resolution strategy
     */
    default TemplateResolving with(Function<? super String, String> resolver) {
        return TemplateResolving.using(this, resolver);
    }
}

/**
 * Support for direct resolving without creating intermediate representation.
 */
final class ResolvingCallback implements TemplateCallback<String> {

    /** Resolver to employ. */
    private final Function<? super String, String> resolver;

    /**
     * Creates a new instance.
     *
     * @param resolving
     *            the resolver to employ. It must not be {@code null}.
     */
    public ResolvingCallback(Function<? super String, String> resolving) {
        resolver = Objects.requireNonNull(resolving);
    }

    /**
     * @see net.yetamine.template.TemplateCallback#skipped(java.lang.String)
     */
    public String skipped(String value) {
        return "";
    }

    /**
     * @see net.yetamine.template.TemplateCallback#literal(java.lang.String)
     */
    public String literal(String value) {
        return value;
    }

    /**
     * @see net.yetamine.template.TemplateCallback#constant(java.lang.String,
     *      java.lang.String)
     */
    public String constant(String definition, String value) {
        return value;
    }

    /**
     * @see net.yetamine.template.TemplateCallback#reference(java.lang.String,
     *      java.lang.String)
     */
    public String reference(String definition, String reference) {
        final String result = resolver.apply(reference);
        return (result != null) ? result : definition;
    }

    /**
     * @see net.yetamine.template.TemplateCallback#none()
     */
    public String none() {
        return null;
    }
}
