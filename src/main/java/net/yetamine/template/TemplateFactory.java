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
 * Implements the default callback that serves as a template factory.
 *
 * <p>
 * All methods return instances of standard {@link Template} implementations,
 * except for {@link #none()} that returns {@code null}, which allows to use
 * simplified termination conditions with this callback implementation.
 */
public final class TemplateFactory implements TemplateCallback<Template> {

    /** Sole instance of this class. */
    private static final TemplateFactory INSTANCE = new TemplateFactory();

    /**
     * Creates a new instance.
     */
    private TemplateFactory() {
        // Default constructor
    }

    /**
     * Returns an instance.
     *
     * @return an instance
     */
    public static TemplateCallback<Template> instance() {
        return INSTANCE;
    }

    /**
     * @see net.yetamine.template.TemplateCallback#skipped(java.lang.String)
     */
    @Override
    public Template skipped(String value) {
        return constant(value, "");
    }

    /**
     * @see net.yetamine.template.TemplateCallback#literal(java.lang.String)
     */
    @Override
    public Template literal(String value) {
        return TemplateLiteral.of(value);
    }

    /**
     * @see net.yetamine.template.TemplateCallback#constant(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public Template constant(String definition, String value) {
        return TemplateConstant.instance(definition, value);
    }

    /**
     * @see net.yetamine.template.TemplateCallback#reference(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public Template reference(String definition, String reference) {
        return TemplateReference.instance(definition, reference);
    }

    /**
     * @see net.yetamine.template.TemplateCallback#none()
     */
    @Override
    public Template none() {
        return null;
    }
}
