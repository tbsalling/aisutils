package dk.tbsalling.ais.filter;

import dk.tbsalling.ais.tracker.AISTrack;
import dk.tbsalling.ais.tracker.AISTracker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;


public class FilterExpressionVisitor extends AisFilterBaseVisitor<Predicate<AISMessage>> {

    private final static Logger LOG = LoggerFactory.getLogger(FilterExpressionVisitor.class);

    private final AISTracker tracker = new AISTracker();

    @Override
    public Predicate<AISMessage> visitAndOr(AisFilterParser.AndOrContext ctx) {
        Predicate<AISMessage> left = computePredicate(ctx.left);
        Predicate<AISMessage> right = computePredicate(ctx.right);

        if (ctx.AND() != null && !ctx.AND().isEmpty())
            return left.and(right);
        else if (ctx.OR() != null && !ctx.OR().isEmpty())
            return left.or(right);
        else
            throw new IllegalStateException("Unknown operator: " + ctx.op.getText());
    }

    @Override
    public Predicate<AISMessage> visitMsgid(AisFilterParser.MsgidContext ctx) {
        ToIntFunction<AISMessage> lhs = aisMessage -> aisMessage.getMessageType().getCode();
        AisFilterParser.CompareToContext compareToOperator = ctx.compareTo();
        int rhs = Integer.valueOf(ctx.INT().getText());
        return createCompareToInt(null, lhs, compareToOperator, rhs);
    }

    @Override
    public Predicate<AISMessage> visitMmsi(AisFilterParser.MmsiContext ctx)  {
        ToIntFunction<AISMessage> lhs = aisMessage -> aisMessage.getSourceMmsi().getMMSI().intValue();
        AisFilterParser.CompareToContext compareToOperator = ctx.compareTo();
        int mmsi = Integer.valueOf(ctx.INT().getText());
        return createCompareToInt(null, lhs, compareToOperator, mmsi);
    }

    @Override
    public Predicate<AISMessage> visitSogCog(AisFilterParser.SogCogContext ctx) {
        String fieldName = ctx.getChild(0).getText();
        AisFilterParser.CompareToContext compareToOperator = ctx.compareTo();

        if (ctx.FLOAT() != null) {
            ToDoubleFunction<AISMessage> lhs = aisMessage -> {
                if (aisMessage instanceof DynamicDataReport) {
                    tracker.update(aisMessage);
                    if (ctx.SOG() != null)
                        return ((DynamicDataReport) aisMessage).getSpeedOverGround();
                    else
                        return ((DynamicDataReport) aisMessage).getCourseOverGround();
                } else if (aisMessage instanceof StaticDataReport) {
                    tracker.update(aisMessage);
                    AISTrack aisTrack = tracker.getAisTrack(aisMessage.getSourceMmsi().getMMSI());
                    Float value ;
                    if (ctx.SOG() != null)
                        value = aisTrack.getSpeedOverGround();
                    else
                        value = aisTrack.getCourseOverGround();
                    return value == null ? 0.0f : value;     // Assume sog|cog=0.0 if not known
                } else {
                    LOG.warn("This is not relevant for SOG. Check program design. ctx = " + ctx.toString());
                    return 0.0; // Assume sog=0.0 message is not relevant for sog / should never happen
                }
            };

            double rhs = Double.valueOf(ctx.FLOAT().getText());
            return createCompareToDouble(fieldName, lhs, compareToOperator, rhs);
        } else {
            ToIntFunction<AISMessage> lhs = aisMessage -> {
                if (aisMessage instanceof DynamicDataReport) {
                    tracker.update(aisMessage);
                    if (ctx.SOG() != null)
                        return ((DynamicDataReport) aisMessage).getSpeedOverGround().intValue();
                    else
                        return ((DynamicDataReport) aisMessage).getCourseOverGround().intValue();
                } else if (aisMessage instanceof StaticDataReport) {
                    tracker.update(aisMessage);
                    AISTrack aisTrack = tracker.getAisTrack(aisMessage.getSourceMmsi().getMMSI());
                    int value ;
                    if (ctx.SOG() != null)
                        value = aisTrack.getSpeedOverGround() == null ? 0 : aisTrack.getSpeedOverGround().intValue();
                    else
                        value = aisTrack.getCourseOverGround() == null ? 0 : aisTrack.getCourseOverGround().intValue();
                    return value;     // Assume sog|cog=0.0 if not known
                } else {
                    LOG.warn("This is not relevant for SOG. Check program design. ctx = " + ctx.toString());
                    return 0; // Assume 0 message is not relevant for sog / should never happen
                }
            };

            int rhs = Integer.valueOf(ctx.INT().getText());
            return createCompareToInt(fieldName, lhs, compareToOperator, rhs);
        }
    }

    private Predicate<AISMessage> computePredicate(AisFilterParser.FilterExpressionContext ctx) {
        final String field = ctx.getChild(0).getText();

        return msg -> {
            if (isFieldRelevantForMessage(field, msg))
                return visit(ctx).test(msg);
            else
                return true;
        };
    }

    private boolean isFieldRelevantForMessage(String field, AISMessage msg) {
        if (field == null)
            return true;
        if ("sog".equalsIgnoreCase(field))
            return msg instanceof DynamicDataReport || msg instanceof StaticDataReport;
        if ("cog".equalsIgnoreCase(field))
            return msg instanceof DynamicDataReport || msg instanceof StaticDataReport;
        return true;
    }

    private Predicate<AISMessage> createCompareToInt(String fieldName, ToIntFunction<AISMessage> lhs, AisFilterParser.CompareToContext compareToOperator, int rhs) {
        if (compareToOperator.eq() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ? lhs.applyAsInt(aisMessage) == rhs : true;
        else if (compareToOperator.neq() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ? lhs.applyAsInt(aisMessage) != rhs : true;
        else if (compareToOperator.lt() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ? lhs.applyAsInt(aisMessage) < rhs : true;
        else if (compareToOperator.lte() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ? lhs.applyAsInt(aisMessage) <= rhs : true;
        else if (compareToOperator.gte() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ? lhs.applyAsInt(aisMessage) >= rhs : true;
        else if (compareToOperator.gt() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ? lhs.applyAsInt(aisMessage) > rhs : true;
        else
            throw new IllegalStateException("Unknown comparison operator: " + compareToOperator.getText());
    }

    private  Predicate<AISMessage> createCompareToDouble(String fieldName, ToDoubleFunction<AISMessage> lhs, AisFilterParser.CompareToContext compareToOperator, double rhs) {
        if (compareToOperator.eq() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ? Math.abs(lhs.applyAsDouble(aisMessage) - rhs) < 10e-6 : true;
        else if (compareToOperator.neq() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ?lhs.applyAsDouble(aisMessage) != rhs : true;
        else if (compareToOperator.lt() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ?lhs.applyAsDouble(aisMessage) < rhs : true;
        else if (compareToOperator.lte() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ?lhs.applyAsDouble(aisMessage) <= rhs : true;
        else if (compareToOperator.gte() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ?lhs.applyAsDouble(aisMessage) >= rhs : true;
        else if (compareToOperator.gt() != null)
            return aisMessage -> isFieldRelevantForMessage(fieldName, aisMessage) ?lhs.applyAsDouble(aisMessage) > rhs : true;
        else
            throw new IllegalStateException("Unknown comparison operator: " + compareToOperator.getText());
    }
}
