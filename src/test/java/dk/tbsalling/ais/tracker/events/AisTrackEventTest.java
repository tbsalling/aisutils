package dk.tbsalling.ais.tracker.events;

import dk.tbsalling.ais.tracker.AISTrack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AisTrackEventTest {

    @Mock
    private AISTrack mockAisTrack;

    // Concrete implementation for testing the abstract class
    private static class ConcreteAisTrackEvent extends AisTrackEvent {
        public ConcreteAisTrackEvent(AISTrack aisTrack) {
            super(aisTrack);
        }
    }

    @Test
    void constructor_withValidTrack_createsEvent() {
        // Arrange
        long expectedMmsi = 123456789L;
        when(mockAisTrack.getMmsi()).thenReturn(expectedMmsi);

        // Act
        AisTrackEvent event = new ConcreteAisTrackEvent(mockAisTrack);

        // Assert
        assertNotNull(event);
        assertEquals(expectedMmsi, event.getMmsi());
        assertSame(mockAisTrack, event.getAisTrack());
    }

    @Test
    void constructor_withNullTrack_throwsException() {
        // Arrange
        AISTrack nullTrack = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> new ConcreteAisTrackEvent(nullTrack));
    }

    @Test
    void getMmsi_returnsCorrectValue() {
        // Arrange
        long expectedMmsi = 999999999L;
        when(mockAisTrack.getMmsi()).thenReturn(expectedMmsi);
        AisTrackEvent event = new ConcreteAisTrackEvent(mockAisTrack);

        // Act
        long actualMmsi = event.getMmsi();

        // Assert
        assertEquals(expectedMmsi, actualMmsi);
    }

    @Test
    void getAisTrack_returnsSameTrackInstance() {
        // Arrange
        AisTrackEvent event = new ConcreteAisTrackEvent(mockAisTrack);

        // Act
        AISTrack result = event.getAisTrack();

        // Assert
        assertSame(mockAisTrack, result);
    }

    @Test
    void toString_returnsFormattedString() {
        // Arrange
        AisTrackEvent event = new ConcreteAisTrackEvent(mockAisTrack);

        // Act
        String result = event.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("AisTrackEvent"));
        assertTrue(result.contains("aisTrack="));
    }

    @Test
    void getMmsi_calledMultipleTimes_usesCachedTrackMmsi() {
        // Arrange
        long expectedMmsi = 111111111L;
        when(mockAisTrack.getMmsi()).thenReturn(expectedMmsi);
        AisTrackEvent event = new ConcreteAisTrackEvent(mockAisTrack);

        // Act
        long firstCall = event.getMmsi();
        long secondCall = event.getMmsi();

        // Assert
        assertEquals(expectedMmsi, firstCall);
        assertEquals(expectedMmsi, secondCall);
    }
}
