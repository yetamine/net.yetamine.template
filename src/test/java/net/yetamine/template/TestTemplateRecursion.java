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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link TemplateRecursion}.
 */
public final class TestTemplateRecursion {

    /** Definitions forming a dependency graph with most cases. */
    private static final Map<String, String> DEFINITIONS;
    static {
        final Map<String, String> definitions = new HashMap<>();

        definitions.put("L1.1", "(L1.1)");
        definitions.put("L1.2", "(L1.2)");
        definitions.put("C1.1", "(${C1.2})");
        definitions.put("C1.2", "(${C1.1} ${L1.1})");
        definitions.put("C1.3", "(${C1.3})");
        definitions.put("T1.1", "(${L1.1} ${L1.2})");
        definitions.put("T1.2", "(${L1.1} ${T1.1})");
        definitions.put("T1.3", "(${T1.2} ${C1.1} ${C1.3})");
        definitions.put("T1.4", "(${L1.1} ${MISSING} ${L1.2})");

        definitions.put("L2.1", "(L2.1)");
        definitions.put("L2.2", "(L2.2)");
        definitions.put("C2.1", "(${C2.2})");
        definitions.put("C2.2", "(${C2.3} ${C1.1})");
        definitions.put("C2.3", "(${C2.1})");
        definitions.put("T2.1", "(${L2.1} ${C2.1})");
        definitions.put("T2.2", "(${T2.2} ${C2.2})");

        definitions.put("L3.1", "(L3.1)");
        definitions.put("L3.2", "(L3.2)");
        definitions.put("T3.1", "(${T1.1} ${L3.1})");
        definitions.put("C3.1.1", "(${C3.1.2})");
        definitions.put("C3.1.2", "(${C3.1.3})");
        definitions.put("C3.1.3", "(${C3.1.1} ${C3.2.1})");
        definitions.put("C3.2.1", "(${C3.2.2})");
        definitions.put("C3.2.2", "(${C3.2.3})");
        definitions.put("C3.2.3", "(${C3.2.1} ${C3.1.1})");
        definitions.put("T3.2", "(${T3.1} ${C3.1.1})");
        definitions.put("T3.3", "(${T3.2} ${C1.1} ${C1.2})");

        // Test custom fallback
        definitions.put("constant", "${L1.1}");     // To test constants
        definitions.put("override", "overridden");  // To test overriding
        definitions.put("preserve", "unexpanded");  // To test hiding a value

        // Test relative placeholders
        definitions.put("/a", "(a)");
        definitions.put("/b", "(b)");
        definitions.put("/a/a", "(a/a)");
        definitions.put("/a/b", "(a/b)");
        definitions.put("/a/c", "(${b})");
        definitions.put("/a/d", "(${/b})");
        definitions.put("/c", "(${a/a})");
        definitions.put("/d/a", "(${b})");
        definitions.put("/d/b", "(${c})");
        definitions.put("/d/c", "(${/d/a})");

        DEFINITIONS = Collections.unmodifiableMap(definitions);
    }

    /**
     * Tests {@link TemplateRecursion} with default settings.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     * @param templates
     *            the templates and their resolutions using the given
     *            definitions. It must not be {@code null}.
     */
    @Test(dataProvider = "defaults")
    public void testDefaults(TemplateRecursion.Source<?> source, Map<String, String> templates) {
        test(templates, TemplateRecursion.with(source).build());
    }

    /**
     * Tests {@link TemplateRecursion} with default settings, but caching on.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     * @param templates
     *            the templates and their resolutions using the given
     *            definitions. It must not be {@code null}.
     */
    @Test(dataProvider = "defaults")
    public void testDefaultsWithCaching(TemplateRecursion.Source<?> source, Map<String, String> templates) {
        test(templates, TemplateRecursion.with(source).caching(true).build());
    }

