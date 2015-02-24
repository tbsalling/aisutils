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

import ;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dk.tbsalling.ais.filter.AisFilterParser.FilterExpressionContext;
import dk.tbsalling.ais.tracker.AisTrack;
import dk.tbsalling.ais.tracker.AisTracker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * AisFilter is a collection of Java 8 predicates which are capable of stateful filtering
 * of AIS messages.
 */
@ThreadSafe
public class AisExpressionFilter implements Predicate<AISMessage> {

    private final static Logger LOG = LoggerFactory.getLogger(AisExpressionFilter.class);

    private final List<com.google.common.base.Predicate<AisTrack>> subFilters = Lists.newLinkedList();

    private final AisTracker tracker = new AisTracker();

    public AisExpressionFilter(String filterExpression) {
        // Create the lexer
        AisFilterLexer lexer = new AisFilterLexer(new ANTLRInputStream(filterExpression));

        // Get a list of matched tokens
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Pass the tokens to the parser
        AisFilterParser parser = new AisFilterParser(tokens);

        // Specify the entry point
        FilterExpressionContext filterExpressionContext = parser.filterExpression();



        // begin parsing at filterExpression rule
        ParseTree parseTree = parser.filterExpression();

        new AisFilterBaseVisitor<>().visit(parseTree) ;

        // Attach listener and walk it
        AisFilterExpressionListener listener = new AisFilterExpressionListener();

        //AisFilterBaseVisitor aisFilterBaseVisitor = new AisFilterBaseVisitor();

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, filterExpressionContext);

    }
    @Override
    public boolean test(AISMessage aisMessage) {
        if (aisMessage instanceof DynamicDataReport || aisMessage instanceof StaticDataReport) {
            tracker.update(aisMessage);
            AisTrack track = tracker.getAisTrack(aisMessage.getSourceMmsi().getMMSI());
            return Pr
        }
        return Predicates.and(subFilters).apply(aisMessage);
    }
}
