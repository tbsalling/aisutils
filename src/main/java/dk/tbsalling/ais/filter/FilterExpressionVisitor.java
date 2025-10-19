package dk.tbsalling.ais.filter;

import com.google.common.collect.Lists;
import dk.tbsalling.ais.tracker.AISTrack;
import dk.tbsalling.ais.tracker.AISTracker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * This class is mainly an internal helper class implementing the ExpressionFilter
 * functionality by the help of ANTLRv4.
 *
 * @author Thomas Borg Salling
 */
class FilterExpressionVisitor extends AisFilterBaseVisitor<Predicate<AISMessage>> {

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
    public Predicate<AISMessage> visitMsgidInList(AisFilterParser.MsgidInListContext ctx) {
        ArrayList<Integer> msgIDList = Lists.newArrayList();
        List<TerminalNode> msgIds = ctx.intList().INT();
        msgIds.forEach(msgId -> msgIDList.add(Integer.valueOf(msgId.getText())));

        return ctx.in() != null ? msg -> msgIDList.contains(msg.getMessageType().getCode()) : msg -> !msgIDList.contains(msg.getMessageType().getCode());
    }

    @Override
    public Predicate<AISMessage> visitMmsi(AisFilterParser.MmsiContext ctx)  {
        ToIntFunction<AISMessage> lhs = aisMessage -> aisMessage.getSourceMmsi().getMmsi();
        AisFilterParser.CompareToContext compareToOperator = ctx.compareTo();
        int mmsi = Integer.valueOf(ctx.INT().getText());
        return createCompareToInt(null, lhs, compareToOperator, mmsi);
    }

    @Override
    public Predicate<AISMessage> visitMmsiInList(AisFilterParser.MmsiInListContext ctx) {
        ArrayList<Integer> mmsiList = Lists.newArrayList();
        List<TerminalNode> mmsis = ctx.intList().INT();
        mmsis.forEach(mmsi -> mmsiList.add(Integer.valueOf(mmsi.getText())));

        return ctx.in() != null ? msg -> mmsiList.contains(msg.getSourceMmsi().getMmsi()) : msg -> !mmsiList.contains(msg.getSourceMmsi().getMmsi());
    }

    @Override
    public Predicate<AISMessage> visitSogCog(AisFilterParser.SogCogContext ctx) {
        String fieldName = ctx.getChild(0).getText();
        AisFilterParser.CompareToContext compareToOperator = ctx.compareTo();

        if (ctx.FLOAT() != null) {
            ToDoubleFunction<AISMessage> lhs = extractDoubleFromAisMessageOrAisTrack(
                aisMessage -> {
                    if (ctx.SOG() != null)
                        return Double.valueOf(((DynamicDataReport) aisMessage).getSpeedOverGround());
                    else
                        return Double.valueOf(((DynamicDataReport) aisMessage).getCourseOverGround());
                },
                aisTrack -> {
                    Float value;
                    if (ctx.SOG() != null)
                        value = aisTrack.getSpeedOverGround();
                    else
                        value = aisTrack.getCourseOverGround();
                    return value == null ? 0.0d : Double.valueOf(value); // Assume 0.0 if not known
                }
            );

            double rhs = Double.valueOf(ctx.FLOAT().getText());
            return createCompareToDouble(fieldName, lhs, compareToOperator, rhs);
        } else {
            ToIntFunction<AISMessage> lhs = extractIntFromAisMessageOrAisTrack(
                aisMessage -> {
                    if (ctx.SOG() != null)
                        return (int) ((DynamicDataReport) aisMessage).getSpeedOverGround();
                    else
                        return (int) ((DynamicDataReport) aisMessage).getCourseOverGround();
                },
                aisTrack -> {
                    if (ctx.SOG() != null)
                        return aisTrack.getSpeedOverGround() == null ? 0 : aisTrack.getSpeedOverGround().intValue();
                    else
                        return aisTrack.getCourseOverGround() == null ? 0 : aisTrack.getCourseOverGround().intValue();
                }
            );

            int rhs = Integer.valueOf(ctx.INT().getText());
            return createCompareToInt(fieldName, lhs, compareToOperator, rhs);
        }
    }

    @Override
    public Predicate<AISMessage> visitLatLng(AisFilterParser.LatLngContext ctx) {
        String fieldName = ctx.getChild(0).getText();

        ToDoubleFunction<AISMessage> lhs = extractDoubleFromAisMessageOrAisTrack(
            aisMessage -> {
                if (ctx.LAT() != null)
                    return Double.valueOf(((DynamicDataReport) aisMessage).getLatitude());
                else
                    return Double.valueOf(((DynamicDataReport) aisMessage).getLongitude());
            },
            aisTrack -> {
                Float value;
                if (ctx.LAT() != null)
                    value = aisTrack.getLatitude();
                else
                    value = aisTrack.getLongitude();
                return value == null ? 0.0d : Double.valueOf(value); // Assume 0.0 if not known
            }
        );

        AisFilterParser.CompareToContext compareToOperator = ctx.compareTo();

        double rhs = Double.valueOf(ctx.FLOAT().getText());

        return createCompareToDouble(fieldName, lhs, compareToOperator, rhs);
    }

    private ToIntFunction<AISMessage> extractIntFromAisMessageOrAisTrack(Function<AISMessage, Integer> extractIntFromAisMessage, Function<AISTrack, Integer> extractIntFromAisTrack) {
        return aisMessage -> {
            if (aisMessage instanceof DynamicDataReport) {
                tracker.update(aisMessage);
                return extractIntFromAisMessage.apply(aisMessage);
            } else if (aisMessage instanceof StaticDataReport) {
                tracker.update(aisMessage);
                AISTrack aisTrack = tracker.getAisTrack(aisMessage.getSourceMmsi().getMmsi());
                return extractIntFromAisTrack.apply(aisTrack);
            } else {
                return 0; // Assume 0 message is not relevant / should never happen
            }
        };
    }

    private ToDoubleFunction<AISMessage> extractDoubleFromAisMessageOrAisTrack(Function<AISMessage, Double> extractDoubleFromAisMessage, Function<AISTrack, Double> extractDoubleFromAisTrack) {
        return aisMessage -> {
            if (aisMessage instanceof DynamicDataReport) {
                tracker.update(aisMessage);
                return extractDoubleFromAisMessage.apply(aisMessage);
            } else if (aisMessage instanceof StaticDataReport) {
                tracker.update(aisMessage);
                AISTrack aisTrack = tracker.getAisTrack(aisMessage.getSourceMmsi().getMmsi());
                return extractDoubleFromAisTrack.apply(aisTrack);
            } else {
                return 0.0; // Assume sog=0.0 message is not relevant for sog / should never happen
            }
        };
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
        if ("lat".equalsIgnoreCase(field))
            return msg instanceof DynamicDataReport || msg instanceof StaticDataReport;
        if ("lng".equalsIgnoreCase(field))
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
