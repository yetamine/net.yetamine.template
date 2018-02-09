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

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a template definition which includes the format of the template
 * besides the symbolic representation of the template itself.
 */
public interface TemplateDefinition extends Template {

    /**
     * Returns the format of the template definition.
     *
     * @return the template format
     */
    TemplateFormat format();

    /**
     * Returns a parser for the definition.
     *
     * <p>
     * This method is equivalent to {@code format().parser(toString())}, which
     * is the default implementation.
     *
     * @return the parser
     */
    default TemplateParser parser() {
        return format().parser(toString());
    }

    /**
     * Creates a new definition from an existing representation.
     *
     * <p>
     * The caller takes the responsibility for the format correctness. The
     * format should be the same as the format used for creating the given
     * template representation, so that for any {@code f} is true:
     *
     * <pre>
     * template.apply(f).equals(format.parse(template.toString()).apply(f))
     * </pre>
     *
     * Use rather {@link #unparsed(String, TemplateFormat)} for an unparsed
     * representation. This method should be used for parsed representations
     * only, when a grouping of the parsed representation with its format is
     * needed.
     *
     * @param template
     *            the template to encapsulate. It must not be {@code null}.
     * @param format
     *            the format that the template should declare. It must not be
     *            {@code null}.
     *
     * @return the definition
     */
    static TemplateDefinition parsed(Template template, TemplateFormat format) {
        return new TemplateDefinitionParsed(template, format);
    }

    /**
     * Parses a template with the given format.
     *
     * @param template
     *            the template to parse. It must not be {@code null}.
     * @param format
     *            the format for parsing the template. It must not be
     *            {@code null}.
     *
     * @return the definition
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    static TemplateDefinition parsed(String template, TemplateFormat format) {
        return new TemplateDefinitionParsed(template, format);
    }

    /**
     * Creates a new definition that parses the given template on demand once.
     *
     * @param template
     *            the template to parse. It must not be {@code null}.
     * @param format
     *            the format for parsing the template. It must not be
     *            {@code null}.
     *
     * @return the definition
     */
    static TemplateDefinition unparsed(String template, TemplateFormat format) {
        return new TemplateDefinitionUnparsed(template, format);
    }
}

/**
 * Encapsulates a parsed template.
 */
final class TemplateDefinitionParsed implements TemplateDefinition {

    /** Underlying template. */
    private final Template template;
    /** Declared format of the template. */
    private final TemplateFormat format;

    /**
     * Creates a new instance.
     *
     * @param templateDefinition
     *            the template to encapsulate. It must not be {@code null}.
     * @param templateFormat
     *            the format that the template should declare. It must not be
     *            {@code null} and it should be the same format that has been
     *            used to create the given template.
     */
    public TemplateDefinitionParsed(Template templateDefinition, TemplateFormat templateFormat) {
        template = Objects.requireNonNull(templateDefinition);
        format = Objects.requireNonNull(templateFormat);
    }

    /**
     * Creates a new instance.
     *
     * @param templateDefinition
     *            the template to parse. It must not be {@code null}.
     * @param templateFormat
     *            the format for parsing the template. It must not be
     *            {@code null}.
     *
     * @throws TemplateSyntaxException
     *             if the parser fails to parse the template
     */
    public TemplateDefinitionParsed(String templateDefinition, TemplateFormat templateFormat) {
        this(templateFormat.parse(templateDefinition), templateFormat);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return template.toString();
    }

    /**
     * @see net.yetamine.template.Template#apply(java.util.function.Function)
     */
    public String apply(Function<? super String, String> resolver) {
        return template.apply(resolver);
    }

    /**
     * @see net.yetamine.template.TemplateDefinition#format()
     */
    public TemplateFormat format() {
        return format;
    }
}

/**
 * Encapsulates a template parsed on demand.
 */
final class TemplateDefinitionUnparsed implements TemplateDefinition {

    /** Template definition. */
    private final String definition;
    /** Format of the template. */
    private final TemplateFormat format;
    /** Parsed underlying template. */
    private volatile Template template;

    /**
     * Creates a new instance.
     *
     * @param templateDefinition
     *            the template to parse. It must not be {@code null}.
     * @param templateFormat
     *            the format for parsing the template. It must not be
     *            {@code null}.
     */
    public TemplateDefinitionUnparsed(String templateDefinition, TemplateFormat templateFormat) {
        definition = Objects.requireNonNull(templateDefinition);
        format = Objects.requireNonNull(templateFormat);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return definition;
    }

    /**
     * @see net.yetamine.template.Template#apply(java.util.function.Function)
     */
    public String apply(Function<? super String, String> resolver) {
        return parsed().apply(resolver);
    }

    /**
     * @see net.yetamine.template.TemplateDefinition#format()
     */
    public TemplateFormat format() {
        return format;
    }

    /**
     * Provides the parsed template.
     *
     * @return the parsed template
     *
     * @throws TemplateResolvingException
     *             if the parsing fails
     */
    private Template parsed() {
        Template result = template;

        if (result == null) {
            try { // Prevent throwing different exceptions!
                result = format.parse(definition);
            } catch (TemplateSyntaxException e) {
                throw new TemplateResolvingException(e);
            }

            template = result;
        }

        return result;
    }
}
