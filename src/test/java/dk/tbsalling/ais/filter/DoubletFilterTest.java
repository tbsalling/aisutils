package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.time.Instant.now;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DoubletFilterTest {

    Predicate<AISMessage> filter;

    @Before
    public void setup() {
        filter = new DoubletFilter(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void duplicatesRejected() throws Exception {
        AISMessage aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertTrue(filter.test(aisMessage));

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertFalse(filter.test(aisMessage));

        Thread.sleep(10);

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertFalse(filter.test(aisMessage));

        Thread.sleep(200);

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertTrue(filter.test(aisMessage));

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertFalse(filter.test(aisMessage));
    }

    @Test
    public void nonDuplicatesNotRejected() throws Exception {
        AISMessage aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertTrue(filter.test(aisMessage));

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,13AkSB001dPi8NVPv9p@S0C<08GI,0*65"));
        assertTrue(filter.test(aisMessage));

        Thread.sleep(10);

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertFalse(filter.test(aisMessage));

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,13AkSB001dPi8NVPv9p@S0C<08GI,0*65"));
        assertFalse(filter.test(aisMessage));

        Thread.sleep(200);

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertTrue(filter.test(aisMessage));

        aisMessage = AISMessage.create(new Metadata("SRC", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,13AkSB001dPi8NVPv9p@S0C<08GI,0*65"));
        assertTrue(filter.test(aisMessage));
    }

    @Test
    public void dupesFromDifferentSourceRejected() throws Exception {
        AISMessage aisMessage = AISMessage.create(new Metadata("SRC1", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertTrue(filter.test(aisMessage));

        aisMessage = AISMessage.create(new Metadata("SRC2", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertFalse(filter.test(aisMessage));

        aisMessage = AISMessage.create(new Metadata("SRC3", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertFalse(filter.test(aisMessage));

        Thread.sleep(200);

        aisMessage = AISMessage.create(new Metadata("SRC3", now()), NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B"));
        assertTrue(filter.test(aisMessage));
    }

}