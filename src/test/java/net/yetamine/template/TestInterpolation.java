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

import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link Interpolation}.
 */
public final class TestInterpolation {

    /**
     * Tests {@link TemplateFormat#constant(String)} with the standard brackets.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param constant
     *            the constant to be produced. It must not be {@code null}.
     */
    @Test(dataProvider = "standardConstants")
    public void testStandardConstants(String template, String constant) {
        testConstants(Interpolation.standard(), template, constant);
    }

    @DataProvider(name = "standardConstants")
    public Object[][] standardConstants() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "${reference}", "$${reference}" },
            { "$${constant}", "$$${constant}" },
            { "Prefixed ${reference}", "Prefixed $${reference}" },
            { "Prefixed $${constant}", "Prefixed $$${constant}" },
            { "${reference} with suffix", "$${reference} with suffix" },
            { "$${constant} with suffix", "$$${constant} with suffix" },
            { "Infixed ${reference}.", "Infixed $${reference}." },
            { "Infixed $${constant}.", "Infixed $$${constant}." },
            { "A $${constant} and ${reference}.", "A $$${constant} and $${reference}." },
            { "Half-open $${constant", "Half-open $$${constant" },
            { "Half-open ${reference", "Half-open $${reference" },
            // Following cases have multiple solutions, this one is safer and more consistent with other results
            { "Half-open $${constant and ${more}", "Half-open $$${constant and $${more}" },
            { "Unintended ${reference and ${more}", "Unintended $${reference and $${more}" }
            // @formatter:on
        };
    }

    /**
     * Tests {@link TemplateFormat#constant(String)} with equal brackets.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param constant
     *            the constant to be produced. It must not be {@code null}.
     */
    @Test(dataProvider = "customConstants")
    public void testCustomConstants(String template, String constant) {
        testConstants(Interpolation.with("~", "~", "!"), template, constant);
    }

    @DataProvider(name = "customConstants")
    public Object[][] customConstants() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "~reference~", "!~reference!~" },
            { "!~constant!~", "!!~constant!!~" },
            { "Prefixed ~reference~", "Prefixed !~reference!~" },
            { "Prefixed !~constant!~", "Prefixed !!~constant!!~" },
            { "~reference~ with suffix", "!~reference!~ with suffix" },
            { "!~constant!~ with suffix", "!!~constant!!~ with suffix" },
            { "Infixed ~reference~.", "Infixed !~reference!~." },
            { "Infixed !~constant!~.", "Infixed !!~constant!!~." },
            { "A !~constant!~ and ~reference~.", "A !!~constant!!~ and !~reference!~." },
            { "Half-open !~constant", "Half-open !!~constant" },
            { "Half-open ~reference", "Half-open !~reference" },
            { "Half-open !~constant and ~more~", "Half-open !!~constant and !~more!~" },
            { "Half-open ~reference and ~more~", "Half-open !~reference and !~more!~" }
            // @formatter:on
        };
    }

    /**
     * Tests {@link TemplateFormat#constant(String)} with equal brackets and no
     * escaping.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param constant
     *            the constant to be produced. It must not be {@code null}.
     */
    @Test(dataProvider = "unescapableConstants")
    public void testUnescapableConstants(String template, String constant) {
        testConstants(Interpolation.with("%", "%"), template, constant);
    }

    @DataProvider(name = "unescapableConstants")
    public Object[][] unescapableConstants() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "%reference%", "%reference%" },
            { "Prefixed %reference%", "Prefixed %reference%" },
            { "%reference% with suffix", "%reference% with suffix" },
            { "Infixed %reference%.", "Infixed %reference%." },
            { "Half-open %reference", "Half-open %reference" },
            { "Unintended %reference and %more%", "Unintended %reference and %more%" }
            // @formatter:on
        };
    }

    /**
     * Tests all resolving possibilities with the standard brackets and with a
     * resolver that converts references to upper case.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param resolution
     *            the expected resolution. It must not be {@code null}.
     */
    @Test(dataProvider = "standardResolving")
    public void testStandardResolve(String template, String resolution) {
        testResolve(Interpolation.standard(), template, resolution);
    }

    @DataProvider(name = "standardResolving")
    public Object[][] standardResolving() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "${reference}", "REFERENCE" },
            { "$${constant}", "${constant}" },
            { "Prefixed ${reference}", "Prefixed REFERENCE" },
            { "Prefixed $${constant}", "Prefixed ${constant}" },
            { "${reference} with suffix", "REFERENCE with suffix" },
            { "$${constant} with suffix", "${constant} with suffix" },
            { "Infixed ${reference}.", "Infixed REFERENCE." },
            { "Infixed $${constant}.", "Infixed ${constant}." },
            { "A $${constant} and ${reference}.", "A ${constant} and REFERENCE." },
            { "Surrounded$${constant}.", "Surrounded${constant}." },
            { "Surrounded${reference}.", "SurroundedREFERENCE." },
            { "Surrounded$${constant}${reference}.", "Surrounded${constant}REFERENCE." },
            { "Half-open $${constant", "Half-open ${constant" },
            { "Half-open ${reference", "Half-open ${reference" },
            // Following cases have multiple solutions, this one is safer and more consistent with other results
            { "Half-open $${constant and ${more}", "Half-open ${constant and MORE" },
            { "Unintended ${reference and ${more}", "Unintended REFERENCE AND ${MORE" },
            { "No ${reference on dot${net}", "No REFERENCE ON DOT${NET" }
            // @formatter:on
        };
    }

    /**
     * Tests all resolving possibilities without brackets and with a resolver
     * that converts references to upper case.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param resolution
     *            the expected resolution. It must not be {@code null}.
     */
    @Test(dataProvider = "reducedResolving")
    public void testReducedResolve(String template, String resolution) {
        testResolve(Interpolation.reduced(), template, resolution);
    }

    @DataProvider(name = "reducedResolving")
    public Object[][] reducedResolving() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "$reference", "REFERENCE" },
            { "$$constant", "$constant" },
            { "Prefixed $reference", "Prefixed REFERENCE" },
            { "Prefixed $$constant", "Prefixed $constant" },
            { "$reference with suffix", "REFERENCE with suffix" },
            { "$$constant with suffix", "$constant with suffix" },
            { "Infixed $reference.", "Infixed REFERENCE." },
            { "Infixed $$constant.", "Infixed $constant." },
            { "A $$constant and $reference.", "A $constant and REFERENCE." },
            { "Surrounded$$constant.", "Surrounded$constant." },
            { "Surrounded$reference.", "SurroundedREFERENCE." },
            { "Surrounded$$constant$reference.", "Surrounded$constantREFERENCE." },
            // @formatter:on
        };
    }

    /**
     * Tests all resolving possibilities with equal brackets and with a resolver
     * that converts references to upper case.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param resolution
     *            the expected resolution. It must not be {@code null}.
     */
    @Test(dataProvider = "customResolvingWithBrackets")
    public void testCustomResolveWithBrackets(String template, String resolution) {
        testResolve(Interpolation.with("~", "~", "!"), template, resolution);
    }

    @DataProvider(name = "customResolvingWithBrackets")
    public Object[][] customResolvingWithBrackets() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "~reference~", "REFERENCE" },
            { "!~constant!~", "~constant~" },
            { "Prefixed ~reference~", "Prefixed REFERENCE" },
            { "Prefixed !~constant!~", "Prefixed ~constant~" },
            { "~reference~ with suffix", "REFERENCE with suffix" },
            { "!~constant!~ with suffix", "~constant~ with suffix" },
            { "Infixed ~reference~.", "Infixed REFERENCE." },
            { "Infixed !~constant!~.", "Infixed ~constant~." },
            { "A !~constant!~ and ~reference~.", "A ~constant~ and REFERENCE." },
            { "Surrounded!~constant!~.", "Surrounded~constant~." },
            { "Surrounded~reference~.", "SurroundedREFERENCE." },
            { "Surrounded!~constant!~~reference~.", "Surrounded~constant~REFERENCE." },
            { "Half-open !~constant", "Half-open ~constant" },
            { "Half-open ~reference", "Half-open ~reference" },
            { "Unintended ~reference and ~more~", "Unintended REFERENCE AND more~" },
            { "No ~reference on dot~net~", "No REFERENCE ON DOTnet~" }
            // @formatter:on
        };
    }

    /**
     * Tests all resolving possibilities without brackets which takes words and
     * with a resolver that converts references to upper case.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param resolution
     *            the expected resolution. It must not be {@code null}.
     */
    @Test(dataProvider = "customResolvingWithoutBrackets")
    public void testCustomResolveWithoutBrackets(String template, String resolution) {
        testResolve(Interpolation.with("~", c -> !Character.isWhitespace(c), "!"), template, resolution);
    }

    @DataProvider(name = "customResolvingWithoutBrackets")
    public Object[][] customResolvingWithoutBrackets() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "~reference", "REFERENCE" },
            { "!~constant", "~constant" },
            { "Prefixed ~reference", "Prefixed REFERENCE" },
            { "Prefixed !~constant", "Prefixed ~constant" },
            { "~reference with suffix", "REFERENCE with suffix" },
            { "!~constant with suffix", "~constant with suffix" },
            { "Infixed ~reference here.", "Infixed REFERENCE here." },
            { "Infixed !~constant here.", "Infixed ~constant here." },
            { "A !~constant and ~reference.", "A ~constant and REFERENCE." },
            { "Some !~special-constant.", "Some ~special-constant." },
            { "Some ~special-reference.", "Some SPECIAL-REFERENCE." },
            { "Surrounded!~constant~reference.", "Surrounded~constantREFERENCE." },
            { "Surrounded~reference!~constant.", "SurroundedREFERENCE~constant." },
            { "Surrounded~reference~more.", "SurroundedREFERENCEMORE." },
            { "A ~reference and ~empty one~", "A REFERENCE and EMPTY one" },
            { "A !~constant and !~empty one~", "A ~constant and ~empty one" },
            // @formatter:on
        };
    }

    /**
     * Tests all resolving possibilities with equal brackets with no escaping
     * and with a resolver that converts references to upper case.
     *
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param resolution
     *            the expected resolution. It must not be {@code null}.
     */
    @Test(dataProvider = "unescapableResolving")
    public void testUnescapableResolve(String template, String resolution) {
        testResolve(Interpolation.with("%", "%"), template, resolution);
    }

    @DataProvider(name = "unescapableResolving")
    public Object[][] unescapableResolving() {
        return new Object[][] {
            // @formatter:off
            { "", "" },
            { "literal", "literal" },
            { "%reference%", "REFERENCE" },
            { "Prefixed %reference%", "Prefixed REFERENCE" },
            { "%reference% with suffix", "REFERENCE with suffix" },
            { "Infixed %reference%.", "Infixed REFERENCE." },
            { "Surrounded%reference%.", "SurroundedREFERENCE." },
            { "Half-open %reference", "Half-open %reference" },
            { "Unintended %reference and %more%", "Unintended REFERENCE AND more%" },
            { "No %reference on dot%net%", "No REFERENCE ON DOTnet%" }
            // @formatter:on
        };
    }

    /**
     * Tests constant processing.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param constant
     *            the constant to be produced. It must not be {@code null}.
     */
    private static void testConstants(TemplateFormat format, String template, String constant) {
        final Optional<String> reproduction = format.reproduction(template);

        reproduction.ifPresent(s -> {
            Assert.assertEquals(s, constant);
            Assert.assertEquals(format.constant(template), constant);
        });

        reproduction.orElseGet(() -> {
            Assert.expectThrows(UnsupportedOperationException.class, () -> format.constant(template));
            return null; // Not interesting actually
        });

        Assert.assertEquals(format.parse(constant).toString(), constant);
        Assert.assertEquals(format.resolve(constant, s -> null), template);
    }

    /**
     * Tests all resolving possibilities with a resolver that converts
     * references to upper case.
     *
     * @param format
     *            the format to use. It must not be {@code null}.
     * @param template
     *            the template to process. It must not be {@code null}.
     * @param resolution
     *            the expected resolution. It must not be {@code null}.
     */
    private static void testResolve(TemplateFormat format, String template, String resolution) {
        final UnaryOperator<String> resolver = s -> s.toUpperCase(Locale.ROOT);
        Assert.assertEquals(format.parse(template).apply(resolver), resolution);
        Assert.assertEquals(format.resolve(template, resolver), resolution);
        Assert.assertEquals(format.with(resolver).apply(template), resolution);
    }
}
