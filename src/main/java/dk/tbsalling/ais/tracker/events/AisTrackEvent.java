package dk.tbsalling.ais.tracker.events;

import dk.tbsalling.ais.tracker.AISTrack;

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * This event is fired whenever a new AisTrack is created by the tracker.
 */
@Immutable
public abstract class AisTrackEvent {

    public AisTrackEvent(AISTrack aisTrack) {
        requireNonNull(aisTrack);
        this.aisTrack = aisTrack;
    }

    public long getMmsi() {
        return aisTrack.getMmsi();
    }

    public AISTrack getAisTrack() {
        return aisTrack;
    }

    @Override
    public String toString() {
        return "AisTrackEvent{" +
                "aisTrack=" + aisTrack +
                '}';
    }

    private AISTrack aisTrack;

}
