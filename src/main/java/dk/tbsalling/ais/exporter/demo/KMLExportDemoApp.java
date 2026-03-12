package dk.tbsalling.ais.exporter.demo;

import dk.tbsalling.ais.exporter.KMLExporter;
import dk.tbsalling.ais.tracker.AISTrack;
import dk.tbsalling.ais.tracker.AISTracker;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Demo application showing how to use the KML exporter to export AIS vessel tracks
 * to KML format for visualization in Google Earth or other KML-compatible applications.
 * 
 * This demo:
 * 1. Creates an AIS tracker
 * 2. Feeds it with sample NMEA AIS messages
 * 3. Exports the tracked vessels to a KML file
 */
public class KMLExportDemoApp {
    
    public static void main(String[] args) throws IOException {
        System.out.println("AIS KML Export Demo");
        System.out.println("===================\n");
        
        // Sample NMEA AIS messages containing position reports and static vessel data
        String sampleNmeaMessages = 
            "!AIVDM,2,1,6,B,55NOpt400001L@O?;G0HuE9@R15D59@E:222220O0p>4440Ht6hhjH4QDiDU,0*46\n" +
            "!AIVDM,2,2,6,B,QH888888880,2*38\n" +
            "!AIVDM,1,1,,B,15NOpt0P00qQJLvA<K4HmwwL2<4T,0*11\n" +
            "!AIVDM,1,1,,A,18UG;P0012G?Uq4EdHa=c;7@051@,0*53\n" +
            "!AIVDM,1,1,,A,15Mv5v?P00IS0J`A86KTROvN0<5k,0*12\n" +
            "!AIVDM,1,1,,A,15Mwd<PP00ISfGpA7jBr??vP0<3:,0*04\n";
        
        // Create a tracker and feed it with AIS messages
        System.out.println("Step 1: Creating AIS tracker and processing messages...");
        AISTracker tracker = new AISTracker();
        InputStream inputStream = new ByteArrayInputStream(sampleNmeaMessages.getBytes());
        tracker.update(inputStream);
        
        // Get all tracked vessels
        Set<AISTrack> tracks = tracker.getAisTracks();
        System.out.println("Step 2: Found " + tracks.size() + " vessel(s) in the message stream\n");
        
        // Display information about tracked vessels
        System.out.println("Tracked vessels:");
        System.out.println("----------------");
        for (AISTrack track : tracks) {
            String name = track.getShipName() != null && !track.getShipName().trim().isEmpty() 
                    ? track.getShipName() 
                    : "MMSI " + track.getMmsi();
            String position = track.getLatitude() != null && track.getLongitude() != null
                    ? String.format("%.4f, %.4f", track.getLatitude(), track.getLongitude())
                    : "Unknown";
            System.out.println("  - " + name + " at position: " + position);
        }
        
        // Export to KML file
        System.out.println("\nStep 3: Exporting vessels to KML file...");
        String kmlFilename = "vessels.kml";
        try (FileWriter writer = new FileWriter(kmlFilename)) {
            KMLExporter.exportToKML(tracks, writer);
        }
        
        System.out.println("✓ KML export complete! File saved as: " + kmlFilename);
        System.out.println("\nYou can now open '" + kmlFilename + "' in Google Earth or any KML-compatible application");
        System.out.println("to visualize the vessel positions.");
    }
}
