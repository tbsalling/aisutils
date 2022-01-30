package dk.tbsalling.ais.tracker;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.UnmodifiableIterator;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.PositionReport;
import dk.tbsalling.aismessages.ais.messages.ShipAndVoyageData;
import dk.tbsalling.aismessages.ais.messages.types.ShipType;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class AISTrackTest {

    static ShipAndVoyageData staticAisMessageMMSI211339980;
    static PositionReport dynamicAisMessageMMSI576048000;

    static ShipAndVoyageData staticAisMessageMMSI367524080;
    static PositionReport dynamicAisMessageMMSI367524080;

    AISTrack track;
    Instant now;

    @BeforeAll
    public static void setup() {
        staticAisMessageMMSI211339980 = (ShipAndVoyageData) AISMessage.create(NMEAMessage.fromString("!AIVDM,2,1,0,B,539S:k40000000c3G04PPh63<00000000080000o1PVG2uGD:00000000000,0*34"), NMEAMessage.fromString("!AIVDM,2,2,0,B,00000000000,2*27"));
        dynamicAisMessageMMSI576048000 = (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,18UG;P0012G?Uq4EdHa=c;7@051@,0*53"));
        staticAisMessageMMSI367524080 = (ShipAndVoyageData) AISMessage.create(NMEAMessage.fromString("!AIVDM,2,1,6,B,55NOpt400001L@O?;G0HuE9@R15D59@E:222220O0p>4440Ht6hhjH4QDiDU,0*46"), NMEAMessage.fromString("!AIVDM,2,2,6,B,QH888888880,2*38"));
        dynamicAisMessageMMSI367524080 = (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,B,15NOpt0P00qQJLvA<K4HmwwL2<4T,0*11"));
    }

    @BeforeEach
    public void createTrack() {
        now = Instant.now();
        track = new AISTrack(staticAisMessageMMSI367524080, dynamicAisMessageMMSI367524080, now, now);
    }

    @Test
    public void testConstructor1() {
        assertThrows(IllegalArgumentException.class, () -> new AISTrack(null, null, null, null));
    }

    @Test
    public void testConstructorStaticTimestampMustBeProvided() {
        assertThrows(NullPointerException.class, () -> new AISTrack(staticAisMessageMMSI211339980, null));
    }

    @Test
    public void testConstructorStaticTimestampMustBeProvided2() {
        assertThrows(IllegalArgumentException.class, () -> new AISTrack(staticAisMessageMMSI367524080, dynamicAisMessageMMSI367524080, null, now));
    }

    @Test
    public void testConstructorDynamicTimestampMustBeProvided() {
        assertThrows(NullPointerException.class, () -> new AISTrack(dynamicAisMessageMMSI367524080, null));
    }

    @Test
    public void testConstructorDynamicTimestampMustBeProvided2() {
        assertThrows(IllegalArgumentException.class, () -> new AISTrack(staticAisMessageMMSI367524080, dynamicAisMessageMMSI367524080, now, null));
    }

    @Test
    public void testConstructor2() {
        assertThrows(IllegalArgumentException.class, () -> new AISTrack(staticAisMessageMMSI211339980, dynamicAisMessageMMSI367524080, now, now));
    }

    @Test
    public void testConstructor3() {
        new AISTrack(staticAisMessageMMSI367524080, null, now, null);
        new AISTrack(null, dynamicAisMessageMMSI367524080, null, now);
    }

    @Test
    public void testConstructor4() {
        assertThrows(IllegalArgumentException.class, () -> new AISTrack(null, dynamicAisMessageMMSI367524080, now, now));
    }

    @Test
    public void testGetMmsi() {
        assertEquals(367524080, track.getMmsi());
    }

    @Test
    public void testGetStaticDataReport() {
        assertSame(staticAisMessageMMSI367524080, track.getStaticDataReport());
    }

    @Test
    public void testGetPositionReport() {
        assertSame(dynamicAisMessageMMSI367524080, track.getDynamicDataReport());
    }

    @Test
    public void testGetTimeOfStaticUpdate() {
        assertEquals(now, track.getTimeOfStaticUpdate());
    }

    @Test
    public void testGetTimeOfDynamicUpdate() {
        assertEquals(now, track.getTimeOfDynamicUpdate());
    }

    @Test
    public void testGetCallsign() {
        assertEquals("WDG3250", track.getCallsign());
    }

    @Test
    public void testGetShipName() {
        assertEquals("FOURTH QUARTER", track.getShipName());
    }

    @Test
    public void testGetShipType() {
        assertEquals(ShipType.Towing, track.getShipType());
    }

    @Test
    public void testGetToBow() {
        assertEquals(Integer.valueOf(7), track.getToBow());
    }

    @Test
    public void testGetToStern() {
        assertEquals(Integer.valueOf(14), track.getToStern());
    }

    @Test
    public void testGetToStarboard() {
        assertEquals(Integer.valueOf(4), track.getToStarboard());
    }

    @Test
    public void testGetToPort() {
        assertEquals(Integer.valueOf(4), track.getToPort());
    }

    @Test
    public void testGetLatitude() {
        assertEquals(Float.valueOf(30.048879623413086f), track.getLatitude(), 1e-5);
    }

    @Test
    public void testGetLongitude() {
        assertEquals(Float.valueOf(-90.56784057617188f), track.getLongitude(), 1e-5);
    }

    @Test
    public void testGetSpeedOverGround() {
        assertEquals(Float.valueOf(0.0f), track.getSpeedOverGround(), 1e-5);
    }

    @Test
    public void testGetCourseOverGround() {
        assertEquals(Float.valueOf(226.3000030517578f), track.getCourseOverGround(), 1e-5);
    }

    @Test
    public void testGetTrueHeading() {
        assertEquals(Integer.valueOf(511), track.getTrueHeading());
    }

    @Test
    public void testGetSecond() {
        assertEquals(Integer.valueOf(46), track.getSecond());
    }

    @Test
    public void testDynamicHistory() {

        now = Instant.parse("2015-01-30T17:00:00.000Z");
        AISTrack track = new AISTrack((ShipAndVoyageData) AISMessage.create(NMEAMessage.fromString("!AIVDM,2,1,7,A,53AkSB02=:9TuaaR2210uDj0htELDptE8r22221J40=5562kN81TQA1DRBlj,0*1D"), NMEAMessage.fromString("!AIVDM,2,2,7,A,0ES`8888880,2*65")), now);

        assertNotNull(track.getDynamicDataHistory());
        assertEquals(0, track.getDynamicDataHistory().size());

        now = now.plusSeconds(10);
        track = new AISTrack(track, (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B")), now);
        assertNotNull(track.getDynamicDataHistory());
        assertEquals(0, track.getDynamicDataHistory().size());

        now = now.plusSeconds(10);
        track = new AISTrack(track, (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,13AkSB0000PhAmHPoTNeoQF@0H6>,0*4B")), now);
        assertNotNull(track.getDynamicDataHistory());
        assertEquals(1, track.getDynamicDataHistory().size());

        now = now.plusSeconds(10);
        track = new AISTrack(track, (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,13AkSB0000PhAmHPoTNcp1Fp0D17,0*00")), now);
        assertNotNull(track.getDynamicDataHistory());
        assertEquals(2, track.getDynamicDataHistory().size());

        now = now.plusSeconds(10);
        track = new AISTrack(track, (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,B,13AkSB0000PhAmJPoTMoiQFT0D1:,0*5E")), now);
        assertNotNull(track.getDynamicDataHistory());
        assertEquals(3, track.getDynamicDataHistory().size());

        now = now.plusSeconds(10);
        track = new AISTrack(track, (PositionReport) AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,33AkSB0000PhAm@PoTNaR1Fp0001,0*59")), now);
        assertNotNull(track.getDynamicDataHistory());
        assertEquals(4, track.getDynamicDataHistory().size());

        ImmutableSortedMap<Instant, DynamicDataReport> dynamicDataHistory = track.getDynamicDataHistory();

        ImmutableSortedSet<Instant> instants = dynamicDataHistory.navigableKeySet();
        assertEquals(4, instants.size());

        UnmodifiableIterator<Instant> iterator = instants.iterator();

        Instant instant = iterator.next();
        assertEquals(Instant.parse("2015-01-30T17:00:10.000Z"), instant);
        DynamicDataReport historicDynamicDataReport = dynamicDataHistory.get(instant);
        assertEquals(AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,B,33AkSB5000PhAltPoTK;@1GL0000,0*1B")), historicDynamicDataReport);

        instant = iterator.next();
        assertEquals(Instant.parse("2015-01-30T17:00:20.000Z"), instant);
        historicDynamicDataReport = dynamicDataHistory.get(instant);
        assertEquals(AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,13AkSB0000PhAmHPoTNeoQF@0H6>,0*4B")), historicDynamicDataReport);

        instant = iterator.next();
        assertEquals(Instant.parse("2015-01-30T17:00:30.000Z"), instant);
        historicDynamicDataReport = dynamicDataHistory.get(instant);
        assertEquals(AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,13AkSB0000PhAmHPoTNcp1Fp0D17,0*00")), historicDynamicDataReport);

        instant = iterator.next();
        assertEquals(Instant.parse("2015-01-30T17:00:40.000Z"), instant);
        historicDynamicDataReport = dynamicDataHistory.get(instant);
        assertEquals(AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,B,13AkSB0000PhAmJPoTMoiQFT0D1:,0*5E")), historicDynamicDataReport);

        assertEquals(AISMessage.create(NMEAMessage.fromString("!AIVDM,1,1,,A,33AkSB0000PhAm@PoTNaR1Fp0001,0*59")), track.getDynamicDataReport());
    }

}