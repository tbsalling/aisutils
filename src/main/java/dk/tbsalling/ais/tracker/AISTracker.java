/*
 * AISUtils
 * - a java-based library for processing of AIS messages received from digital
 * VHF radio traffic related to maritime navigation and safety in compliance with ITU 1371.
 *
 * (C) Copyright 2011- by S-Consult ApS, DK31327490, http://s-consult.dk, Denmark.
 *
 * Released under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * For details of this license see the nearby LICENCE-full file, visit http://creativecommons.org/licenses/by-nc-sa/3.0/
 * or send a letter to Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 *
 * NOT FOR COMMERCIAL USE!
 * Contact sales@s-consult.dk to obtain a commercially licensed version of this software.
 *
 */

package dk.tbsalling.ais.tracker;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import dk.tbsalling.ais.tracker.events.*;
import dk.tbsalling.aismessages.AISInputStreamReader;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.time.Instant.EPOCH;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;

/**
 * An AisTracker receives AISMessages and based on these it maintains a collection of all known tracks,
 * including their position, speed, course, etc.
 *
 * If a certain track has not received any updates for a while it enters status 'stale' and will receive
 * no further updates. Instead a new track is created if more AISMessages are received from the same vessel
 * later on.
 */
@ThreadSafe
public class AISTracker implements TrackEventEmitter {

    private final static Logger LOG = LoggerFactory.getLogger(AISTracker.class);

    private final Predicate<AISMessage> messageFilter;

    /**
     * Construct an AISTracker which processes all received AISMessages.
     */
    public AISTracker() {
        LOG.info("AisTracker created.");
        messageFilter = msg -> true;
        shutdown = false;
    }

    /**
     * Construct on AISTracker which processes only messages satisfying the messageFilter.
     * @param messageFilter
     */
    public AISTracker(Predicate<AISMessage> messageFilter) {
        LOG.info("AisTracker created with custom predicate.");
        this.messageFilter = messageFilter;
        shutdown = false;
    }

    /**
     * Update the tracker from an input stream of NMEA armoured AIS messages.
     *
     * If an IOException is thrown, the state of the tracker is maintained, and tracking
     * is resumed when this method is called again with a new InputStream.
     *
     * @param nmeaInputStream
     */
    public void update(InputStream nmeaInputStream) throws IOException {
        new AISInputStreamReader(nmeaInputStream, aisMessage -> update(aisMessage)).run();
    }

    /**
     * Update the tracker with a new AIS message.
     *
     * If there is a reception timestamp in the meta data of the AIS message, then it will be used as the
     * message timestamp. If not, current system time will be used as the timestamp.
     *
     * @param aisMessage the AIS message.
     */
    public void update(AISMessage aisMessage) {
        if (threadSafeGet(() -> shutdown))
            throw new IllegalStateException("Tracker has been requested to shutdown.");

        requireNonNull(aisMessage);

        Metadata metadata = aisMessage.getMetadata();
        Instant messageTimestamp = metadata == null ? now(Clock.systemUTC()) : metadata.getReceived();

        if (messageFilter.test(aisMessage))
            updateAisTrack(aisMessage, messageTimestamp);
    }

    /**
     * Update the tracker with a new AIS message.
     *
     * @param aisMessage the AIS message.
     * @param messageTimestamp the time this AIS message was received.
     */
    public void update(AISMessage aisMessage, Instant messageTimestamp) {
        if (threadSafeGet(() -> shutdown))
            throw new IllegalStateException("Tracker has been requested to shutdown.");

        requireNonNull(aisMessage);
        requireNonNull(messageTimestamp);

        if (messageFilter.test(aisMessage))
            updateAisTrack(aisMessage, messageTimestamp);
    }

    /**
     * Check if a given vessel is currently tracked by the tracker.
     * @param mmsi The MMSI no.
     * @return true if the vessel is currently tracked, false if not.
     */
    public boolean isTracked(long mmsi) {
        return threadSafeGet(() -> tracks.containsKey(mmsi));
    }

