package dk.tbsalling.ais.tracker.events;

import dk.tbsalling.ais.tracker.AISTrack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AisTrackCreatedEventTest {

    @Mock
    private AISTrack mockAisTrack;

    @Test
    void constructor_withValidTrack_createsEvent() {
        // Arrange
        long expectedMmsi = 123456789L;
        when(mockAisTrack.getMmsi()).thenReturn(expectedMmsi);

        // Act
        AisTrackCreatedEvent event = new AisTrackCreatedEvent(mockAisTrack);

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
        assertThrows(NullPointerException.class, () -> new AisTrackCreatedEvent(nullTrack));
    }

    @Test
    void toString_returnsFormattedString() {
        // Arrange
        AisTrackCreatedEvent event = new AisTrackCreatedEvent(mockAisTrack);

        // Act
        String result = event.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("AisTrackCreatedEvent"));
    }

    @Test
    void getMmsi_returnsCorrectValue() {
        // Arrange
        long expectedMmsi = 555555555L;
        when(mockAisTrack.getMmsi()).thenReturn(expectedMmsi);
        AisTrackCreatedEvent event = new AisTrackCreatedEvent(mockAisTrack);

        // Act
        long actualMmsi = event.getMmsi();

        // Assert
        assertEquals(expectedMmsi, actualMmsi);
    }

    @Test
    void getAisTrack_returnsSameTrackInstance() {
        // Arrange
        AisTrackCreatedEvent event = new AisTrackCreatedEvent(mockAisTrack);

        // Act
        AISTrack result = event.getAisTrack();

        // Assert
        assertSame(mockAisTrack, result);
    }
}
