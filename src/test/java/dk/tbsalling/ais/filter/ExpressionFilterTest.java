package dk.tbsalling.ais.filter;

import com.google.common.collect.Lists;
import dk.tbsalling.ais.tracker.AISTrack;
import dk.tbsalling.ais.tracker.AISTracker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpressionFilterTest {

    //
    // Test MSGID
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
    // Test MSGID list
    //

    @Test
    public void testMsgIdInList() throws Exception {
        verifyExpressionFilter("msgid in (1, 2, 3, 5)", msg -> Lists.newArrayList(1, 2, 3, 5).contains(msg.getMessageType().getCode()));
    }

    @Test
    public void testMsgIdNotInList() throws Exception {
        verifyExpressionFilter("msgid not in (1, 2, 3, 5)", msg -> ! Lists.newArrayList(1, 2, 3, 5).contains(msg.getMessageType().getCode()));
    }

    //
    // Test MMSI
    // Test and/or operator
    //

    @Test
    public void testMsgIdEqualsOrMmsiEquals() throws Exception {
        verifyExpressionFilter("msgid=1 or mmsi=227006760", msg -> msg.getMessageType().getCode() == 1 || msg.getSourceMmsi().getMmsi() == 227006760);
    }

    @Test
    public void testMsgIdEqualsAndMmsiEquals() throws Exception {
        verifyExpressionFilter("msgid=1 and mmsi=227006760", msg -> msg.getMessageType().getCode() == 1 && msg.getSourceMmsi().getMmsi() == 227006760);
    }

    //
    // Test MMSI list
    //

    @Test
    public void testMmsiIdInList() throws Exception {
        verifyExpressionFilter("mmsi in (227006760, 258009500, 257287000, 2734450)", msg -> Lists.newArrayList(227006760, 258009500, 257287000, 2734450).contains(msg.getSourceMmsi().getMmsi()));
    }

    @Test
    public void testMmsiIdNotInList() throws Exception {
        verifyExpressionFilter("mmsi not in (258009500, 257287000, 2734450)", msg -> !Lists.newArrayList(258009500, 257287000, 2734450).contains(msg.getSourceMmsi().getMmsi()));
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
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
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
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
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
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
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
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
                Float sog = track.getSpeedOverGround();
                if (sog == null)
                    sog = 0.0f;
                return Math.abs(sog - 10.1f) < 1e-6;
            } else
                return true;
        });
    }

    //
    // Test COG
    //

    @Test
    public void testCogGreaterThan() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("cog>270.0", msg -> { // triggers visitCog
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return ((DynamicDataReport) msg).getCourseOverGround() > 270.0;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
                Float cog = track.getCourseOverGround();
                if (cog == null)
                    cog = 0.0f;
                return cog > 270.0;
            } else
                return true;
        });
    }

    @Test
    public void testCogGreaterThanOrLessThan() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("cog>330.0 or cog<30.0", msg -> { // triggers visitAndOr
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return ((DynamicDataReport) msg).getCourseOverGround() > 330 || ((DynamicDataReport) msg).getCourseOverGround() < 30;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
                Float cog = track.getCourseOverGround();
                if (cog == null)
                    cog = 0.0f;
                return cog > 330 || cog < 30;
            } else
                return true;
        });
    }

    //
    // Test LAT / LNG
    //

    @Test
    public void testLatGreaterThanAndLessThan() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("lat>55.0 and lat<55.5", msg -> { // triggers visitAndOr
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return ((DynamicDataReport) msg).getLatitude() > 55.0 && ((DynamicDataReport) msg).getLatitude() < 55.5;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
                Float lat = track.getLatitude();
                if (lat == null)
                    lat = 0.0f;
                return lat > 55.0 && lat < 55.5;
            } else
                return true;
        });
    }

    @Test
    public void testLngGreaterThanAndLessThan() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("lng>10.0 and lng<11.0", msg -> { // triggers visitAndOr
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return ((DynamicDataReport) msg).getLongitude() > 10.0 && ((DynamicDataReport) msg).getLongitude() < 11.0;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
                Float lng = track.getLongitude();
                if (lng == null)
                    lng = 0.0f;
                return lng > 10.0 && lng < 11.0;
            } else
                return true;
        });
    }

    @Test
    public void testLatAndLngGreaterThanAndLessThan() throws Exception {
        final AISTracker tracker = new AISTracker();

        verifyExpressionFilter("lat>58.8 and lat<59.0 and lng>5.0 and lng<6.0", msg -> { // triggers visitAndOr
            if (msg instanceof DynamicDataReport) {
                tracker.update(msg);
                return  ((DynamicDataReport) msg).getLatitude() > 58.8 &&
                        ((DynamicDataReport) msg).getLatitude() < 59.0 &&
                        ((DynamicDataReport) msg).getLongitude() > 5.0 &&
                        ((DynamicDataReport) msg).getLongitude() < 6.0;
            } else if (msg instanceof StaticDataReport) {
                tracker.update(msg);
                AISTrack track = tracker.getAisTrack(msg.getSourceMmsi().getMmsi());
                Float lat = track.getLatitude();
                Float lng = track.getLongitude();
                if (lat == null)
                    lat = 0.0f;
                if (lng == null)
                    lng = 0.0f;
                return lat > 58.8 && lat < 59.0 && lng > 5.0 && lng < 6.0;
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
                boolean testValue = expressionFilter.test(msg);
                boolean verificationValue = verification.test(msg);
                System.out.println("Test value: " + testValue + " expected: " + verificationValue + " " + msg);
                if (verificationValue) {
                    assertTrue(testValue);
                    weSawTrueResults[0] = true;
                } else {
                    assertFalse(testValue);
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
                nmeaMessageHandler.accept(new NMEAMessage(line));
            } catch(InvalidMessage e) {
                System.out.println(e.getMessage());
            }
        }
    }

}