    /**
     * Count the no. of tracks currently being tracked.
     * @return the no of tracks.
     */
    public int getNumberOfAisTracks() {
        return threadSafeGet(() -> tracks.size());
    }

    /**
     * Lookup a tracked AisTrack with the given mmsi no.
     * @param mmsi the mmsi no. to lookup.
     * @return The tracked AisTrack or null if no such track is currently tracked.
     */
    public AISTrack getAisTrack(long mmsi) {
        return threadSafeGet(() -> tracks.get(mmsi));
    }

    /**
     * Extract an immutable copy of all tracks currently tracked.
     * @return An immutable set of all tracks currently tracked.
     */
    public Set<AISTrack> getAisTracks() {
        return threadSafeGet(() -> ImmutableSet.copyOf(tracks.values()));
    }

    /** Return the value of the current wallclock. */
    public Instant getWallclock() {
        return threadSafeGet(() -> wallclock);
    }

    /** Return the instant when track history pruning was last performed. */
    public Instant getTimeOfLastPruning() {
        return threadSafeGet(() -> timeOfLastPruning);
    }

    public boolean isShutdown() {
        return threadSafeGet(() -> shutdown);
    }

    /** Shut down the tracker */
    public void shutdown() {
        LOG.info("AisTracker shutdown requested.");
        lock.lock();
        try {
            shutdown = true;
        } finally {
            lock.unlock();
        }
        try {
            taskExecutor.shutdown();
            boolean cleanShutdown = taskExecutor.awaitTermination(1, TimeUnit.MINUTES);
            LOG.debug("taskExecutor: shutdown:" + taskExecutor.isShutdown() + " terminated:" + taskExecutor.isTerminated());
            if (cleanShutdown == false)
                LOG.warn("AisTracker was shut down before all pending tasks were processed");
        } catch (InterruptedException e) {
            LOG.error("Tracker failed to shutdown cleanly", e);
        }
        try {
            eventBusExecutor.shutdown();
            boolean cleanShutdown = eventBusExecutor.awaitTermination(1, TimeUnit.MINUTES);
            LOG.debug("eventBusExecutor: shutdown:" + eventBusExecutor.isShutdown() + " terminated:" + eventBusExecutor.isTerminated());
            if (cleanShutdown == false)
                LOG.warn("AisTracker was shut down before all pending events were processed");
        } catch (InterruptedException e) {
            LOG.error("Tracker failed to shutdown cleanly", e);
        }
        LOG.info("AisTracker shutdown completed.");
    }

    private <T> T threadSafeGet(Supplier<T> getter) {
        lock.lock();
        try {
            return getter.get();
        } finally {
            lock.unlock();
        }
    }

    private void updateAisTrack(final AISMessage aisMessage, final Instant messageTimestamp) {
        final long mmsi = aisMessage.getSourceMmsi().getMMSI();

        lock.lock();
        try {
            if (messageTimestamp.isBefore(wallclock))
                throw new IllegalArgumentException("Current time is " + wallclock + "; message timestamp is too old: " + messageTimestamp);

            setWallclock(messageTimestamp);

            if (aisMessage instanceof StaticDataReport) {
                if (isTracked(mmsi)) {
                    updateAisTrack(mmsi, (StaticDataReport) aisMessage, messageTimestamp);
                } else {
                    insertAisTrack(mmsi, (StaticDataReport) aisMessage, messageTimestamp);
                }
            } else if (aisMessage instanceof DynamicDataReport) {
                if (isTracked(mmsi)) {
                    updateAisTrack(mmsi, (DynamicDataReport) aisMessage, messageTimestamp);
                } else {
                    insertAisTrack(mmsi, (DynamicDataReport) aisMessage, messageTimestamp);
                }
            }
            if (isHistoryPruneNeeded()) {
                taskExecutor.execute(() -> processTrackHistory());
            }
            if (isStaleCheckNeeded()) {
                taskExecutor.execute(() -> processStaleTracks());
            }
        } finally {
            lock.unlock();
        }
    }

