package dk.tbsalling.ais.tracker.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WallclockChangedEventTest {

    @Test
    void constructor_withValidInstant_createsEvent() {
        // Arrange
        Instant expectedWallclock = Instant.parse("2023-01-15T10:30:00Z");

        // Act
        WallclockChangedEvent event = new WallclockChangedEvent(expectedWallclock);

        // Assert
        assertNotNull(event);
        assertEquals(expectedWallclock, event.getWallclock());
    }

    @Test
    void constructor_withNullInstant_throwsException() {
        // Arrange
        Instant nullInstant = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> new WallclockChangedEvent(nullInstant));
    }

    @Test
    void getWallclock_returnsCorrectValue() {
        // Arrange
        Instant expectedWallclock = Instant.parse("2023-06-20T14:45:30Z");
        WallclockChangedEvent event = new WallclockChangedEvent(expectedWallclock);

        // Act
        Instant actualWallclock = event.getWallclock();

        // Assert
        assertEquals(expectedWallclock, actualWallclock);
    }

    @Test
    void getWallclock_returnsSameInstance() {
        // Arrange
        Instant expectedWallclock = Instant.now();
        WallclockChangedEvent event = new WallclockChangedEvent(expectedWallclock);

        // Act
        Instant actualWallclock = event.getWallclock();

        // Assert
        assertSame(expectedWallclock, actualWallclock);
    }

    @Test
    void constructor_withPastInstant_createsEvent() {
        // Arrange
        Instant pastInstant = Instant.parse("2020-01-01T00:00:00Z");

        // Act
        WallclockChangedEvent event = new WallclockChangedEvent(pastInstant);

        // Assert
        assertNotNull(event);
        assertEquals(pastInstant, event.getWallclock());
    }

    @Test
    void constructor_withFutureInstant_createsEvent() {
        // Arrange
        Instant futureInstant = Instant.parse("2030-12-31T23:59:59Z");

        // Act
        WallclockChangedEvent event = new WallclockChangedEvent(futureInstant);

        // Assert
        assertNotNull(event);
        assertEquals(futureInstant, event.getWallclock());
    }
}
