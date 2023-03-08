/*
 * AISUtils
 * - a java-based library for processing of AIS messages received from digital
 * VHF radio traffic related to maritime navigation and safety in compliance with ITU 1371.
 *
 * (C) Copyright 2011- by S-Consult ApS, DK31327490, http://s-consult.dk, Denmark.
 *
 * Released under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * For details of this license see the nearby LICENCE-full file, visit http://creativecommons.org/licenses/by-nc-sa/3.0/
 * or send a letter to Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 *
 * NOT FOR COMMERCIAL USE!
 * Contact sales@s-consult.dk to obtain a commercially licensed version of this software.
 *
 */

package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.function.Predicate;

/**
 * ExpressionFilter is a type of Predicate which can be used to filter AISMessages
 * based on a free-text expression.
 *
 * The filtering is stateful in the sense that vessel-related messages are tracked
 * so that e.g. a vessel's position, course and speed is taken into account even
 * when static messages are processed.
 *
 * An ExpressionFilter cannot be created directly, but is instantiated through
 * the FilterFactory.newExpressionFilter(...) method.
 *
 * The grammar currently supports the following fields:
 * - msgid
 * - mmsi
 * - sog
 * - cog
 * - lat
 * - lng
 *
 * And the following operators:
 *    <       less than
 *    <=      less than or equals
 *    =       equals
 *    >=      larger than or equals
 *    >       larger than
 *    !=      not equals
 *    in      in set
 *    not in  not in set
 *
 * And the following boolean expressions:
 *    and
 *    or
 *
 * Example expressions:
 *    "sog>5.0"
 *    "lat>55.0 and lat<56.0 and lng>9.0 and lng<11.0
 *    "msgid in (1, 2, 3, 5)"
 *    "mmsi not in (219001000, 219000000)"
 *
 * @author Thomas Borg Salling
 * @see FilterFactory
 */
class ExpressionFilter implements Predicate<AISMessage> {

    private final Predicate<AISMessage> filter;

    private ExpressionFilter() {
        filter = null;
    }

    ExpressionFilter(String filterExpression) {
        // Create the lexer
        AisFilterLexer lexer = new AisFilterLexer(CharStreams.fromString(filterExpression));

        // Get a list of matched tokens
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Pass the tokens to the parser
        AisFilterParser parser = new AisFilterParser(tokens);

        // Specify the entry point
        // FilterExpressionContext filterExpressionContext = parser.filterExpression();

        // begin parsing at filterExpression rule
        ParseTree parseTree = parser.filterExpression();

        filter = new FilterExpressionVisitor().visit(parseTree);

        if (filter == null)
            throw new IllegalStateException("filter == null");

    }

    /**
     * Test an incoming aisMessage against the filter expression.
     * The algorithm is stateful, so that previously detected positions
     * for a given mmsi are remembered across messages.
     *
     * @param aisMessage
     * @return true if the aisMessage satisfies the filter expression; false otherwise.
     */
    @Override
    public boolean test(AISMessage aisMessage) {
        return filter.test(aisMessage);
    }
}