    private void insertAisTrack(final long mmsi, final StaticDataReport shipStaticDataReport, final Instant msgTimestamp) {
        /* Assumes lock is locked */
        final AISTrack aisTrack = new AISTrack(shipStaticDataReport, msgTimestamp);
        tracks.put(mmsi, aisTrack);
        fireTrackCreated(aisTrack);
    }

    private void insertAisTrack(final long mmsi, final DynamicDataReport basicShipDynamicDataReport, final Instant msgTimestamp) {
        /* Assumes lock is locked */
        final AISTrack aisTrack = new AISTrack(basicShipDynamicDataReport, msgTimestamp);
        tracks.put(mmsi, aisTrack);
        fireTrackCreated(aisTrack);
    }

    private void updateAisTrack(final long mmsi, final StaticDataReport shipStaticDataReport, final Instant msgTimestamp) {
        /* Assumes lock is locked */
        AISTrack oldTrack = tracks.get(mmsi);
        if (msgTimestamp.isBefore(oldTrack.getTimeOfLastUpdate()))
            throw new IllegalArgumentException("Cannot update track with an older message: " + msgTimestamp + " is before previous update " + oldTrack.getTimeOfStaticUpdate());

        AISTrack newTrack = new AISTrack(shipStaticDataReport, oldTrack.getDynamicDataReport(), msgTimestamp, oldTrack.getTimeOfDynamicUpdate());
        tracks.put(mmsi, newTrack);
        fireTrackUpdated(newTrack);
    }

    private void updateAisTrack(final long mmsi, final DynamicDataReport basicShipDynamicDataReport, final Instant msgTimestamp) {
        /* Assumes lock is locked */
        AISTrack oldTrack = tracks.get(mmsi);
        if (msgTimestamp.isBefore(oldTrack.getTimeOfLastUpdate()))
            throw new IllegalArgumentException("Cannot update track with an older message: " + msgTimestamp + " is before previous update " + oldTrack.getTimeOfDynamicUpdate());

        AISTrack newTrack = new AISTrack(oldTrack.getStaticDataReport(), basicShipDynamicDataReport, oldTrack.getTimeOfStaticUpdate(), msgTimestamp);
        tracks.put(mmsi, newTrack);
        fireTrackUpdated(newTrack);
        fireTrackDynamicsUpdated(newTrack);
    }

    //
    // Core data fields of the tracker
    //

    private ReentrantLock lock = new ReentrantLock();

    /** */
    @GuardedBy("lock")
    private boolean shutdown = false;

    @GuardedBy("lock")
    private Map<Long, AISTrack> tracks = new HashMap<>();

