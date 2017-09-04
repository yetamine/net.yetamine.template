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

/**
 * Implements the default parsing callback.
 *
 * <p>
 * All methods return instances of standard {@link Template} implementations,
 * except for {@link #done()} that returns {@code null}, which allows to use
 * simplified termination conditions with this callback implementation.
 */
public final class TemplateFactoryCallback implements TemplateParser.Callback<Template> {

    /** Sole instance of this class. */
    private static final TemplateFactoryCallback INSTANCE = new TemplateFactoryCallback();

    /**
     * Creates a new instance.
     */
    private TemplateFactoryCallback() {
        // Default constructor
    }

    /**
     * Returns an instance.
     *
     * @return an instance
     */
    public static TemplateParser.Callback<Template> instance() {
        return INSTANCE;
    }

    /**
     * @see net.yetamine.template.TemplateParser.Callback#skipped(java.lang.String)
     */
    public Template skipped(String value) {
        return TemplateConstant.instance(value, "");
    }

    /**
     * @see net.yetamine.template.TemplateParser.Callback#literal(java.lang.String)
     */
    public Template literal(String value) {
        return TemplateLiteral.of(value);
    }

    /**
     * @see net.yetamine.template.TemplateParser.Callback#constant(java.lang.String,
     *      java.lang.String)
     */
    public Template constant(String definition, String value) {
        return TemplateConstant.instance(definition, value);
    }

    /**
     * @see net.yetamine.template.TemplateParser.Callback#reference(java.lang.String,
     *      java.lang.String)
     */
    public Template reference(String definition, String reference) {
        return TemplateReference.instance(definition, reference);
    }

    /**
     * @see net.yetamine.template.TemplateParser.Callback#done()
     */
    public Template done() {
        return null;
    }
}
