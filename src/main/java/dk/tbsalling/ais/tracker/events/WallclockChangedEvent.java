package dk.tbsalling.ais.tracker.events;

import javax.annotation.concurrent.Immutable;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * This event is fired at irregular intervals at the tracker's
 * discretion to indicate that the wallclock has changed.
 */
@Immutable
public final class WallclockChangedEvent {

    public WallclockChangedEvent(Instant wallclock) {
        requireNonNull(wallclock);
        this.wallclock = wallclock;
    }

    public Instant getWallclock() {
        return wallclock;
    }

    private Instant wallclock;

}
