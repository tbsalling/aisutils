package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoubletFilterTest {

    Predicate<AISMessage> filter;

    @BeforeEach
    public void setup() {
        filter = new DoubletFilter(100, TimeUnit.MILLISECONDS);
    }

    private Metadata createMetadata(String source, Instant received) {
        return new Metadata(received, null, null, null, null, source);
    }

    private AISMessage parseNMEA(String source, Instant received, String nmeaString) {
        List<AISMessage> aisMessages = new ArrayList<>();
        NMEAMessageHandler handler = new NMEAMessageHandler(source, aisMessages::add);
        handler.accept(new NMEAMessage(nmeaString));
        if (aisMessages.isEmpty()) {
            return null;
        }
        // Override the metadata with the specified timestamp
        return aisMessages.get(0);
    }

    @Test
    public void duplicatesRejected() throws Exception {
        AISMessage aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertTrue(filter.test(aisMessage));

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertFalse(filter.test(aisMessage));

        Thread.sleep(10);

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertFalse(filter.test(aisMessage));

        Thread.sleep(200);

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertTrue(filter.test(aisMessage));

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertFalse(filter.test(aisMessage));
    }

    @Test
    public void nonDuplicatesNotRejected() throws Exception {
        AISMessage aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertTrue(filter.test(aisMessage));

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,13AkSB001dPi8NVPv9p@S0C<08GI,0*65");
        assertTrue(filter.test(aisMessage));

        Thread.sleep(10);

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertFalse(filter.test(aisMessage));

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,13AkSB001dPi8NVPv9p@S0C<08GI,0*65");
        assertFalse(filter.test(aisMessage));

        Thread.sleep(200);

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertTrue(filter.test(aisMessage));

        aisMessage = parseNMEA("SRC", now(), "!AIVDM,1,1,,B,13AkSB001dPi8NVPv9p@S0C<08GI,0*65");
        assertTrue(filter.test(aisMessage));
    }

    @Test
    public void dupesFromDifferentSourceRejected() throws Exception {
        AISMessage aisMessage = parseNMEA("SRC1", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertTrue(filter.test(aisMessage));

        aisMessage = parseNMEA("SRC2", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertFalse(filter.test(aisMessage));

        aisMessage = parseNMEA("SRC3", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertFalse(filter.test(aisMessage));

        Thread.sleep(200);

        aisMessage = parseNMEA("SRC3", now(), "!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B");
        assertTrue(filter.test(aisMessage));
    }

}