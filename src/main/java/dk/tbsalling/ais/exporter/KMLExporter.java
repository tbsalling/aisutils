package dk.tbsalling.ais.exporter;

import dk.tbsalling.ais.tracker.AISTrack;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * KMLExporter provides functionality to export AIS track data to KML (Keyhole Markup Language) format
 * for visualization in Google Earth and other KML-compatible applications.
 * 
 * <p>The exporter can generate KML documents containing vessel positions, metadata, and track history.
 * Each vessel is represented as a Placemark with its current position and detailed information including
 * name, MMSI, callsign, ship type, speed, course, and heading.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * AISTracker tracker = new AISTracker();
 * // ... update tracker with AIS messages ...
 * 
 * Set&lt;AISTrack&gt; tracks = tracker.getAisTracks();
 * try (FileWriter writer = new FileWriter("vessels.kml")) {
 *     KMLExporter.exportToKML(tracks, writer);
 * }
 * </pre>
 * 
 * @see AISTrack
 */
public class KMLExporter {
    
    private static final String KML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "<Document>\n" +
            "  <name>AIS Vessel Tracks</name>\n" +
            "  <description>Vessel positions from AIS data</description>\n";
    
    private static final String KML_FOOTER = "</Document>\n</kml>\n";
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Exports a collection of AIS tracks to KML format.
     * 
     * @param tracks the collection of AIS tracks to export
     * @param writer the writer to output KML content to
     * @throws IOException if an I/O error occurs during writing
     * @throws IllegalArgumentException if tracks is null or writer is null
     */
    public static void exportToKML(Collection<AISTrack> tracks, Writer writer) throws IOException {
        if (tracks == null) {
            throw new IllegalArgumentException("tracks cannot be null");
        }
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }
        
        writer.write(KML_HEADER);
        
        for (AISTrack track : tracks) {
            if (track.getLatitude() != null && track.getLongitude() != null) {
                writeTrackPlacemark(track, writer);
            }
        }
        
        writer.write(KML_FOOTER);
        writer.flush();
    }
    
    /**
     * Exports a single AIS track to KML format.
     * 
     * @param track the AIS track to export
     * @param writer the writer to output KML content to
     * @throws IOException if an I/O error occurs during writing
     * @throws IllegalArgumentException if track is null, writer is null, or track has no position data
     */
    public static void exportSingleTrackToKML(AISTrack track, Writer writer) throws IOException {
        if (track == null) {
            throw new IllegalArgumentException("track cannot be null");
        }
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }
        if (track.getLatitude() == null || track.getLongitude() == null) {
            throw new IllegalArgumentException("track must have valid position data (latitude and longitude)");
        }
        
        writer.write(KML_HEADER);
        writeTrackPlacemark(track, writer);
        writer.write(KML_FOOTER);
        writer.flush();
    }
    
    private static void writeTrackPlacemark(AISTrack track, Writer writer) throws IOException {
        writer.write("  <Placemark>\n");
        
        // Name - use ship name if available, otherwise MMSI
        String name = track.getShipName() != null && !track.getShipName().trim().isEmpty() 
                ? track.getShipName().trim() 
                : "MMSI " + track.getMmsi();
        writer.write("    <name>");
        writer.write(escapeXml(name));
        writer.write("</name>\n");
        
        // Description with detailed vessel information
        writer.write("    <description><![CDATA[\n");
        writer.write("      <b>MMSI:</b> " + track.getMmsi() + "<br/>\n");
        
        if (track.getCallsign() != null && !track.getCallsign().trim().isEmpty()) {
            writer.write("      <b>Callsign:</b> " + escapeXml(track.getCallsign().trim()) + "<br/>\n");
        }
        
        if (track.getShipName() != null && !track.getShipName().trim().isEmpty()) {
            writer.write("      <b>Vessel Name:</b> " + escapeXml(track.getShipName().trim()) + "<br/>\n");
        }
        
        if (track.getShipType() != null) {
            writer.write("      <b>Ship Type:</b> " + track.getShipType() + "<br/>\n");
        }
        
        if (track.getTransponderClass() != null) {
            writer.write("      <b>Transponder Class:</b> " + track.getTransponderClass() + "<br/>\n");
        }
        
        writer.write("      <b>Position:</b> " + 
                String.format("%.6f", track.getLatitude()) + ", " + 
                String.format("%.6f", track.getLongitude()) + "<br/>\n");
        
        if (track.getSpeedOverGround() != null) {
            writer.write("      <b>Speed:</b> " + String.format("%.1f", track.getSpeedOverGround()) + " knots<br/>\n");
        }
        
        if (track.getCourseOverGround() != null) {
            writer.write("      <b>Course:</b> " + String.format("%.1f", track.getCourseOverGround()) + "°<br/>\n");
        }
        
        if (track.getTrueHeading() != null && track.getTrueHeading() != 511) {
            writer.write("      <b>Heading:</b> " + track.getTrueHeading() + "°<br/>\n");
        }
        
        if (track.getTimeOfLastUpdate() != null) {
            writer.write("      <b>Last Update:</b> " + TIME_FORMATTER.format(track.getTimeOfLastUpdate()) + "<br/>\n");
        }
        
        // Add vessel dimensions if available
        try {
            Integer toBow = track.getToBow();
            Integer toStern = track.getToStern();
            Integer toPort = track.getToPort();
            Integer toStarboard = track.getToStarboard();
            
            if (toBow != null || toStern != null || toPort != null || toStarboard != null) {
                writer.write("      <b>Dimensions:</b> ");
                if (toBow != null && toStern != null) {
                    writer.write("Length " + (toBow + toStern) + "m ");
                }
                if (toPort != null && toStarboard != null) {
                    writer.write("(Beam " + (toPort + toStarboard) + "m)");
                }
                writer.write("<br/>\n");
            }
        } catch (Exception e) {
            // Skip dimensions if there's an error retrieving them
        }
        
        writer.write("    ]]></description>\n");
        
        // Point coordinates (longitude, latitude, altitude)
        writer.write("    <Point>\n");
        writer.write("      <coordinates>");
        writer.write(String.format("%.6f,%.6f,0", track.getLongitude(), track.getLatitude()));
        writer.write("</coordinates>\n");
        writer.write("    </Point>\n");
        
        writer.write("  </Placemark>\n");
    }
    
    /**
     * Escapes special XML characters in a string.
     */
    private static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
