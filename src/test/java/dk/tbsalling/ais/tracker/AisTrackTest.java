package dk.tbsalling.ais.tracker;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.PositionReport;
import dk.tbsalling.aismessages.ais.messages.ShipAndVoyageData;
import dk.tbsalling.aismessages.ais.messages.types.ShipType;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class AisTrackTest {

    static ShipAndVoyageData staticAisMessageMMSI211339980;
    static PositionReport dynamicAisMessageMMSI576048000;

    static ShipAndVoyageData staticAisMessageMMSI367524080;
    static PositionReport dynamicAisMessageMMSI367524080;

    AisTrack track;

    @BeforeClass
    public static void setup() {
        staticAisMessageMMSI211339980 = (ShipAndVoyageData) AISMessage.create(NMEAMessage.fromString("!AIVDM,2,1,0,B,539S:k40000000c3G04PPh63<00000000080000o1PVG2uGD:00000000000,0*34"), NMEAMessage.fromString("!AIVDM,2,2,0,B,00000000000,2*27"));
        dynamicAisMessageMMSI576048000 = (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,18UG;P0012G?Uq4EdHa=c;7@051@,0*53"));
        staticAisMessageMMSI367524080 = (ShipAndVoyageData) AISMessage.create(NMEAMessage.fromString("!AIVDM,2,1,6,B,55NOpt400001L@O?;G0HuE9@R15D59@E:222220O0p>4440Ht6hhjH4QDiDU,0*46"), NMEAMessage.fromString("!AIVDM,2,2,6,B,QH888888880,2*38"));
        dynamicAisMessageMMSI367524080 = (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,B,15NOpt0P00qQJLvA<K4HmwwL2<4T,0*11"));
    }

    @Before
    public void createTrack() {
        track = new AisTrack(staticAisMessageMMSI367524080, dynamicAisMessageMMSI367524080);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor1() throws Exception {
        new AisTrack(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor2() throws Exception {
        new AisTrack(staticAisMessageMMSI211339980, dynamicAisMessageMMSI367524080);
    }

    @Test
    public void testConstructor3() throws Exception {
        new AisTrack(staticAisMessageMMSI367524080, null);
        new AisTrack(null, dynamicAisMessageMMSI367524080);
    }

    @Test
    public void testGetMmsi() throws Exception {
        assertEquals(367524080, track.getMmsi());
    }

    @Test
    public void testGetStaticDataReport() throws Exception {
        assertSame(staticAisMessageMMSI367524080, track.getStaticDataReport());
    }

    @Test
    public void testGetPositionReport() throws Exception {
        assertSame(dynamicAisMessageMMSI367524080, track.getDynamicDataReport());
    }

    @Test
    public void testGetCallsign() throws Exception {
        assertEquals("WDG3250", track.getCallsign());
    }

    @Test
    public void testGetShipName() throws Exception {
        assertEquals("FOURTH QUARTER", track.getShipName());
    }

    @Test
    public void testGetShipType() throws Exception {
        assertEquals(ShipType.Towing, track.getShipType());
    }

    @Test
    public void testGetToBow() throws Exception {
        assertEquals(Integer.valueOf(7), track.getToBow());
    }

    @Test
    public void testGetToStern() throws Exception {
        assertEquals(Integer.valueOf(14), track.getToStern());
    }

    @Test
    public void testGetToStarboard() throws Exception {
        assertEquals(Integer.valueOf(4), track.getToStarboard());
    }

    @Test
    public void testGetToPort() throws Exception {
        assertEquals(Integer.valueOf(4), track.getToPort());
    }

    @Test
    public void testGetLatitude() throws Exception {
        assertEquals(Float.valueOf(30.048879623413086f), track.getLatitude(), 1e-5);
    }

    @Test
    public void testGetLongitude() throws Exception {
        assertEquals(Float.valueOf(-90.56784057617188f), track.getLongitude(), 1e-5);
    }

    @Test
    public void testGetSpeedOverGround() throws Exception {
        assertEquals(Float.valueOf(0.0f), track.getSpeedOverGround(), 1e-5);
    }

    @Test
    public void testGetCourseOverGround() throws Exception {
        assertEquals(Float.valueOf(226.3000030517578f), track.getCourseOverGround(), 1e-5);
    }

    @Test
    public void testGetTrueHeading() throws Exception {
        assertEquals(Integer.valueOf(511), track.getTrueHeading());
    }

    @Test
    public void testGetSecond() throws Exception {
        assertEquals(Integer.valueOf(46), track.getSecond());
    }
}