package dk.tbsalling.ais.filter;

import dk.tbsalling.ais.tracker.AISTrack;
import dk.tbsalling.aismessages.ais.messages.AISMessage;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;


public class FilterExpressionVisitor extends AisFilterBaseVisitor<BiPredicate<AISMessage, AISTrack>> {

    @Override public BiPredicate<AISMessage, AISTrack> visitAnd(AisFilterParser.AndContext ctx) {
        BiPredicate<AISMessage, AISTrack> left = visit(ctx.left);
        BiPredicate<AISMessage, AISTrack> right = visit(ctx.right);
        return left.and(right);
    }

    @Override
    public BiPredicate<AISMessage, AISTrack> visitMsgid(AisFilterParser.MsgidContext ctx) {
        ToIntFunction<AISMessage> lhs = aisMessage -> aisMessage.getMessageType().getCode();
        AisFilterParser.CompareToIntContext compareToOperator = ctx.compareToInt();
        int rhs = Integer.valueOf(ctx.INT().getText());
        return createCompareToInt(lhs, compareToOperator, rhs);
    }

    @Override
    public BiPredicate<AISMessage, AISTrack> visitMmsi(AisFilterParser.MmsiContext ctx)  {
        ToIntFunction<AISMessage> lhs = aisMessage -> aisMessage.getSourceMmsi().getMMSI().intValue();
        AisFilterParser.CompareToIntContext compareToOperator = ctx.compareToInt();
        int mmsi = Integer.valueOf(ctx.INT().getText());
        return createCompareToInt(lhs, compareToOperator, mmsi);
    }

    private static BiPredicate<AISMessage, AISTrack> createCompareToInt(ToIntFunction<AISMessage> lhs, AisFilterParser.CompareToIntContext compareToOperator, int rhs) {
        if (compareToOperator.eq() != null)
            return (aisMessage, aisTrack) -> lhs.applyAsInt(aisMessage) == rhs;
        else if (compareToOperator.neq() != null)
            return (aisMessage, aisTrack) -> lhs.applyAsInt(aisMessage) != rhs;
        else if (compareToOperator.lt() != null)
            return (aisMessage, aisTrack) -> lhs.applyAsInt(aisMessage) < rhs;
        else if (compareToOperator.lte() != null)
            return (aisMessage, aisTrack) -> lhs.applyAsInt(aisMessage) <= rhs;
        else if (compareToOperator.gte() != null)
            return (aisMessage, aisTrack) -> lhs.applyAsInt(aisMessage) >= rhs;
        else if (compareToOperator.gt() != null)
            return (aisMessage, aisTrack) -> lhs.applyAsInt(aisMessage) > rhs;
        else
            throw new IllegalStateException("Unknown comparison operator: " + compareToOperator.getText());
    }

}
