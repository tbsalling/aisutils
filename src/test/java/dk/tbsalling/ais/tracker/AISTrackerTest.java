package dk.tbsalling.ais.tracker;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class AISTrackerTest {

    AISTracker tracker;
    InputStream inputStream;
    Instant wallclock = Instant.parse("2015-01-30T12:06:51.611Z");

    @BeforeEach
    public void setup() throws Exception {
        tracker = new AISTracker();
        inputStream = Thread.currentThread().getContextClassLoader().getResource("ais-sample-1.nmea").openStream();
        processAISInputStream(inputStream, msg -> {
            try {
                tracker.update(msg, wallclock);
                wallclock = wallclock.plusSeconds(1);
            } catch (IllegalArgumentException e) {
                System.err.println(msg.getSourceMmsi() + ": " + e.getMessage());
            }
        });
        tracker.shutdown();
    }

    @Test
    public void testGetNumberOfAisTracks() {
        assertEquals(922, tracker.getNumberOfAisTracks());
    }

    @Test
    public void testIsVesselTracked() throws Exception {
        assertTrue(tracker.isTracked(211179670));
        assertTrue(tracker.isTracked(236037000));
        assertTrue(tracker.isTracked(244660180));
        assertTrue(tracker.isTracked(992111811));
        assertFalse(tracker.isTracked(219000000));
    }

    @Test
    public void testGetAisTrack() throws Exception {
        AISTrack track = tracker.getAisTrack(236037000);

        assertNotNull(track.getStaticDataReport());
        assertNotNull(track.getDynamicDataReport());

        assertEquals(236037000, track.getMmsi());

        assertEquals(Instant.parse("2015-01-30T12:16:35.611Z"), track.getTimeOfLastUpdate());
        assertEquals(Instant.parse("2015-01-30T12:07:27.611Z"), track.getTimeOfStaticUpdate());
        assertEquals(Instant.parse("2015-01-30T12:16:35.611Z"), track.getTimeOfDynamicUpdate());

        assertEquals(57.48651885986328, track.getLatitude(), 1e-5);
        assertEquals(11.340173721313477, track.getLongitude(), 1e-5);

        assertEquals("MAERSK VIGO", track.getShipName());
        assertEquals("ZDFC2", track.getCallsign());
    }

    @Test
    public void testGetAisTracks() throws Exception {
        Set<AISTrack> aisTracks = tracker.getAisTracks();
        assertEquals(922, aisTracks.size());

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
                nmeaMessageHandler.accept(new NMEAMessage(line));
            } catch(InvalidMessage e) {
                System.out.println(e.getMessage());
            }
        }
    }

}