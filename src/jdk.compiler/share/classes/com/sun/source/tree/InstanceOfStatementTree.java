package com.sun.source.tree;

import jdk.internal.javac.PreviewFeature;

/**
 * A tree node for an {@code instanceof} statement.
 *
 * For example:
 * <pre>
 *   <em>expression</em> instanceof <em>record pattern</em>;
 * </pre>
 *
 * @jls 14.22 The {@code instanceof} Statement
 *
 * @author Aggelos Biboudis
 * @since 23
 */
public interface InstanceOfStatementTree extends StatementTree {
    /**
     * Returns the pattern for the {@code instanceof} statement.
     * @return pattern
     */
    @PreviewFeature(feature=PreviewFeature.Feature.PATTERN_DECLARATIONS, reflective=true)
    Tree getPattern();

    /**
     * Returns the expression to be pattern matched.
     * @return the expression
     */
    @PreviewFeature(feature=PreviewFeature.Feature.PATTERN_DECLARATIONS, reflective=true)
    ExpressionTree getExpression();

    /**
     * Returns the type for which to check.
     * @return the type
     * @see #getPattern()
     */
    @PreviewFeature(feature=PreviewFeature.Feature.PATTERN_DECLARATIONS, reflective=true)
    Tree getType();
}