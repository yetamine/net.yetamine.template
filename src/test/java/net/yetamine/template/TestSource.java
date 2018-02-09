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

import java.util.function.Predicate;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests {@link Source}.
 */
public final class TestSource {

    /**
     * Tests {@code isLiteral}.
     */
    @Test
    public void testIsLiteral() {
        final TemplateFormat format = Interpolation.standard();

        final Predicate<String> literal = Source.isLiteral(format);
        Assert.assertTrue(literal.test(""));
        Assert.assertTrue(literal.test("This is a literal."));
        Assert.assertFalse(literal.test("Symbols like $${this escaped one} violate literals."));
        Assert.assertFalse(literal.test("Symbols like ${placeholders} are definitely not literals."));

        // @formatter:off
        Assert.assertTrue(Source.parse("", format).isLiteral());
        Assert.assertTrue(Source.parse("This is a literal.", format).isLiteral());
        Assert.assertFalse(Source.parse("Symbols like $${this escaped one} violate literals.", format).isLiteral());
        Assert.assertFalse(Source.parse("Symbols like ${placeholders} are definitely not literals.", format).isLiteral());
        // @formatter:on
    }
}
