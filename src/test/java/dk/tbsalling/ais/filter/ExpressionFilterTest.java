package dk.tbsalling.ais.filter;

import dk.tbsalling.ais.tracker.AISTrack;
import dk.tbsalling.ais.tracker.AISTracker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpressionFilterTest {

    //
    // Test eq, neq, lt, lte, gt, gte
    //

    @Test
    public void testMsgIdEquals() throws Exception {
        verifyExpressionFilter("msgid=3", msg -> msg.getMessageType().getCode() == 3);
    }

    @Test
    public void testMsgIdNotEquals() throws Exception {
        verifyExpressionFilter("msgid!=3", msg -> msg.getMessageType().getCode() != 3);
    }

    @Test
    public void testMsgIdLessThan() throws Exception {
        verifyExpressionFilter("msgid<3", msg -> msg.getMessageType().getCode() < 3);
    }

    @Test
    public void testMsgIdLessThanEquals() throws Exception {
        verifyExpressionFilter("msgid<=3", msg -> msg.getMessageType().getCode() <= 3);
    }

    @Test
    public void testMsgIdGreaterThan() throws Exception {
        verifyExpressionFilter("msgid>3", msg -> msg.getMessageType().getCode() > 3);
    }

    @Test
    public void testMsgIdGreaterThanEquals() throws Exception {
        verifyExpressionFilter("msgid>=3", msg -> msg.getMessageType().getCode() >= 3);
    }

    //
    // Test and/or operator
    //

    @Test
    public void testMsgIdEqualsOrMmsiEquals() throws Exception {
        verifyExpressionFilter("msgid=1 or mmsi=227006760", msg -> msg.getMessageType().getCode() == 1 || msg.getSourceMmsi().getMMSI() == 227006760);
    }

    @Test
    public void testMsgIdEqualsAndMmsiEquals() throws Exception {
        verifyExpressionFilter("msgid=1 and mmsi=227006760", msg -> msg.getMessageType().getCode() == 1 && msg.getSourceMmsi().getMMSI() == 227006760);
    }

    //
    // Test SOG
    //

    @Test
    public void testSogGreaterThan() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("sog>5.9", msg -> { // triggers visitSog
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return ((DynamicDataReport) msg).getSpeedOverGround() > 5.9;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMMSI());
                Float sog = track.getSpeedOverGround();
                if (sog == null)
                    sog = 0.0f;
                return sog > 5.9;
            } else
                return true;
        });
    }

    @Test
    public void testSogGreaterThan2() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("sog>5.9 or mmsi=1", msg -> { // triggers visitAndOr
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return ((DynamicDataReport) msg).getSpeedOverGround() > 5.9;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMMSI());
                Float sog = track.getSpeedOverGround();
                if (sog == null)
                    sog = 0.0f;
                return sog > 5.9;
            } else
                return true;
        });
    }

    @Test
    public void testSogGreaterThanOrEquals() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("sog>=6.0", msg -> { // triggers visitSog
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return ((DynamicDataReport) msg).getSpeedOverGround() >= 6.0;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMMSI());
                Float sog = track.getSpeedOverGround();
                if (sog == null)
                    sog = 0.0f;
                return sog > 6.0;
            } else
                return true;
        });
    }

    @Test
    public void testSogEquals() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("sog=10.1", msg -> { // triggers visitSog
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return Math.abs(((DynamicDataReport) msg).getSpeedOverGround() - 10.1f) < 1e-6;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMMSI());
                Float sog = track.getSpeedOverGround();
                if (sog == null)
                    sog = 0.0f;
                return Math.abs(sog - 10.1f) < 1e-6;
            } else
                return true;
        });
    }

    //
    // Internal methods
    //

    private static void verifyExpressionFilter(String filterExpression, Predicate<AISMessage> verification) throws Exception {
        final Predicate<AISMessage> expressionFilter = FilterFactory.newExpressionFilter(filterExpression);
        final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResource("ais-sample-1.nmea").openStream();

        final boolean[] weSawTrueResults = {false};
        final boolean[] weSawFalseResults = {false};

        processAISInputStream(inputStream, msg -> {
            try {
                System.err.println("Testing " + msg);
                boolean test = expressionFilter.test(msg);
                if (verification.test(msg)) {
                    assertTrue(test);
                    weSawTrueResults[0] = true;
                } else {
                    assertFalse(test);
                    weSawFalseResults[0] = true;
                }
            } catch (IllegalArgumentException e) {
                System.err.println(msg.getSourceMmsi() + ": " + e.getMessage());
            }
        });

        assertTrue(weSawTrueResults[0]);
        assertTrue(weSawFalseResults[0]);
    }

    private static void processAISInputStream(InputStream inputStream, Consumer<AISMessage> doSomething) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));

        NMEAMessageHandler nmeaMessageHandler = new NMEAMessageHandler("TESTSRC1", new Consumer<AISMessage>() {
            @Override
            public void accept(AISMessage aisMessage) {
                doSomething.accept(aisMessage);
            }
        });

        String line;
        while((line = input.readLine()) != null) {
            try {
                nmeaMessageHandler.accept(NMEAMessage.fromString(line));
            } catch(InvalidMessage e) {
                System.out.println(e.getMessage());
            }
        }
    }

}