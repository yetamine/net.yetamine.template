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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests {@link TemplateReference}.
 */
public final class TestTemplateReference {

    /**
     * Tests the core functionality.
     */
    @Test
    public void test() {
        final Template reference = TemplateReference.instance("reference", "value");
        Assert.assertEquals(reference.apply(s -> null), reference.toString());
        Assert.assertEquals(reference.apply(s -> "symbol"), "symbol");
        Assert.assertEquals(reference.apply(s -> "value".equals(s) ? "symbol" : "failed"), "symbol");
        Assert.assertEquals(reference.toString(), "reference");
    }
}
