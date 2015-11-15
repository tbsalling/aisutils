package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;

import java.util.function.Predicate;

/**
 * FilterFactory is a factory class which is intended to create different types
 * of AISMessage filters.
 *
 * Currently it supports the creation of ExpressionFilters, which are AISMessage
 * filters based on a grammar of free-text expressions.
 *
 * @author Thomas Borg Salling
 * @see ExpressionFilter
 */
public final class FilterFactory {

    public static Predicate<AISMessage> newExpressionFilter(String expression) {
        return new ExpressionFilter(expression);
    }

}
