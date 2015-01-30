package dk.tbsalling.ais.tracker;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AisTrackerTest {

    AisTracker tracker;
    InputStream inputStream;
    Instant wallclock = Instant.parse("2015-01-30T12:06:51.611Z");

    @Before
    public void setup() throws Exception {
        tracker = new AisTracker();
        inputStream = Thread.currentThread().getContextClassLoader().getResource("ais-sample-1.nmea").openStream();
        processAISInputStream(inputStream, msg -> {
            try {
                tracker.update(msg, wallclock);
                wallclock = wallclock.plusSeconds(1);
            } catch (IllegalArgumentException e) {
                System.err.println(msg.getSourceMmsi() + ": " + e.getMessage());
            }
        });
    }

    @Test
    public void testGetNumberOfAisTracks() {
        assertEquals(917, tracker.getNumberOfAisTracks());
    }

    @Test
    public void testIsVesselTracked() throws Exception {
        assertTrue(tracker.isVesselTracked(211179670));
        assertTrue(tracker.isVesselTracked(236037000));
        assertTrue(tracker.isVesselTracked(244660180));
        assertFalse(tracker.isVesselTracked(219000000));
    }

    @Test
    public void testGetAisTrack() throws Exception {
        AisTrack track = tracker.getAisTrack(236037000);

        assertNotNull(track.getStaticDataReport());
        assertNotNull(track.getDynamicDataReport());

        assertEquals(236037000, track.getMmsi());

        assertEquals(Instant.parse("2015-01-30T12:16:36.611Z"), track.getTimeOfLastUpdate());
        assertEquals(Instant.parse("2015-01-30T12:07:27.611Z"), track.getTimeOfStaticUpdate());
        assertEquals(Instant.parse("2015-01-30T12:16:36.611Z"), track.getTimeOfDynamicUpdate());

        assertEquals(57.48651885986328, track.getLatitude(), 1e-5);
        assertEquals(11.340173721313477, track.getLongitude(), 1e-5);

        assertEquals("MAERSK VIGO", track.getShipName());
        assertEquals("ZDFC2", track.getCallsign());
    }

    @Test
    public void testGetAisTracks() throws Exception {
        Set<AisTrack> aisTracks = tracker.getAisTracks();
        assertEquals(917, aisTracks.size());

        final boolean[] found = {false};
        aisTracks.forEach(track -> { if (track.getMmsi() == 236037000) found[0] = true; });
        assertTrue(found[0]);
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