package dk.tbsalling.ais.tracker.events;

import dk.tbsalling.ais.tracker.AisTrack;

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * This event is fired whenever a new AisTrack is created by the tracker.
 */
@Immutable
public abstract class AisTrackEvent {

    public AisTrackEvent(AisTrack aisTrack) {
        requireNonNull(aisTrack);
        this.aisTrack = aisTrack;
    }

    public long getMmsi() {
        return aisTrack.getMmsi();
    }

    public AisTrack getAisTrack() {
        return aisTrack;
    }

    @Override
    public String toString() {
        return "AisTrackEvent{" +
                "aisTrack=" + aisTrack +
                '}';
    }

    private AisTrack aisTrack;

}
