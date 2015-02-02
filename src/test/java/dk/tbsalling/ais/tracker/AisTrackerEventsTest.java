package dk.tbsalling.ais.tracker;

import com.google.common.eventbus.Subscribe;
import dk.tbsalling.ais.tracker.events.AisTrackCreatedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackDeletedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackDynamicsUpdatedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackUpdatedEvent;
import dk.tbsalling.ais.tracker.events.WallclockChangedEvent;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AisTrackerEventsTest {

    @Test
    public void testEvents() throws Exception {
        final AtomicInteger numWallclockEvents = new AtomicInteger();
        final AtomicInteger numCreateEvents = new AtomicInteger();
        final AtomicInteger numUpdateEvents = new AtomicInteger();
        final AtomicInteger numDynamicUpdateEvents = new AtomicInteger();
        final AtomicInteger numDeleteEvents = new AtomicInteger();

        AisTracker aisTracker = new AisTracker();
        aisTracker.setStalePeriod(Duration.ofMinutes(10));
        aisTracker.setStaleCheckPeriod(Duration.ofMinutes(1));
        aisTracker.setTaskExecutor(new CurrentThreadExecutor());
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResource("ais-sample-1.nmea").openStream();

        aisTracker.registerSubscriber(new Object() {
            @Subscribe
            public void wallclockChanged(WallclockChangedEvent event) {
                numWallclockEvents.incrementAndGet();
            }

            @Subscribe
            public void aisTrackCreated(AisTrackCreatedEvent event) {
                numCreateEvents.incrementAndGet();
                assertNotNull(event.getAisTrack());
                assertTrue(event.getMmsi() > 0);
                assertEquals(event.getMmsi(), event.getAisTrack().getMmsi());
            }

            @Subscribe
            public void aisTrackUpdated(AisTrackUpdatedEvent event) {
                numUpdateEvents.incrementAndGet();
                assertNotNull(event.getAisTrack());
                assertTrue(event.getMmsi() > 0);
                assertEquals(event.getMmsi(), event.getAisTrack().getMmsi());
            }

            @Subscribe
            public void aisTrackDynamicsUpdated(AisTrackDynamicsUpdatedEvent event) {
                numDynamicUpdateEvents.incrementAndGet();
                assertNotNull(event.getAisTrack());
                assertTrue(event.getMmsi() > 0);
                assertEquals(event.getMmsi(), event.getAisTrack().getMmsi());
            }

            @Subscribe
            public void aisTrackDeleted(AisTrackDeletedEvent event) {
                numDeleteEvents.incrementAndGet();
                assertNotNull(event.getAisTrack());
                assertTrue(event.getMmsi() > 0);
                assertEquals(event.getMmsi(), event.getAisTrack().getMmsi());
            }
        });

        final Instant wallclockStart = Instant.parse("2015-02-02T00:00:00.000Z");
        System.err.println("START wallclock: " + wallclockStart);

        final Instant[] wallclock = {wallclockStart};
        processAISInputStream(inputStream, msg -> {
            try {
                aisTracker.update(msg, wallclock[0]);
                wallclock[0] = wallclock[0].plusSeconds(5);
            } catch (IllegalArgumentException e) {
                System.err.println(msg.getSourceMmsi() + ": " + e.getMessage());
            }
        });
        aisTracker.shutdown();
        Instant wallclockEnd = aisTracker.getWallclock();

        System.err.println("END wallclock: " + wallclockEnd + " - ran data for " + ChronoUnit.MINUTES.between(wallclockStart, wallclockEnd) + " minutes.");

        assertTrue(aisTracker.isShutdown());
        assertEquals(1001, numWallclockEvents.get());
        assertEquals(933, numCreateEvents.get());
        assertEquals(7, numUpdateEvents.get());
        assertEquals(6, numDynamicUpdateEvents.get());
        assertEquals(812, numDeleteEvents.get());
        assertEquals(121, aisTracker.getNumberOfAisTracks());
    }

    private void processAISInputStream(InputStream inputStream, Consumer<AISMessage> doSomething) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));

        NMEAMessageHandler nmeaMessageHandler = new NMEAMessageHandler("TESTSRC1", new Consumer<AISMessage>() {
            @Override
            public void accept(AISMessage aisMessage) {
                doSomething.accept(aisMessage);
            }
        });

        int numLines = 0;
        String line;
        while((line = input.readLine()) != null) {
            try {
                nmeaMessageHandler.accept(NMEAMessage.fromString(line));
                numLines++;
            } catch(InvalidMessage e) {
                System.out.println(e.getMessage());
            }
        }

        assertEquals(1018, numLines);
    }

}
