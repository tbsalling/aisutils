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
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.function.Predicate;

/**
 * AisFilter is a collection of Java 8 predicates which are capable of stateful filtering
 * of AIS messages.
 */
@ThreadSafe
public class ExpressionFilter implements Predicate<AISMessage> {

    private final static Logger LOG = LoggerFactory.getLogger(ExpressionFilter.class);

    private final Predicate<AISMessage> filter;

    private ExpressionFilter() {
        filter = null;
    }

    ExpressionFilter(String filterExpression) {
        // Create the lexer
        AisFilterLexer lexer = new AisFilterLexer(new ANTLRInputStream(filterExpression));

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
