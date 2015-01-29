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
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.BasicShipDynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.ais.messages.ShipStaticDataReport;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

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
        updateAisTrack(aisMessage, messageTimestamp);
    }

    private void updateAisTrack(final AISMessage aisMessage, final Instant messageTimestamp) {
        /* extract mmsi from message */
        final long mmsi = aisMessage.getSourceMmsi().getMMSI();

        lock.lock();
        try {
            if (aisMessage instanceof ShipStaticDataReport) {
                if (isVesselTracked(mmsi)) {
                    updateVessel(mmsi, (ShipStaticDataReport) aisMessage, messageTimestamp);
                } else {
                    insertVessel(mmsi, (ShipStaticDataReport) aisMessage, messageTimestamp);
                }
            } else if (aisMessage instanceof BasicShipDynamicDataReport) {
                if (isVesselTracked(mmsi)) {
                    updateVessel(mmsi, (BasicShipDynamicDataReport) aisMessage, messageTimestamp);
                } else {
                    insertVessel(mmsi, (BasicShipDynamicDataReport) aisMessage, messageTimestamp);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if a given vessel is currently tracked by the tracker.
     * @param mmsi The MMSI no.
     * @return true if the vessel is currently tracked, false if not.
     */
    public boolean isVesselTracked(long mmsi) {
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

    private void insertVessel(final long mmsi, final ShipStaticDataReport shipStaticDataReport, final Instant msgTimestamp) {
        tracks.put(mmsi, new AisTrack(shipStaticDataReport, msgTimestamp));
    }

    private void insertVessel(final long mmsi, final BasicShipDynamicDataReport basicShipDynamicDataReport, final Instant msgTimestamp) {
        tracks.put(mmsi, new AisTrack(basicShipDynamicDataReport, msgTimestamp));
    }

    private void updateVessel(final long mmsi, final ShipStaticDataReport shipStaticDataReport, final Instant msgTimestamp) {
        AisTrack oldTrack = tracks.get(mmsi);

        if (msgTimestamp.isBefore(oldTrack.getTimeOfStaticUpdate()))
            throw new IllegalArgumentException("Cannot update track with an older message: " + msgTimestamp + " is before previous update " + oldTrack.getTimeOfStaticUpdate());

        AisTrack newTrack = new AisTrack(shipStaticDataReport, oldTrack.getDynamicDataReport(), msgTimestamp, oldTrack.getTimeOfDynamicUpdate());
        tracks.put(mmsi, newTrack);
    }

    private void updateVessel(final long mmsi, final BasicShipDynamicDataReport basicShipDynamicDataReport, final Instant msgTimestamp) {
        AisTrack oldTrack = tracks.get(mmsi);

        if (msgTimestamp.isBefore(oldTrack.getTimeOfDynamicUpdate()))
            throw new IllegalArgumentException("Cannot update track with an older message: " + msgTimestamp + " is before previous update " + oldTrack.getTimeOfDynamicUpdate());

        AisTrack newTrack = new AisTrack(oldTrack.getStaticDataReport(), basicShipDynamicDataReport, oldTrack.getTimeOfStaticUpdate(), msgTimestamp);
        tracks.put(mmsi, newTrack);
    }

    private ReentrantLock lock = new ReentrantLock();

    @GuardedBy("lock")
    private Map<Long, AisTrack> tracks = new HashMap<>();

}
