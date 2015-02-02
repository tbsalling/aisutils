package dk.tbsalling.ais.tracker.events;

import dk.tbsalling.ais.tracker.AisTrack;

import javax.annotation.concurrent.Immutable;

/**
 * This event is fired whenever a new AisTrack is created by the tracker.
 */
@Immutable
public final class AisTrackCreatedEvent extends AisTrackEvent {

    public AisTrackCreatedEvent(AisTrack aisTrack) {
        super(aisTrack);
    }

    @Override
    public String toString() {
        return "AisTrackCreatedEvent{} " + super.toString();
    }
}