    @SuppressWarnings("javadoc")
    @DataProvider(name = "defaults")
    public Object[][] defaults() {
        final Map<String, String> templates = new LinkedHashMap<>(); // Keep the traversal stable

        templates.put("${MISSING}", "${MISSING}");

        templates.put("${L1.1}", "(L1.1)");
        templates.put("${L1.2}", "(L1.2)");
        templates.put("${C1.1}", "${C1.1}");
        templates.put("${C1.2}", "${C1.2}");
        templates.put("${C1.3}", "${C1.3}");
        templates.put("${T1.1}", "((L1.1) (L1.2))");
        templates.put("${T1.2}", "((L1.1) ((L1.1) (L1.2)))");
        templates.put("${T1.3}", "(((L1.1) ((L1.1) (L1.2))) ${C1.1} ${C1.3})");
        templates.put("${T1.4}", "((L1.1) ${MISSING} (L1.2))");

        templates.put("${L2.1}", "(L2.1)");
        templates.put("${L2.2}", "(L2.2)");
        templates.put("${C2.1}", "${C2.1}");
        templates.put("${C2.2}", "${C2.2}");
        templates.put("${C2.3}", "${C2.3}");
        templates.put("${T2.1}", "((L2.1) ${C2.1})");
        templates.put("${T2.2}", "${T2.2}");

        templates.put("${L3.1}", "(L3.1)");
        templates.put("${L3.2}", "(L3.2)");
        templates.put("${T3.1}", "(((L1.1) (L1.2)) (L3.1))");
        templates.put("${C3.1.1}", "${C3.1.1}");
        templates.put("${C3.1.2}", "${C3.1.2}");
        templates.put("${C3.1.3}", "${C3.1.3}");
        templates.put("${C3.2.1}", "${C3.2.1}");
        templates.put("${C3.2.2}", "${C3.2.2}");
        templates.put("${C3.2.3}", "${C3.2.3}");
        templates.put("${T3.2}", "((((L1.1) (L1.2)) (L3.1)) ${C3.1.1})");
        templates.put("${T3.3}", "(((((L1.1) (L1.2)) (L3.1)) ${C3.1.1}) ${C1.1} ${C1.2})");

        return new Object[][] { { TemplateRecursion.source().templates(DEFINITIONS::get), templates } };
    }

    /**
     * Tests {@link TemplateRecursion} with fallbacks.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     * @param templates
     *            the templates and their resolutions using the given
     *            definitions. It must not be {@code null}.
     */
    @Test(dataProvider = "fallbacks")
    public void testFallbacks(TemplateRecursion.Source<?> source, Map<String, String> templates) {
        test(templates, TemplateRecursion.with(source).build());
    }

    @SuppressWarnings("javadoc")
    @DataProvider(name = "fallbacks")
    public Object[][] fallbacks() {
        final Map<String, String> templates = new LinkedHashMap<>(); // Keep the traversal stable

        templates.put("${MISSING}", "${MISSING}");
        templates.put("${missing}", "not really");
        templates.put("${constant}", "${L1.1}");
        templates.put("${override}", "hidden");
        templates.put("${preserve}", "${preserve}");
        templates.put("${L1.1}", "(L1.1)");

        final TemplateRecursion.Source<String> source = TemplateRecursion.source()
                .onDeliveryMissed(reference -> "missing".equals(reference) ? "not really" : null)
                .constants(reference -> "constant".equals(reference) ? DEFINITIONS.get(reference) : null)
                .templates(reference -> {
                    switch (reference) {
                        case "override":
                            return "hidden";

                        case "preserve":
                            return null;

                        default:
                            return DEFINITIONS.get(reference);
                    }
                });

        return new Object[][] { { source, templates } };
    }

    /**
     * Tests {@link TemplateRecursion} with an override for cycles.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     * @param templates
     *            the templates and their resolutions using the given
     *            definitions. It must not be {@code null}.
     */
    @Test(dataProvider = "recusionFailure")
    public void testRecursionFailure(TemplateRecursion.Source<?> source, Map<String, String> templates) {
        test(templates, builderWithOverriddenRecursionFailure(source).build());
    }

    /**
     * Tests {@link TemplateRecursion} with an override for cycles and with
     * caching on.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     * @param templates
     *            the templates and their resolutions using the given
     *            definitions. It must not be {@code null}.
     */
    @Test(dataProvider = "recusionFailure")
    public void testRecursionFailureWithCaching(TemplateRecursion.Source<?> source, Map<String, String> templates) {
        test(templates, builderWithOverriddenRecursionFailure(source).caching(true).build());
    }

    @SuppressWarnings("javadoc")
    @DataProvider(name = "recusionFailure")
    public Object[][] recursionOverrides() {
        final Map<String, String> templates = new LinkedHashMap<>(); // Keep the traversal stable

        templates.put("${L1.1}", "(L1.1)");
        templates.put("${L1.2}", "(L1.2)");
        templates.put("${C1.1}", "#C1.1!");
        templates.put("${C1.2}", "#C1.2!");
        templates.put("${C1.3}", "#C1.3!");
        templates.put("${T1.1}", "((L1.1) (L1.2))");
        templates.put("${T1.2}", "((L1.1) ((L1.1) (L1.2)))");
        templates.put("${T1.3}", "(((L1.1) ((L1.1) (L1.2))) #C1.1! #C1.3!)");
        templates.put("${T1.4}", "((L1.1) ${MISSING} (L1.2))");

        templates.put("${L2.1}", "(L2.1)");
        templates.put("${L2.2}", "(L2.2)");
        templates.put("${C2.1}", "#C2.1!");
        templates.put("${C2.2}", "#C2.2!");
        templates.put("${C2.3}", "#C2.3!");
        templates.put("${T2.1}", "((L2.1) #C2.1!)");
        templates.put("${T2.2}", "#T2.2!");

        templates.put("${L3.1}", "(L3.1)");
        templates.put("${L3.2}", "(L3.2)");
        templates.put("${T3.1}", "(((L1.1) (L1.2)) (L3.1))");
        templates.put("${C3.1.1}", "#C3.1.1!");
        templates.put("${C3.1.2}", "#C3.1.2!");
        templates.put("${C3.1.3}", "#C3.1.3!");
        templates.put("${C3.2.1}", "#C3.2.1!");
        templates.put("${C3.2.2}", "#C3.2.2!");
        templates.put("${C3.2.3}", "#C3.2.3!");
        templates.put("${T3.2}", "((((L1.1) (L1.2)) (L3.1)) #C3.1.1!)");
        templates.put("${T3.3}", "(((((L1.1) (L1.2)) (L3.1)) #C3.1.1!) #C1.1! #C1.2!)");

        return new Object[][] { { TemplateRecursion.source().templates(DEFINITIONS::get), templates } };
    }

