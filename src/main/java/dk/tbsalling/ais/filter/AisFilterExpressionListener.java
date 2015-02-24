package dk.tbsalling.ais.filter;

import com.google.common.collect.Lists;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.function.Predicate;

public class AisFilterExpressionListener extends AisFilterBaseListener {

    private List<Predicate<AISMessage>> filters = Lists.newLinkedList();

    @Override
    public void enterFilterExpression(AisFilterParser.FilterExpressionContext ctx) {
    }
}
