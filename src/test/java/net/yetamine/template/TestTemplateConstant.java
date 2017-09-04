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
 * Tests {@link TemplateConstant}.
 */
public final class TestTemplateConstant {

    /**
     * Tests the core functionality.
     */
    @Test
    public void test() {
        final Template constant = TemplateConstant.instance("definition", "value");
        Assert.assertEquals(constant.apply(s -> null), "value");
        Assert.assertEquals(constant.apply(s -> "symbol"), "value");
        Assert.assertEquals(constant.toString(), "definition");
    }
}
