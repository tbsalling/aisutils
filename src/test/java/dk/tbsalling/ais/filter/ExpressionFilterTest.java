package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpressionFilterTest {

    @Test
    public void testMsgIdOrMmsi() throws Exception {
        Predicate<AISMessage> expressionFilter = FilterFactory.newExpressionFilter("msgid=1 or mmsi=227006760");
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResource("ais-sample-1.nmea").openStream();

        final boolean[] weSawTrueResults = {false};
        final boolean[] weSawFalseResults = {false};

        processAISInputStream(inputStream, msg -> {
            try {
                boolean test = expressionFilter.test(msg);
                if (msg.getMessageType().getCode() == 1 || msg.getSourceMmsi().getMMSI() == 227006760) {
                    assertTrue(test);
                    weSawTrueResults[0] = true;
                }
                else {
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

    @Test
    public void testMsgIdAndMmsi() throws Exception {
        Predicate<AISMessage> expressionFilter = FilterFactory.newExpressionFilter("msgid=1 and mmsi=227006760");
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResource("ais-sample-1.nmea").openStream();

        final boolean[] weSawTrueResults = {false};
        final boolean[] weSawFalseResults = {false};

        processAISInputStream(inputStream, msg -> {
            try {
                boolean test = expressionFilter.test(msg);
                if (msg.getMessageType().getCode() == 1 && msg.getSourceMmsi().getMMSI() == 227006760) {
                    assertTrue(test);
                    weSawTrueResults[0] = true;
                }
                else {
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