    /** To inject special executors for unit testing */
    void setTaskExecutor(ExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /** Asynchroneous executor service to take care of pruning */
    private ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

    //
    // Fields and methods related to the wallclock
    //

    /** Time of last update - perceived by the tracker as current time; or time as seen on the wallclock. */
    @GuardedBy("lock")
    private Instant wallclock = EPOCH;

    private void setWallclock(Instant wallclock) {
        lock.lock();
        try {
            this.wallclock = wallclock;
            fireWallclockChanged(this.wallclock);
        } finally {
            lock.unlock();
        }
    }

    //
    // Fields and methods related to pruning
    //

    /** Run through all tracks and prune historic items which have expired */
    private void processTrackHistory() {
        lock.lock();
        try {
            Map<Long, AISTrack> prunedTracks = Maps.newTreeMap();
            tracks.forEach((mmsi, track) -> {
                if (TRACK_NEEDS_PRUNING.test(track)) {
                    prunedTracks.put(track.getMmsi(), new AISTrack(track, INSTANT_IMPLIES_PRUNING));
                }
            });
            prunedTracks.forEach((mmsi, track) -> tracks.put(mmsi, prunedTracks.get(mmsi)));
            timeOfLastPruning = wallclock;
        } finally {
            lock.unlock();
        }
    }

    private boolean isHistoryPruneNeeded() {
        /* Assumes lock is locked */
        return timeOfLastPruning.isBefore(wallclock.minus(PRUNE_CHECK_PERIOD));
    }

    /** Time on the wall clock between track history pruning jobs */
    private final static Duration PRUNE_CHECK_PERIOD = Duration.ofMinutes(5);

    /** The instant in time when the last pruning job ran */
    @GuardedBy("lock")
    private Instant timeOfLastPruning = EPOCH;

    /** Max duration to keep dynamic history of each track */
    private final static Duration DYNAMIC_DATA_HISTORY_MAX_AGE = Duration.ofHours(6);

    /** Predicate for instants which imply that pruning is required */
    private final Predicate<Instant> INSTANT_IMPLIES_PRUNING  = instant -> instant.isBefore(wallclock.minus(DYNAMIC_DATA_HISTORY_MAX_AGE));

    /** Predicate for tracks which need pruning of their dynamic history */
    private final Predicate<AISTrack> TRACK_NEEDS_PRUNING = aisTrack -> !aisTrack.getDynamicDataHistory().isEmpty() && INSTANT_IMPLIES_PRUNING.test(aisTrack.getDynamicDataHistory().firstKey());

    //
    // Fields and methods related to track stale check
    //

    /** Run through all tracks and note which ones are stale */
    private void processStaleTracks() {
        lock.lock();
        try {
            Map<Long, AISTrack> staleTracks = Maps.newTreeMap();
            tracks.forEach((mmsi, track) -> {
                if (track.getTimeOfLastUpdate().isBefore(wallclock.minus(STALE_PERIOD))) {
                    staleTracks.put(mmsi, track);
                }
            });
            staleTracks.forEach((mmsi, track) -> { tracks.remove(mmsi); fireTrackDeleted(track); });
            timeOfLastStaleCheck = wallclock;
        } finally {
            lock.unlock();
        }
    }

    private boolean isStaleCheckNeeded() {
        lock.lock();
        try {
            return timeOfLastStaleCheck.isBefore(wallclock.minus(STALE_CHECK_PERIOD));
        } finally {
            lock.unlock();
        }
    }

    void setStaleCheckPeriod(Duration staleCheckPeriod) {
        lock.lock();
        try {
            STALE_CHECK_PERIOD = staleCheckPeriod;
        } finally {
            lock.unlock();
        }
    }

    void setStalePeriod(Duration stalePeriod) {
        lock.lock();
        try {
            STALE_PERIOD = stalePeriod;
        } finally {
            lock.unlock();
        }
    }

    /** Every this duration on the wallclock tracks are checked to be stale */
    private Duration STALE_CHECK_PERIOD = Duration.ofMinutes(1);

    /** Tracks not updated within this duration are considered stale. */
    private Duration STALE_PERIOD = Duration.ofMinutes(30);

    /** The instant in time when the last pruning job ran */
    @GuardedBy("lock")
    private Instant timeOfLastStaleCheck = EPOCH;

    //
    // Fields and methods related to event firing
    // The event bus is Guava Eventbus - see more: http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/eventbus/EventBus.html
    //

    private final ExecutorService eventBusExecutor = Executors.newCachedThreadPool();
    private final EventBus eventBus = new AsyncEventBus(eventBusExecutor);

    @Override
    public void registerSubscriber(Object subscriber) {
        eventBus.register(subscriber);
        LOG.info("Subscribed to tracker events: " + subscriber);
    }

    private void fireTrackCreated(AISTrack track) {
        eventBus.post(new AisTrackCreatedEvent(track));
    }

    private void fireTrackUpdated(AISTrack track) {
        eventBus.post(new AisTrackUpdatedEvent(track));
    }

    private void fireTrackDynamicsUpdated(AISTrack track) {
        eventBus.post(new AisTrackDynamicsUpdatedEvent(track));
    }

    private void fireTrackDeleted(AISTrack track) {
        eventBus.post(new AisTrackDeletedEvent(track));
    }

    private void fireWallclockChanged(Instant wallclock) {
        eventBus.post(new WallclockChangedEvent(getWallclock()));
    }
}
