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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Provides a flexible recursion-capable resolver.
 *
 * <p>
 * This resolver employs several custom handlers to adapt to various use cases.
 * The most important is the handler for finding resolvable values of requested
 * placeholders. A placeholder resolution returns a template definition that may
 * require recursive resolution. Circular dependencies between such placeholders
 * are detected and processed with another handler.
 *
 * <p>
 * Resolving a placeholder in some situations may depend on the context, e.g.,
 * available templates can be referred with structured names and placeholders
 * may then contain references that are relative to the template name where the
 * particular placeholder occurs. In such cases, the resolver needs to retrieve
 * an absolute reference and employs an external linking strategy for that. The
 * strategy receives the placeholder and the context linked to its occurrence,
 * so that the strategy can determine the absolute reference pointing to the
 * target template. If no absolute reference could be constructed (e.g., the
 * placeholder contains an invalid reference), the strategy may indicate the
 * failure with an exception or by returning {@code null}, which requests
 * keeping the placeholder unresolved.
 *
 * <p>
 * All custom handlers mentioned above must provide stable results.
 *
 * <p>
 * The builder for defining a new resolver is not thread-safe, but the resolver
 * itself is thread-safe under the assumption that the used custom handlers are
 * thread-safe as well.
 *
 * <p>
 * Resolved references may be cached to speed up future resolutions.
 *
 * @param <T>
 *            the type of the references
 */
public final class TemplateRecursion<T> implements UnaryOperator<String> {

    /**
     * Computes a reference for a placeholder linked to a context.
     *
     * @param <T>
     *            the type of the reference
     */
    @FunctionalInterface
    public interface Linking<T> extends Function<String, T> {

        /**
         * Computes the reference for a placeholder with no context provided.
         *
         * <p>
         * The default implementation calls {@code apply(placeholder, null)}.
         *
         * @param placeholder
         *            the placeholder. It must not be {@code null}.
         *
         * @return the reference for a placeholder, or {@code null} if no valid
         *         reference could be linked with the available information and
         *         the placeholder should be kept unresolved
         *
         * @throws TemplateResolvingException
         *             if the linking should fail rather than keeping an
         *             unresolvable placeholder in the result
         *
         * @see java.util.function.Function#apply(java.lang.Object)
         */
        default T apply(String placeholder) {
            return apply(placeholder, null);
        }

        /**
         * Computes the reference for a placeholder.
         *
         * @param placeholder
         *            the placeholder. It must not be {@code null}.
         * @param context
         *            the reference for the context which the placeholder shall
         *            be linked in. It may be {@code null} for no context.
         *
         * @return the reference for a placeholder, or {@code null} if no valid
         *         reference could be linked with the available information and
         *         the placeholder should be kept unresolved
         *
         * @throws TemplateResolvingException
         *             if the linking should fail rather than keeping an
         *             unresolvable placeholder in the result
         */
        T apply(String placeholder, T context);
    }

    /**
     * Represents a template bound to a context.
     *
     * @param <T>
     *            the type of the context
     */
    public static final class Binding<T> implements Template {

        /** Defining template. */
        private final Template template;
        /** Context for the template */
        private final T context;

        /**
         * Creates a new instance.
         *
         * @param definition
         *            the defining template. It must not be {@code null}.
         * @param scope
         *            the context bound to the template. It should not be
         *            {@code null}, but implementations may tolerate that.
         */
        private Binding(Template definition, T scope) {
            template = Objects.requireNonNull(definition);
            context = scope;
        }

        /**
         * Creates a new instance.
         *
         * @param <T>
         *            the type of the context
         * @param definition
         *            the defining template. It must not be {@code null}.
         * @param scope
         *            the context bound to the template. It should not be
         *            {@code null}, but implementations may tolerate that.
         *
         * @return the new instance
         */
        public static <T> Binding<T> of(Template definition, T scope) {
            return new Binding<>(definition, scope);
        }

        /**
         * Creates a new instance for a literal.
         *
         * @param <T>
         *            the type of the context
         * @param value
         *            the value to represent. It must not be {@code null}.
         * @param scope
         *            the context bound to the template. It should not be
         *            {@code null}, but implementations may tolerate that.
         *
         * @return the new instance
         */
        public static <T> Binding<T> of(String value, T scope) {
            return of(TemplateLiteral.of(value), scope);
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return template.toString();
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof Binding<?>) {
                final Binding<?> o = (Binding<?>) obj;
                return template.equals(o.template) && Objects.equals(context, o.context);
            }

            return false;
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return Objects.hash(template, context);
        }

        /**
         * @see net.yetamine.template.Template#apply(java.util.function.Function)
         */
        public String apply(Function<? super String, String> resolver) {
            return template.apply(resolver);
        }