    /**
     * Returns a builder for making {@link TemplateRecursion} with a recursion
     * failure override that returns a replacements that includes the name of
     * the failing placeholder.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     *
     * @return the builder
     */
    private static TemplateRecursion.Builder<?> builderWithOverriddenRecursionFailure(TemplateRecursion.Source<?> source) {
        return TemplateRecursion.with(source).onRecursionFailure((r, t, d) -> String.format("#%s!", r));
    }

    /**
     * Tests {@link TemplateRecursion} with relative placeholders.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     * @param templates
     *            the templates and their resolutions using the given
     *            definitions. It must not be {@code null}.
     */
    @Test(dataProvider = "relativePlaceholders")
    public void testRelativePlaceholders(TemplateRecursion.Source<?> source, Map<String, String> templates) {
        test(templates, TemplateRecursion.with(source).build());
    }

    /**
     * Tests {@link TemplateRecursion} with relative placeholders and with
     * caching on.
     *
     * @param source
     *            the source of templates to be referred. It must not be
     *            {@code null}.
     * @param templates
     *            the templates and their resolutions using the given
     *            definitions. It must not be {@code null}.
     */
    @Test(dataProvider = "relativePlaceholders")
    public void testRelativePlaceholdersWithCaching(TemplateRecursion.Source<?> source, Map<String, String> templates) {
        test(templates, TemplateRecursion.with(source).caching(true).build());
    }

    @SuppressWarnings("javadoc")
    @DataProvider(name = "relativePlaceholders")
    public Object[][] relativePlaceholders() {
        final Map<String, String> templates = new LinkedHashMap<>(); // Keep the traversal stable

        templates.put("${/a}", "(a)");
        templates.put("${/b}", "(b)");
        templates.put("${/a/a}", "(a/a)");
        templates.put("${/a/b}", "(a/b)");
        templates.put("${/a/c}", "((a/b))");
        templates.put("${/a/d}", "((b))");
        templates.put("${/c}", "((a/a))");
        templates.put("${/d/a}", "${/d/a}");
        templates.put("${/d/b}", "${/d/b}");
        templates.put("${/d/c}", "${/d/c}");

        templates.put("${a}", "(a)");
        templates.put("${b}", "(b)");
        templates.put("${a/a}", "(a/a)");
        templates.put("${a/b}", "(a/b)");
        templates.put("${a/c}", "((a/b))");
        templates.put("${a/d}", "((b))");
        templates.put("${c}", "((a/a))");
        templates.put("${d/a}", "${d/a}");
        templates.put("${d/b}", "${d/b}");
        templates.put("${d/c}", "${d/c}");

        templates.put("${L1.1}", "${L1.1}"); // Keep unresolved

        // Makes absolute references (starting with '/') from relative by resolving them as siblings if possible
        final TemplateRecursion.Linking<String> linking = (placeholder, context) -> {
            if (placeholder.startsWith("/")) {
                return placeholder;
            }

            if (placeholder.isEmpty() || Character.isUpperCase(placeholder.charAt(0))) {
                return null; // Not a valid placeholder, keep as it is
            }

            if (context == null) {
                return '/' + placeholder; // No context, try to make absolute
            }

            assert context.startsWith("/");
            final List<String> components = Arrays.asList(context.split("/"));
            final int size = components.size();
            if (size == 0) { // Would be just "/"
                return '/' + placeholder;
            }

            components.set(size - 1, placeholder);
            return components.stream().collect(Collectors.joining("/"));
        };

        return new Object[][] { { TemplateRecursion.source(linking).templates(DEFINITIONS::get), templates } };
    }

    /**
     * Resolves all templates.
     *
     * @param templates
     *            the templates and their resolutions using the given resolving
     *            strategy. It must not be {@code null}.
     * @param resolver
     *            the resolving strategy to use. It must not be {@code null}.
     */
    private static void test(Map<String, String> templates, UnaryOperator<String> resolver) {
        final TemplateResolving resolving = Interpolation.standard().with(resolver);
        templates.forEach((template, resolved) -> Assert.assertEquals(resolving.apply(template), resolved));
    }
}
