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
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

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
public class AisTracker {

    /**
     * Update the tracker with a new AIS message.
     *
     * If there is a reception timestamp in the meta data of the AIS message, then it will be used as the
     * message timestamp. If not, current system time will be used as the timestamp.
     *
     * @param aisMessage the AIS message.
     */
    public void update(AISMessage aisMessage) {
        requireNonNull(aisMessage);

        Metadata metadata = aisMessage.getMetadata();
        Instant messageTimestamp = metadata == null ? Instant.now(Clock.systemUTC()) : Instant.ofEpochMilli(metadata.getReceived());
        updateAisTrack(aisMessage, messageTimestamp);
    }

    /**
     * Update the tracker with a new AIS message.
     *
     * @param aisMessage the AIS message.
     * @param messageTimestamp the time this AIS message was received.
     */
    public void update(AISMessage aisMessage, Instant messageTimestamp) {
        requireNonNull(aisMessage);
        requireNonNull(messageTimestamp);

        updateAisTrack(aisMessage, messageTimestamp);
    }

    /**
     * Check if a given vessel is currently tracked by the tracker.
     * @param mmsi The MMSI no.
     * @return true if the vessel is currently tracked, false if not.
     */
    public boolean isTracked(long mmsi) {
        lock.lock();
        try {
            return tracks.containsKey(mmsi);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Count the no. of tracks currently being tracked.
     * @return the no of tracks.
     */
    public int getNumberOfAisTracks() {
        lock.lock();
        try {
            return tracks.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lookup a tracked AisTrack with the given mmsi no.
     * @param mmsi the mmsi no. to lookup.
     * @return The tracked AisTrack or null if no such track is currently tracked.
     */
    public AisTrack getAisTrack(long mmsi) {
        lock.lock();
        try {
            return tracks.get(mmsi);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Extract an immutable copy of all tracks currently tracked.
     * @return An immutable set of all tracks currently tracked.
     */
    public Set<AisTrack> getAisTracks() {
        lock.lock();
        try {
            return ImmutableSet.copyOf(tracks.values());
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
            wallclock = messageTimestamp;
        } finally {
            lock.unlock();
        }
    }

    private void insertAisTrack(final long mmsi, final StaticDataReport shipStaticDataReport, final Instant msgTimestamp) {
        tracks.put(mmsi, new AisTrack(shipStaticDataReport, msgTimestamp));
    }

    private void insertAisTrack(final long mmsi, final DynamicDataReport basicShipDynamicDataReport, final Instant msgTimestamp) {
        tracks.put(mmsi, new AisTrack(basicShipDynamicDataReport, msgTimestamp));
    }

    private void updateAisTrack(final long mmsi, final StaticDataReport shipStaticDataReport, final Instant msgTimestamp) {
        AisTrack oldTrack = tracks.get(mmsi);

        if (msgTimestamp.isBefore(oldTrack.getTimeOfLastUpdate()))
            throw new IllegalArgumentException("Cannot update track with an older message: " + msgTimestamp + " is before previous update " + oldTrack.getTimeOfStaticUpdate());

        AisTrack newTrack = new AisTrack(shipStaticDataReport, oldTrack.getDynamicDataReport(), msgTimestamp, oldTrack.getTimeOfDynamicUpdate());
        tracks.put(mmsi, newTrack);
    }

    private void updateAisTrack(final long mmsi, final DynamicDataReport basicShipDynamicDataReport, final Instant msgTimestamp) {
        AisTrack oldTrack = tracks.get(mmsi);

        if (msgTimestamp.isBefore(oldTrack.getTimeOfLastUpdate()))
            throw new IllegalArgumentException("Cannot update track with an older message: " + msgTimestamp + " is before previous update " + oldTrack.getTimeOfDynamicUpdate());

        AisTrack newTrack = new AisTrack(oldTrack.getStaticDataReport(), basicShipDynamicDataReport, oldTrack.getTimeOfStaticUpdate(), msgTimestamp);
        tracks.put(mmsi, newTrack);
    }

    private ReentrantLock lock = new ReentrantLock();

    @GuardedBy("lock")
    private Map<Long, AisTrack> tracks = new HashMap<>();

    // --- Fields related to wall clock

    /** Time of last update */
    @GuardedBy("lock")
    private Instant wallclock = Instant.EPOCH;

    // --- Fields and methods related to pruning

    /** Run through all tracks and prune historic items which have expired */
    private void pruneTracks() {
        lock.lock();
        try {
            Map<Long, AisTrack> prunedTracks = Maps.newTreeMap();
            tracks.forEach((mmsi, track) -> {
                if (TRACK_NEEDS_PRUNING.test(track)) {
                    prunedTracks.put(track.getMmsi(), new AisTrack(track, INSTANT_IMPLIES_PRUNING));
                }
            });
            prunedTracks.forEach((mmsi, track) -> tracks.put(mmsi, prunedTracks.get(mmsi)));
        } finally {
            lock.unlock();
        }
    }

    /** Max duration to keep dynamic history of each track */
    private final static Duration DYNAMIC_DATA_HISTORY_MAX_AGE = Duration.ofHours(6);

    /** Predicate for instants which imply that pruning is required */
    private final Predicate<Instant> INSTANT_IMPLIES_PRUNING  = instant -> instant.isBefore(wallclock.minus(DYNAMIC_DATA_HISTORY_MAX_AGE));

    /** Predicate for tracks which need pruning of their dynamic history */
    private final Predicate<AisTrack> TRACK_NEEDS_PRUNING = aisTrack -> INSTANT_IMPLIES_PRUNING.test(aisTrack.getDynamicDataHistory().firstKey());

}
