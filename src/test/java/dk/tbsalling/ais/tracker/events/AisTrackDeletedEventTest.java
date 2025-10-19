package dk.tbsalling.ais.tracker.events;

import dk.tbsalling.ais.tracker.AISTrack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AisTrackDeletedEventTest {

    @Mock
    private AISTrack mockAisTrack;

    @Test
    void constructor_withValidTrack_createsEvent() {
        // Arrange
        long expectedMmsi = 456789012L;
        when(mockAisTrack.getMmsi()).thenReturn(expectedMmsi);

        // Act
        AisTrackDeletedEvent event = new AisTrackDeletedEvent(mockAisTrack);

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
        assertThrows(NullPointerException.class, () -> new AisTrackDeletedEvent(nullTrack));
    }

    @Test
    void toString_returnsFormattedString() {
        // Arrange
        AisTrackDeletedEvent event = new AisTrackDeletedEvent(mockAisTrack);

        // Act
        String result = event.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("AisTrackDeletedEvent"));
    }

    @Test
    void getMmsi_returnsCorrectValue() {
        // Arrange
        long expectedMmsi = 777777777L;
        when(mockAisTrack.getMmsi()).thenReturn(expectedMmsi);
        AisTrackDeletedEvent event = new AisTrackDeletedEvent(mockAisTrack);

        // Act
        long actualMmsi = event.getMmsi();

        // Assert
        assertEquals(expectedMmsi, actualMmsi);
    }

    @Test
    void getAisTrack_returnsSameTrackInstance() {
        // Arrange
        AisTrackDeletedEvent event = new AisTrackDeletedEvent(mockAisTrack);

        // Act
        AISTrack result = event.getAisTrack();

        // Assert
        assertSame(mockAisTrack, result);
    }
}