        /**
         * Returns the context for resolving the template.
         *
         * @return the context for resolving the template
         */
        public T context() {
            return context;
        }
    }

    /**
     * Handler for parsing errors.
     *
     * @param <T>
     *            the type of the context
     */
    @FunctionalInterface
    public interface ParsingFailureHandler<T> {

        /**
         * Handles the reported parsing error.
         *
         * <p>
         * The handler should return either a replacement for the failed
         * reference, or throw an exception that shall be relayed to the caller.
         * Note that the handler shall be invoked during the resolution process,
         * so the exception should be a {@link TemplateResolvingException} as an
         * unrelated exception might have surprising results to the caller.
         *
         * <p>
         * The handler may return {@code null} which means the default handling.
         * In the case of {@link TemplateRecursion}, the default handling keeps
         * the definition.
         *
         * @param reference
         *            the reference to the template that could not be parsed. It
         *            must not be {@code null}.
         * @param definition
         *            the definition of the referred template. It must not be
         *            {@code null}.
         * @param e
         *            the exception describing the problem. It may be
         *            {@code null} if no actual exception is available.
         *
         * @return the replacement for the failed definition
         */
        String handle(T reference, String definition, TemplateSyntaxException e);

        /**
         * Returns a handler returning {@code null}, which requests the default
         * handling implemented by the caller.
         *
         * @param <T>
         *            the type of the context
         *
         * @return a handler returning {@code null}
         */
        static <T> ParsingFailureHandler<T> defaultHandling() {
            return (reference, definition, e) -> null;
        }
    }

    /**
     * Handler for resolving templates that lead to a circular dependency.
     *
     * @param <T>
     *            the type of the context
     */
    @FunctionalInterface
    public interface RecursionFailureHandler<T> {

        /**
         * Returns a replacement for a troubling template.
         *
         * @param reference
         *            the reference to the template that could not be resolved
         *            recursively. It must not be {@code null}.
         * @param definition
         *            the template that the reference refers to. It must not be
         *            {@code null}.
         * @param resolved
         *            the resolver for successfully resolved dependencies of the
         *            template. It must not be {@code null}.
         *
         * @return the replacement, or {@code null} if the reference should be
         *         resolved to the definition of the template (so the template
         *         remains unresolved and its definition would be used as a
         *         literal)
         */
        String handle(T reference, Binding<T> definition, Function<T, String> resolved);

        /**
         * Returns a handler returning {@code null}, which requests the default
         * handling implemented by the caller.
         *
         * @param <T>
         *            the type of the context
         *
         * @return a handler returning {@code null}
         */
        static <T> RecursionFailureHandler<T> defaultHandling() {
            return (reference, definition, resolved) -> null;
        }
    }

    /** Reference linking strategy. */
    final Linking<T> linking;
    /** Template lookup strategy. */
    final Function<? super T, Binding<T>> lookup;
    /** Strategy for handling circular dependencies. */
    final RecursionFailureHandler<T> recursionFailureHandler;

    /** Cache for resolved references. */
    private final Map<T, Nullable<String>> cache;

    /**
     * Creates a new instance.
     *
     * @param builder
     *            the builder to use. It must not be {@code null}.
     */
    TemplateRecursion(Builder<T> builder) {
        lookup = Objects.requireNonNull(builder.lookup);
        linking = Objects.requireNonNull(builder.linking);
        recursionFailureHandler = Objects.requireNonNull(builder.recursionFailureHandler);
        cache = builder.caching ? new ConcurrentHashMap<>() : null;
    }

    /**
     * Creates a new builder.
     *
     * @param <T>
     *            the type of the references
     * @param linking
     *            the placeholder linking strategy. It must not be {@code null}.
     * @param lookup
     *            the template lookup strategy. It must not be {@code null}.
     *
     * @return the builder
     */
    public static <T> Builder<T> builder(Linking<T> linking, Function<? super T, Binding<T>> lookup) {
        return new Builder<>(linking, lookup);
    }

    /**
     * Creates a new source definition.
     *
     * @param <T>
     *            the type of the references
     * @param linking
     *            the placeholder linking strategy. It must not be {@code null}.
     *
     * @return the source definition
     */
    public static <T> Source<T> source(Linking<T> linking) {
        return new Source<>(linking);
    }

    /**
     * Creates a new source definition with the default linking strategy.
     *
     * <p>
     * The default linking strategy just returns the given placeholder, so it
     * works well when placeholders are equal to absolute references and when
     * they may have a free form.
     *
     * @return the source definition
     */
    public static Source<String> source() {
        return new Source<>((placeholder, context) -> placeholder);
    }

    /**
     * Creates a new builder with the given source.
     *
     * @param <T>
     *            the type of the references
     * @param source
     *            the source to use. It must not be {@code null}.
     *
     * @return the builder
     */
    public static <T> Builder<T> with(Source<T> source) {
        return new Builder<>(source.linking(), source.lookup());
    }

    /**
     * Creates a new builder with the given template source and fallback.
     *
     * @param templates
     *            the function to supply template definitions. It must not be
     *            {@code null}.
     * @param fallback
     *            the function to supply fallback values for missing templates.
     *            It must not be {@code null}.
     *
     * @return the builder
     */
    public static Builder<String> with(Function<? super String, String> templates, Function<? super String, String> fallback) {
        return with(source().templates(templates).onDeliveryMissed(fallback));
    }

    /**
     * Creates a new builder with the given template source.
     *
     * @param templates
     *            the function to supply template definitions. It must not be
     *            {@code null}.
     *
     * @return the builder
     */
    public static Builder<String> with(Function<? super String, String> templates) {
        return with(source().templates(templates));
    }

    /**
     * Resolves the given placeholder using the implicit context.
     *
     * @see java.util.function.Function#apply(java.lang.Object)
     */
    public String apply(String placeholder) {
        return resolve(linking.apply(placeholder));
    }

    /**
     * Resolves the given reference.
     *
     * @param reference
     *            the reference to resolve
     *
     * @return the resolution, or {@code null} if the placeholder for the
     *         reference should be retained, which occurs always when the
     *         reference is {@code null}
     */
    public String resolve(T reference) {
        if (reference == null) {
            return null;
        }

        final Nullable<String> cached = cached(reference);
        if (cached != null) {
            return cached.value();
        }

        final Binding<T> resolvable = lookup.apply(reference);
        if (resolvable == null) {
            return null;
        }

        // Use a local object as it allows changing the cache implementation
        // if necessary or applying different thread safety policy (e.g.,
        // having the updates atomic indeed). Now, we postpone the update
        // until the resolution succeeds (so we are safe from exceptions).

        final Map<T, Nullable<String>> resolved = new Resolution().add(reference, resolvable).resolve();

        if (cache != null) {
            cache.putAll(resolved);
        }

        return Nullable.toValue(resolved.get(reference));
    }

    /**
     * Returns a cached resolution.
     *
     * @param reference
     *            the reference to check. It must not be {@code null}.
     *
     * @return the result (which may represent an unresolvable reference), or
     *         {@code null} if the referred result was not found in the cache
     */
    Nullable<String> cached(T reference) {
        return (cache != null) ? cache.get(reference) : null;
    }

    /**
     * Builder for composing the lookup strategy from simpler components.
     *
     * <p>
     * The resulting lookup strategy composes several components into a seamless
     * whole:
     *
     * <ol>
     * <li>The {@link #constants(Function)} function can provide a constant
     * value for a reference. A constant shall not be parsed as a template,
     * therefore it can be used for overriding the remaining steps and/or for
     * optimizing the processing as the constants are not parsed as templates
     * later.</li>
     *
     * <li>The {@link #templates(Function)} function can provide a template
     * definition that shall be parsed with the given {@link #format()}. The
     * parsing failure handler applies if a supplied template could not be
     * parsed.</li>
     *
     * <li>If the previous steps fails to deliver any usable outcome, then the
     * {@link #onDeliveryMissed(Function)} handler gets the chance to supply a
     * fallback. If supplies no value, the resulting lookup strategy gives up,
     * indicating that the placeholder should be kept unresolved.</li>
     * </ol>
     *
     * Any of the components may throw a {@link TemplateResolvingException} to
     * break the resolution process immediately. The exception must be relayed
     * to the original resolver invoker.
     *
     * @param <T>
     *            the type of references
     */
    public static final class Source<T> {

        /** Common handler indicating a missed delivery. */
        private static final Function<Object, String> MISSED = reference -> null;

        /** Linking strategy. */
        private final Linking<T> linking;

        /** Strategy for falling back from failed outcome delivery. */
        private Function<? super T, String> deliveryMissedHandler = MISSED;
        /** Strategy for dereferencing a reference to a constant value. */
        private Function<? super T, String> constantHandler = MISSED;
        /** Strategy for dereferencing a reference to a template. */
        private Function<? super T, String> templateHandler = MISSED;
        /** Format of the templates to use for parsing them. */
        private TemplateFormat format = Interpolation.standard();
        /** Strategy for handling parsing failures in nested templates. */
        private ParsingFailureHandler<? super T> parsingFailureHandler = ParsingFailureHandler.defaultHandling();

        /**
         * Creates a new instance.
         *
         * @param linkStrategy
         *            the linking strategy to base the source on. It must not be
         *            {@code null}.
         */
        Source(Linking<T> linkStrategy) {
            linking = Objects.requireNonNull(linkStrategy);
        }

        /**
         * Sets the template format.
         *
         * <p>
         * The template format applies for parsing {@link #templates(Function)}.
         * If not overridden, {@link Interpolation#standard()} shall be used.
         *
         * @param value
         *            the value to set. It must not be {@code null}.
         *
         * @return this instance
         */
        public Source<T> format(TemplateFormat value) {
            format = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Returns the template format.
         *
         * @return the template format
         */
        public TemplateFormat format() {
            return format;
        }

        /**
         * Sets the function to dereference a reference to a constant value.
         *
         * <p>
         * The default handler returns {@code null}, which results to consult
         * {@link #templates(Function)} for all references.
         *
         * @param handler
         *            the handler to set. It must not be {@code null}.
         *
         * @return this instance
         */
        public Source<T> constants(Function<? super T, String> handler) {
            constantHandler = Objects.requireNonNull(handler);
            return this;
        }

        /**
         * Sets the function to dereference a reference to a template.
         *
         * <p>
         * The default handler returns {@code null}, which should should be
         * overridden usually, unless all references shall be constants, so
         * recursive resolution becomes effectively disabled.
         *
         * @param handler
         *            the handler to set. It must not be {@code null}.
         *
         * @return this instance
         */
        public Source<T> templates(Function<? super T, String> handler) {
            templateHandler = Objects.requireNonNull(handler);
            return this;
        }

        /**
         * Sets a handler for fallback resolution when other components fail to
         * deliver a usable outcome.
         *
         * <p>
         * The default handler returns {@code null} that triggers the built-in
         * fallback strategy which should keep the definition of the template,
         * that could not be parsed, as a literal.
         *
         * @param handler
         *            the handler to set. It must not be {@code null}.
         *
         * @return this instance
         */
        public Source<T> onDeliveryMissed(Function<? super T, String> handler) {
            deliveryMissedHandler = Objects.requireNonNull(handler);
            return this;
        }

        /**
         * Sets a handler for dealing with failures occuring when parsing nested
         * templates.
         *
         * <p>
         * The default handler returns {@code null} that triggers the built-in
         * fallback strategy which should keep the definition of the template,
         * that could not be parsed, as a literal.
         *
         * @param handler
         *            the handler to set. It must not be {@code null}.
         *
         * @return this instance
         */
        public Source<T> onParsingFailure(ParsingFailureHandler<? super T> handler) {
            parsingFailureHandler = Objects.requireNonNull(handler);
            return this;
        }

        /**
         * Returns the lookup strategy composed from the given components.
         *
         * @return the lookup strategy
         */
        public Function<T, Binding<T>> lookup() {
            // Bind the following ugly lambda to these local variables. We could make an object as well to freeze that.
            final TemplateFormat templateFormat = format;
            final Function<? super T, String> constants = constantHandler;
            final Function<? super T, String> templates = templateHandler;
            final ParsingFailureHandler<? super T> parsingFailure = parsingFailureHandler;
            final Function<? super T, String> deliveryMissed = deliveryMissedHandler;

            return reference -> {
                if (reference == null) { // Just a safety guard, but the algorithm never passes null to the lookup
                    return null;
                }

                final String constant = constants.apply(reference);
                if (constant != null) {
                    return Binding.of(constant, reference);
                }

                final String template = templates.apply(reference);
                if (template != null) {
                    try { // Parse the template if possible
                        return Binding.of(templateFormat.parse(template), reference);
                    } catch (TemplateSyntaxException e) {
                        final String fallback = parsingFailure.handle(reference, template, e);
                        if (fallback != null) { // Prefer parsing fallback to the general one
                            return Binding.of(fallback, reference);
                        }
                    }
                }

                final String fallback = deliveryMissed.apply(reference);
                return (fallback != null) ? Binding.of(fallback, reference) : null;
            };
        }

        /**
         * Returns the linking strategy.
         *
         * @return the linking strategy
         */
        public Linking<T> linking() {
            return linking;
        }
    }

    /**
     * Builder for making {@link TemplateRecursion}.
     *
     * @param <T>
     *            the type of the references
     */
    public static final class Builder<T> {

        /** Reference linking strategy. */
        final Linking<T> linking;
        /** Lookup strategy if given explicitly. */
        final Function<? super T, Binding<T>> lookup;

        /** Handler for dealing with circular dependencies. */
        RecursionFailureHandler<T> recursionFailureHandler = RecursionFailureHandler.defaultHandling();
        /** Caching enabled. */
        boolean caching;

        /**
         * Creates a new instance.
         *
         * @param lookupStrategy
         *            the lookup strategy to use. It must not be {@code null}.
         */
        Builder(Linking<T> linkingStrategy, Function<? super T, Binding<T>> lookupStrategy) {
            linking = Objects.requireNonNull(linkingStrategy);
            lookup = Objects.requireNonNull(lookupStrategy);
        }

        /**
         * Sets a handler for dealing with circular placeholder dependencies.
         *
         * <p>
         * If a placeholder resolution finds out that a placeholder suffers from
         * a circular dependency or depends on a cycle, the handler receives the
         * placeholder, the definition of the template to resolve and a resolver
         * for non-circular references that the template refers to. The handler
         * may return {@code null} to keep the whole template unresolved, or it
         * may throw an exception; {@link TemplateResolvingException} should be
         * preferred in such cases, because the exception shall be relayed to
         * the caller and this exception is expected.
         *
         * <p>
         * The default handler uses the given resolver to compose a partially
         * resolved result.
         *
         * @param handler
         *            the handler to set. It must not be {@code null}.
         *
         * @return this instance
         */
        public Builder<T> onRecursionFailure(RecursionFailureHandler<T> handler) {
            recursionFailureHandler = Objects.requireNonNull(handler);
            return this;
        }

        /**
         * Sets whether the resolver should cache resolved placeholders.
         *
         * <p>
         * The resolver does not cache resolved placeholders by default, but
         * this method provides means for enabling this feature. The caching
         * support is thread-safe, but increases memory consumption and inital
         * resolution time.
         *
         * @param value
         *            {@code true} if the resolver should cache resolved
         *            placeholders
         *
         * @return this instance
         */
        public Builder<T> caching(boolean value) {
            caching = value;
            return this;
        }

        /**
         * Indicates whether the resolver should cache resolved placeholders.
         *
         * @return {@code true} if the resolver should cache resolved
         *         placeholders
         */
        public boolean caching() {
            return caching;
        }

        /**
         * Returns a resolver from this builder.
         *
         * @return the resolver
         */
        public TemplateRecursion<T> build() {
            return new TemplateRecursion<>(this);
        }
    }

    /**
     * Represents a nullable value as a regular object that can be used where
     * distinguishing {@code null} can't represent "no object", e.g., a value
     * stored in a {@link ConcurrentHashMap}.
     *
     * @param <T>
     *            the type of the value
     */
    private static final class Nullable<T> {

        /** Shared constant for a {@code null}. */
        private static final Nullable<Object> NULL = new Nullable<>(null);

        /** Stored value. */
        private final T value;

        /**
         * Creates a new instance.
         *
         * @param content
         *            the content to represent
         */
        private Nullable(T content) {
            value = content;
        }

        /**
         * Returns an instance representing the value.
         *
         * @param <T>
         *            the type of the value
         * @param value
         *            the value to store
         *
         * @return an instance representing the value
         */
        @SuppressWarnings("unchecked")
        public static <T> Nullable<T> of(T value) {
            return (value != null) ? new Nullable<>(value) : (Nullable<T>) NULL;
        }

        /**
         * Returns the represented value, or {@code null} if no instance given.
         *
         * @param <T>
         *            the type of the value
         * @param nullable
         *            the instance to query
         *
         * @return the value (possibly {@code null}), or {@code null} if no
         *         instance given
         */
        public static <T> T toValue(Nullable<? extends T> nullable) {
            return (nullable != null) ? nullable.value() : null;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return (value != null) ? String.format("Nullable[value=%s]", value) : "Nullable[null]";
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            return ((obj instanceof Nullable<?>) && Objects.equals(value, ((Nullable<?>) obj).value));
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        /**
         * Returns the represented value.
         *
         * @return the represented value, which may be {@code null}
         */
        public T value() {
            return value;
        }
    }

    /**
     * Represents a missing template.
     *
     * <p>
     * This implementation violates somewhat {@link Template#toString()} as a
     * missing template has no definition to return, but when used just for a
     * missing binding, that should not leak out, such a violation seems
     * acceptable.
     */
    private static final class MissingTemplate implements Template {

        /** Sole instance of this class. */
        private static final Template INSTANCE = new MissingTemplate();

        /**
         * Prevents creating instances of this class.
         */
        private MissingTemplate() {
            // Default constructor
        }

        /**
         * Creates a binding representing a missing template.
         *
         * @param <T>
         *            the type of the context
         * @param scope
         *            the context bound to the template. It should not be
         *            {@code null}, but implementations may tolerate that.
         *
         * @return the new instance
         */
        public static <T> Binding<T> bind(T scope) {
            return Binding.of(INSTANCE, scope);
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return ""; // Should not be used anyway!
        }

        /**
         * @see net.yetamine.template.Template#apply(java.util.function.Function)
         */
        public String apply(Function<? super String, String> resolver) {
            return null;
        }
    }

    /**
     * Implements the resolution algorithm and holds the locally needed data.
     *
     * <p>
     * The algorithm uses a directed graph with templates being the vertices of
     * the graph. An <i>incoming</i> edge leads from vertex <i>A</i> to <i>B</i>
     * when <i>B</i> contains a placeholder that refers to <i>A</i>, according
     * to the given lookup function. An <i>outgoing</i> edge leads then in the
     * other direction.
     *
     * <p>
     * Before resolving anything, the graph must be constructed. Placholders and
     * their definitions are added in the graph. Actually, a single placeholder
     * comes from outside, but the construction algorithm does not care if more
     * placeholders are specified. The definition of each placeholder <i>B</i>
     * must be decomposed and each reference <i>A</i> found in that definition
     * must be dereferences into a definition for placeholder <i>A</i>. Edges
     * between <i>A</i> and <i>B</i> are added as described above and recursion
     * takes care of the rest, so that the transitive closure is computed for
     * all explicitly added placeholders and their definitions. After all the
     * vertices and edges for the resolution are added, the resolution can be
     * performed.
     *
     * <p>
     * The algorithm uses topological sort of the graph as the main means for
     * resolving the templates represented by the vertices. It cuts the vertices
     * with no incoming edges (i.e., having no unresolved dependencies) and puts
     * their resolutions in the final result map, which is used for resolving an
     * unresolved template in subsequent steps.
     *
     * <p>
     * Topological sort can't handle cycles. It can process and remove all trees
     * from the graph and it can chop off the trees that are rooted in a cycle.
     * Hence after it stops, the graph must be either empty (it was a forest),
     * or all remaining vertices depend on a cycle (i.e., when following even
     * any of the incoming edges, the path leads to a cycle).
     *
     * <p>
     * When topological sort fails to resolve all vertices, the algorithm finds
     * all vertices lying on a cycle and resolves them with the given handler
     * indepedently, which removes all cycles from the graph. Then it primary
     * topological sort algorithm resumes and resolves the remaining vertices.
     * This strategy delivers more intuitive results with the simple rule that
     * only a placeholder lying in a cycle is resolved as a failure, while any
     * placeholder just depending on a cycle resolves as much as possible. The
     * implementation of this strategy does not require so complex cache, too,
     * which simplifies ensuring thread safety.
     *
     * <p>
     * The implementation several maps to capture the graph and the progress:
     *
     * <ul>
     * <li>The <i>incoming</i> edge map: it captures all incoming edges for a
     * vertex, so that removing an entry from this map effectively removes the
     * appropriate vertex from the graph for topological sort. By modifying it,
     * the algorithm records the progress and holds the traversal state.</li>
     *
     * <li>The <i>outgoing</i> edge map: it captures all outgoing edges for a
     * vertex and it mirrors the <i>incoming</i> edge map at the beginning. It
     * is used for updating <i>incoming</i> edges for depending vertices.</li>
     *
     * <li>The <i>resolved</i> map holds the stable resolutions, which at the
     * end is the result of the algorithm. It is important during the progress
     * as well, because when resolving a vertex, this map supplies resolutions
     * for its references. When following the edges of the graph, it never fails
     * to supply a valid resolution.</li>
     *
     * <li>Finally, there are two maps that translate references and vertices
     * (i.e., the templates for the references) in both directions, so that a
     * vertex can be found for each reference and vice versa.</li>
     * </ul>
     */
    private final class Resolution {

        /** References for {@link #vertices}. */
        private Map<Binding<T>, T> references = new HashMap<>();
        /** Vertices of the dependency graph by their references. */
        private Map<T, Binding<T>> vertices = new HashMap<>();
        /** Set of incoming edges in the dependency graph. */
        private Map<Binding<T>, Set<Binding<T>>> incoming = new HashMap<>();
        /** Set of outgoing edges in the dependency graph. */
        private Map<Binding<T>, Set<Binding<T>>> outgoing;
        /** Resolution results for {@link #vertices}. */
        private Map<T, Nullable<String>> resolved;

        /**
         * Creates a new instance.
         */
        public Resolution() {
            // Default constructor
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format("Resolution[incoming=%s, outgoing=%s, resolved=%s]", incoming, outgoing, resolved);
        }

        /**
         * Adds a vertex for later resolution.
         *
         * @param reference
         *            the reference to resolve. It must not be {@code null}.
         * @param definition
         *            the definition of the reference. It must not be
         *            {@code null}.
         *
         * @return this instance
         *
         * @throws IllegalStateException
         *             if {@link #resolve()} has been called
         */
        public Resolution add(T reference, Binding<T> definition) {
            if (vertices == null) {
                throw new IllegalStateException();
            }

            putAll(reference, definition);
            return this;
        }

        /**
         * Resolves all vertices added by {@link #add(Object, Binding)} and
         * resolved recursively.
         *
         * @return the map with the resolutions
         */
        public Map<T, Nullable<String>> resolve() {
            if (resolved != null) {
                return resolved;
            }

            assert (vertices.size() == references.size());
            assert (vertices.size() == incoming.size());

            // Find all outgoing edges
            outgoing = new HashMap<>();
            incoming.forEach((vertex, dependsOn) -> {
                dependsOn.forEach(dependency -> outgoing.computeIfAbsent(dependency, k -> new HashSet<>()).add(vertex));
            });

            // Resolve the complete graph
            resolved = new HashMap<>();

            if (!resolveTrees()) {
                final Set<Binding<T>> cycles = discoverCycles();
                resolveRecursionFailures(cycles);
                incoming.keySet().removeAll(cycles);
                final boolean done = resolveTrees();
                assert done; // All vertices should be handled then!
            }

            assert (vertices.size() == resolved.size());

            references = null;
            vertices = null;
            incoming = null;
            outgoing = null;
            return resolved;
        }

        /**
         * Returns the resolved value for a reference.
         *
         * @param reference
         *            the reference
         *
         * @return the resolved value, {@code null} when could not be resolved
         *         or not resolved yet
         */
        private String resolved(T reference) {
            return (reference != null) ? Nullable.toValue(resolved.get(reference)) : null;
        }

        /**
         * Marks the given vertex as finished.
         *
         * <p>
         * Cuts the given vertex from the <i>incoming</i> edges for all vertices
         * that are linked through the vertex's <i>outgoing</i> edges (i.e., for
         * all depending vertices the vertex disappears as their dependency).
         *
         * @param vertex
         *            the vertex to mark as finished. It must not be
         *            {@code null}.
         */
        private void finished(Binding<T> vertex) {
            final Collection<Binding<T>> targets = outgoing.remove(vertex);
            if (targets == null) {
                return;
            }

            targets.forEach(target -> incoming.get(target).remove(vertex));
        }

        /**
         * Resolves all trees contained in the graph that depend on no cycle
         * with topological sort.
         *
         * @return {@code true} iff all remaining vertices have been resolved
         */
        private boolean resolveTrees() {
            final Set<Map.Entry<Binding<T>, Set<Binding<T>>>> entries = incoming.entrySet();
            boolean unstable; // Remains false as long as there were any updates and the graph is not fixed

            do {
                unstable = false;
                for (Iterator<Map.Entry<Binding<T>, Set<Binding<T>>>> it = entries.iterator(); it.hasNext();) {
                    final Map.Entry<Binding<T>, Set<Binding<T>>> next = it.next();
                    final Set<Binding<T>> sources = next.getValue();
                    if (!sources.isEmpty()) {
                        continue;
                    }

                    final Binding<T> vertex = next.getKey();

                    unstable = true; // Require another iteration to see if there are no nodes to remove
                    final Nullable<String> resolution = Nullable.of(vertex.apply(placeholder -> {
                        return resolved(linking.apply(placeholder, vertex.context()));
                    }));

                    final Nullable<String> previous = resolved.put(references.get(vertex), resolution);
                    assert (previous == null);
                    finished(vertex);
                    it.remove();
                }
            } while (unstable);

            return incoming.isEmpty();
        }

        /**
         * Resolves all given vertices using the handler for failed recursion.
         *
         * <p>
         * This method updates {@link #resolved} and {@link #outgoing}, but does
         * not update {@link #incoming}. The caller remains responsible for it.
         *
         * @param failed
         *            the vertices to resolve. It must not be {@code null}.
         */
        private void resolveRecursionFailures(Collection<Binding<T>> failed) {
            final Map<T, Nullable<String>> resolutions = new HashMap<>(failed.size());

            for (Binding<T> vertex : failed) {
                final T placeholder = references.get(vertex);
                assert !resolved.containsKey(placeholder); // Not done yet
                final Nullable<String> resolution = handleRecursionFailure(placeholder, vertex);
                final Nullable<String> previous = resolutions.put(placeholder, resolution);
                assert (previous == null);
                finished(vertex);
            }

            resolved.putAll(resolutions);
        }

        /**
         * Invokes the handler for failed recursion.
         *
         * @param reference
         *            the reference to handle. It must not be {@code null}.
         * @param definition
         *            the referred template definition. It must not be
         *            {@code null}.
         *
         * @return the result of the handler
         */
        private Nullable<String> handleRecursionFailure(T reference, Binding<T> definition) {
            return Nullable.of(recursionFailureHandler.handle(reference, definition, this::resolved));
        }

        /**
         * Discovers all vertices lying on a cycle.
         *
         * @return all vertices lying on a cycle
         */
        private Set<Binding<T>> discoverCycles() {
            final Set<Binding<T>> result = new HashSet<>();
            final Set<Binding<T>> visited = new HashSet<>();
            final List<Binding<T>> path = new ArrayList<>();
            final Map<Binding<T>, Integer> depth = new HashMap<>();
            // This might be a bit nicer with a class that holds all the data passed down...
            incoming.keySet().forEach(template -> discoverCycles(result::add, visited, template, path, depth));
            return result;
        }

        /**
         * Discovers cycles for the given vertex.
         *
         * @param found
         *            the callback to report all vertices of a cycle that has
         *            been discovered. It must not be {@code null}.
         * @param visited
         *            the set of visited vertices to prevent entering already
         *            explored vertices. It must not be {@code null}.
         * @param vertex
         *            the vertex to search the cycle from. It must not be
         *            {@code null}.
         * @param path
         *            the path searched so far. It must not be {@code null}.
         * @param depth
         *            the vertices on the path with the index into the path
         *            where a particular vertex was added. It must not be
         *            {@code null}.
         */
        private void discoverCycles(Consumer<? super Binding<T>> found, Set<Binding<T>> visited, Binding<T> vertex, List<Binding<T>> path, Map<Binding<T>, Integer> depth) {
            if (visited.contains(vertex)) {
                return;
            }

            final int currentDepth = path.size();
            final Integer existing = depth.putIfAbsent(vertex, currentDepth);
            if (existing != null) { // Found a cycle starting at the existing path index
                path.listIterator(existing).forEachRemaining(found);
                return;
            }

            path.add(vertex); // Next vertex on the path, continue recursively with adjacent vertices
            Optional.ofNullable(incoming.get(vertex)).ifPresent(dependsOn -> dependsOn.forEach(source -> {
                discoverCycles(found, visited, source, path, depth);
            }));

            // Done with adjacent vertices, remove this vertex from the path (tracking back)
            final Integer removed = depth.remove(vertex);
            assert (removed == currentDepth);
            path.remove(currentDepth);
            assert (path.size() == currentDepth);
            visited.add(vertex); // Do not enter this ever again!
        }

        /**
         * Adds a vertex including its dependencies for later resolution.
         *
         * @param reference
         *            the reference to resolve. It must not be {@code null}.
         * @param definition
         *            the definition of the reference. It must not be
         *            {@code null}.
         *
         * @return this instance
         */
        private Binding<T> putAll(T reference, Binding<T> definition) {
            assert (references != null);
            assert (vertices != null);
            assert (incoming != null);

            // First take care not to lose the existing definition. If the definition
            // exists, do nothing more. Otherwise we can use the cache or define that
            // template from scratch.

            final Binding<T> before = vertices.get(Objects.requireNonNull(reference));

            if (before != null) {
                // If the definition of a placeholder does not match, the lookup
                // is not consistent, which is definitely bad. But should it be
                // treated as a hard error? Is it worth the additional overhead?
                assert before.equals(definition);
                return before;
            }

            final Nullable<String> cached = cached(reference);

            if (cached != null) {
                // We can employ already cached resolutions, but we have to add them in
                // the graph to resolve to be able to refer to them in the graph scope
                return put(reference, cached.value(), Collections.emptySet());
            }

            final Set<String> placeholders = new HashSet<>();
            final String resolution = decompose(definition, placeholders::add);
            if (placeholders.isEmpty()) { // Optimize for a literal, so that we do not dive recursively
                return put(reference, resolution, Collections.emptySet());
            }

            put(reference, definition); // Before recursion!

            placeholders.forEach(placeholder -> {
                final T target = linking.apply(placeholder, definition.context());
                if (target != null) { // If the linking is stable, later we'll get null too and find nothing resolved
                    incoming.computeIfAbsent(definition, k -> new HashSet<>()).add(dereference(target));
                }
            });

            incoming.putIfAbsent(definition, Collections.emptySet()); // Ensure we have an existing incoming edges set
            return definition;
        }

        /**
         * Captures references of a template.
         *
         * @param template
         *            the parsed template. It must not be {@code null}.
         * @param placeholders
         *            the consumer to accept placeholder occurrences. It must
         *            not be {@code null}.
         *
         * @return the resolution with unresolved placeholders
         */
        private String decompose(Template template, Consumer<? super String> placeholders) {
            return template.apply(placeholder -> {
                placeholders.accept(placeholder);
                return null;
            });
        }

        /**
         * Returns the vertex according to the given reference after adding the
         * vertex in the graph recursively.
         *
         * @param reference
         *            the reference for the template. It must not be
         *            {@code null}.
         *
         * @return the vertex
         */
        private Binding<T> dereference(T reference) {
            assert (reference != null);
            assert (vertices != null);
            assert (incoming != null);

            final Nullable<String> cached = cached(reference);

            if (cached != null) {
                return put(reference, cached.value(), Collections.emptySet());
            }

            final Binding<T> found = lookup.apply(reference);
            if (found == null) { // Fallback preserves the original reference as a literal
                return put(reference, null, Collections.emptySet());
            }

            return putAll(reference, found);
        }

        /**
         * Adds a constant as a vertex.
         *
         * @param reference
         *            the placeholder. It must not be {@code null}.
         * @param value
         *            the value of the vertex. It must not be {@code null}.
         * @param dependsOn
         *            the set of dependencies. It must not be {@code null} and
         *            it must be a mutable set if not empty.
         *
         * @return the vertex
         */
        private Binding<T> put(T reference, String value, Set<Binding<T>> dependsOn) {
            final Binding<T> result = (value != null) ? Binding.of(value, reference) : MissingTemplate.bind(reference);
            incoming.put(result, dependsOn);
            return put(reference, result);
        }

        /**
         * Adds a template as a node.
         *
         * @param placeholder
         *            the placeholder. It must not be {@code null}.
         * @param template
         *            the template. It must not be {@code null}.
         *
         * @return the template
         */
        private Binding<T> put(T placeholder, Binding<T> template) {
            assert (template != null);
            assert (placeholder != null);
            assert (vertices.size() == references.size());
            references.put(template, placeholder);
            vertices.put(placeholder, template);
            assert (vertices.size() == references.size());
            return template;
        }
    }
}
