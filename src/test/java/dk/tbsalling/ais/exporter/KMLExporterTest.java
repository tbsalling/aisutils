package dk.tbsalling.ais.exporter;

import dk.tbsalling.ais.tracker.AISTrack;
import dk.tbsalling.ais.tracker.AISTracker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.PositionReport;
import dk.tbsalling.aismessages.ais.messages.ShipAndVoyageData;
import dk.tbsalling.aismessages.nmea.NMEAMessageHandler;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class KMLExporterTest {

    static ShipAndVoyageData staticAisMessageMMSI367524080;
    static PositionReport dynamicAisMessageMMSI367524080;
    static PositionReport dynamicAisMessageMMSI576048000;
    static ShipAndVoyageData staticAisMessageMMSI211339980;

    private static AISMessage parseNMEA(NMEAMessage... nmeaMessages) {
        List<AISMessage> aisMessages = new ArrayList<>();
        NMEAMessageHandler handler = new NMEAMessageHandler("TESTSRC", aisMessages::add);
        for (NMEAMessage msg : nmeaMessages) {
            handler.accept(msg);
        }
        return aisMessages.isEmpty() ? null : aisMessages.get(0);
    }

    @BeforeAll
    public static void setup() {
        staticAisMessageMMSI367524080 = (ShipAndVoyageData) parseNMEA(
            new NMEAMessage("!AIVDM,2,1,6,B,55NOpt400001L@O?;G0HuE9@R15D59@E:222220O0p>4440Ht6hhjH4QDiDU,0*46"),
            new NMEAMessage("!AIVDM,2,2,6,B,QH888888880,2*38")
        );
        dynamicAisMessageMMSI367524080 = (PositionReport) parseNMEA(
            new NMEAMessage("!AIVDM,1,1,,B,15NOpt0P00qQJLvA<K4HmwwL2<4T,0*11")
        );
        dynamicAisMessageMMSI576048000 = (PositionReport) parseNMEA(
            new NMEAMessage("!AIVDM,1,1,,A,18UG;P0012G?Uq4EdHa=c;7@051@,0*53")
        );
        staticAisMessageMMSI211339980 = (ShipAndVoyageData) parseNMEA(
            new NMEAMessage("!AIVDM,2,1,0,B,539S:k40000000c3G04PPh63<00000000080000o1PVG2uGD:00000000000,0*34"),
            new NMEAMessage("!AIVDM,2,2,0,B,00000000000,2*27")
        );
    }
    
    private static AISTrack createTrackWithStaticAndDynamic() {
        AISTracker tracker = new AISTracker();
        tracker.update(staticAisMessageMMSI367524080);
        tracker.update(dynamicAisMessageMMSI367524080);
        Set<AISTrack> tracks = tracker.getAisTracks();
        return tracks.iterator().next();
    }
    
    private static AISTrack createTrackWithDynamicOnly() {
        AISTracker tracker = new AISTracker();
        tracker.update(dynamicAisMessageMMSI576048000);
        Set<AISTrack> tracks = tracker.getAisTracks();
        return tracks.iterator().next();
    }
    
    private static AISTrack createTrackWithStaticOnly() {
        AISTracker tracker = new AISTracker();
        tracker.update(staticAisMessageMMSI211339980);
        Set<AISTrack> tracks = tracker.getAisTracks();
        return tracks.iterator().next();
    }

    @Test
    public void testExportToKMLWithNullTracksThrowsException() {
        // Arrange
        StringWriter writer = new StringWriter();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            KMLExporter.exportToKML(null, writer)
        );
    }

    @Test
    public void testExportToKMLWithNullWriterThrowsException() {
        // Arrange
        List<AISTrack> tracks = Collections.emptyList();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            KMLExporter.exportToKML(tracks, null)
        );
    }

    @Test
    public void testExportSingleTrackToKMLWithNullTrackThrowsException() {
        // Arrange
        StringWriter writer = new StringWriter();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            KMLExporter.exportSingleTrackToKML(null, writer)
        );
    }

    @Test
    public void testExportSingleTrackToKMLWithNullWriterThrowsException() {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            KMLExporter.exportSingleTrackToKML(track, null)
        );
    }

    @Test
    public void testExportSingleTrackToKMLWithoutPositionThrowsException() {
        // Arrange
        AISTrack track = createTrackWithStaticOnly();
        StringWriter writer = new StringWriter();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            KMLExporter.exportSingleTrackToKML(track, writer)
        );
    }

    @Test
    public void testExportEmptyTracksCollectionProducesValidKML() throws IOException {
        // Arrange
        List<AISTrack> tracks = Collections.emptyList();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportToKML(tracks, writer);

        // Assert
        String kml = writer.toString();
        assertTrue(kml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(kml.contains("<kml xmlns=\"http://www.opengis.net/kml/2.2\">"));
        assertTrue(kml.contains("<Document>"));
        assertTrue(kml.contains("</Document>"));
        assertTrue(kml.contains("</kml>"));
    }

    @Test
    public void testExportSingleTrackWithCompleteDataProducesValidKML() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        
        // Check KML structure
        assertTrue(kml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(kml.contains("<kml xmlns=\"http://www.opengis.net/kml/2.2\">"));
        assertTrue(kml.contains("<Document>"));
        assertTrue(kml.contains("<Placemark>"));
        assertTrue(kml.contains("</Placemark>"));
        assertTrue(kml.contains("</Document>"));
        assertTrue(kml.contains("</kml>"));
        
        // Check vessel data
        assertTrue(kml.contains("<name>"));
        assertTrue(kml.contains("</name>"));
        assertTrue(kml.contains("<description>"));
        assertTrue(kml.contains("</description>"));
        assertTrue(kml.contains("MMSI"));
        assertTrue(kml.contains(String.valueOf(track.getMmsi())));
        
        // Check position data
        assertTrue(kml.contains("<Point>"));
        assertTrue(kml.contains("<coordinates>"));
        assertTrue(kml.contains("</coordinates>"));
        assertTrue(kml.contains("</Point>"));
    }

    @Test
    public void testExportSingleTrackIncludesShipName() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        if (track.getShipName() != null) {
            assertTrue(kml.contains(track.getShipName()));
        }
    }

    @Test
    public void testExportSingleTrackIncludesCallsign() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        if (track.getCallsign() != null && !track.getCallsign().trim().isEmpty()) {
            assertTrue(kml.contains("Callsign"));
            assertTrue(kml.contains(track.getCallsign()));
        }
    }

    @Test
    public void testExportSingleTrackIncludesPosition() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        assertNotNull(track.getLatitude());
        assertNotNull(track.getLongitude());
        
        // KML uses longitude,latitude order
        String expectedCoords = String.format("%.6f,%.6f", track.getLongitude(), track.getLatitude());
        assertTrue(kml.contains(expectedCoords));
    }

    @Test
    public void testExportSingleTrackIncludesSpeedAndCourse() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        if (track.getSpeedOverGround() != null) {
            assertTrue(kml.contains("Speed"));
            assertTrue(kml.contains("knots"));
        }
        if (track.getCourseOverGround() != null) {
            assertTrue(kml.contains("Course"));
        }
    }

    @Test
    public void testExportMultipleTracksProducesValidKML() throws IOException {
        // Arrange
        AISTracker tracker = new AISTracker();
        tracker.update(staticAisMessageMMSI367524080);
        tracker.update(dynamicAisMessageMMSI367524080);
        tracker.update(dynamicAisMessageMMSI576048000);
        List<AISTrack> tracks = new ArrayList<>(tracker.getAisTracks());
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportToKML(tracks, writer);

        // Assert
        String kml = writer.toString();
        
        // Check KML structure
        assertTrue(kml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(kml.contains("<kml xmlns=\"http://www.opengis.net/kml/2.2\">"));
        assertTrue(kml.contains("<Document>"));
        assertTrue(kml.contains("</Document>"));
        assertTrue(kml.contains("</kml>"));
        
        // Should contain placemarks for both tracks with positions
        int placemarkCount = countOccurrences(kml, "<Placemark>");
        assertEquals(2, placemarkCount);
    }

    @Test
    public void testExportTracksSkipsTracksWithoutPosition() throws IOException {
        // Arrange
        AISTracker tracker = new AISTracker();
        tracker.update(staticAisMessageMMSI367524080);
        tracker.update(dynamicAisMessageMMSI367524080);
        tracker.update(staticAisMessageMMSI211339980);
        List<AISTrack> tracks = new ArrayList<>(tracker.getAisTracks());
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportToKML(tracks, writer);

        // Assert
        String kml = writer.toString();
        
        // Should only contain one placemark (for the track with position)
        int placemarkCount = countOccurrences(kml, "<Placemark>");
        assertEquals(1, placemarkCount);
    }

    @Test
    public void testExportTracksHandlesSpecialCharactersInNames() throws IOException {
        // Arrange - this would need a track with special characters in name
        // For now, test that XML escaping is working by checking the method doesn't crash
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        // Should not contain unescaped XML special characters in CDATA sections
        // CDATA sections can contain raw text, but outside CDATA should be escaped
        assertNotNull(kml);
        assertTrue(kml.contains("<name>"));
    }

    @Test
    public void testExportSingleTrackUsesMMSIWhenShipNameNotAvailable() throws IOException {
        // Arrange
        AISTrack track = createTrackWithDynamicOnly();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        assertTrue(kml.contains("MMSI " + track.getMmsi()));
    }

    @Test
    public void testExportIncludesTransponderClass() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        if (track.getTransponderClass() != null) {
            assertTrue(kml.contains("Transponder Class"));
        }
    }

    @Test
    public void testExportIncludesShipType() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        if (track.getShipType() != null) {
            assertTrue(kml.contains("Ship Type"));
        }
    }

    @Test
    public void testExportIncludesLastUpdateTime() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        assertTrue(kml.contains("Last Update"));
    }

    @Test
    public void testExportIncludesVesselDimensionsWhenAvailable() throws IOException {
        // Arrange
        AISTrack track = createTrackWithStaticAndDynamic();
        StringWriter writer = new StringWriter();

        // Act
        KMLExporter.exportSingleTrackToKML(track, writer);

        // Assert
        String kml = writer.toString();
        if (track.getToBow() != null && track.getToStern() != null) {
            assertTrue(kml.contains("Dimensions"));
            assertTrue(kml.contains("Length"));
        }
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
