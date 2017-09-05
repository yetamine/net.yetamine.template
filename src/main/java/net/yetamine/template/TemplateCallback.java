package net.yetamine.template;

/**
 * Defines a callback in a visitor style that allows to react on different types
 * of templates, or rather fragments of a template.
 *
 * <p>
 * This kind of callback can be useful for decomposing a linear template
 * representation, e.g., during parsing a string form with meta-symbols.
 *
 * @param <R>
 *            the type of the callback result
 */
public interface TemplateCallback<R> {

    /**
     * Handles an occurrence of an insignificant input part.
     *
     * @param value
     *            the part of the input that has been skipped. It must not be
     *            {@code null}.
     *
     * @return the result to pass
     */
    R skipped(String value);

    /**
     * Handles an occurrence of a literal.
     *
     * @param value
     *            the literal value. It must not be {@code null}.
     *
     * @return the result to pass
     */
    R literal(String value);

    /**
     * Handles an occurrence of a constant.
     *
     * @param definition
     *            the definition of the constant. It must not be {@code null}.
     * @param value
     *            the value of the constant. It must not be {@code null}.
     *
     * @return the result to pass
     */
    R constant(String definition, String value);

    /**
     * Handles an occurrence of a reference.
     *
     * @param definition
     *            the definition of the constant. It must not be {@code null}.
     * @param reference
     *            the reference. It must not be {@code null}.
     *
     * @return the result to pass
     */
    R reference(String definition, String reference);

    /**
     * Handles missing occurrence of a template, usually when the source has
     * been completely consumed.
     *
     * @return the result to pass
     */
    R none();
}
