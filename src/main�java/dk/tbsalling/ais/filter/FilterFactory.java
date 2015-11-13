package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;

import java.util.function.Predicate;

/**
 * Created by tbsalling on 03/02/15.
 */
public final class FilterFactory {

    public static Predicate<AISMessage> newExpressionFilter(String expression) {
        return new ExpressionFilter(expression);
    }

}
