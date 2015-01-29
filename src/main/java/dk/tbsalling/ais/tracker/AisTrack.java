package dk.tbsalling.ais.tracker;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.BasicShipDynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.ShipDynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.ShipStaticDataReport;
import dk.tbsalling.aismessages.ais.messages.types.ShipType;

import javax.annotation.concurrent.Immutable;
import java.time.Instant;

/**
 * The AisTrack class contains the consolidated information known about a given target, normally as the result
 * of several received AIS messages.
 */
@Immutable
public final class AisTrack {

    AisTrack(ShipStaticDataReport staticDataReport, Instant timeOfStaticUpdate) {
        this.mmsi = ((AISMessage) staticDataReport).getSourceMmsi().getMMSI();
        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = null;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = null;

        validateState();
    }

    AisTrack(BasicShipDynamicDataReport dynamicDataReport, Instant timeOfDynamicUpdate) {
        this.mmsi = ((AISMessage) dynamicDataReport).getSourceMmsi().getMMSI();
        this.staticDataReport = null;
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = null;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;

        validateState();
    }

    AisTrack(ShipStaticDataReport staticDataReport, BasicShipDynamicDataReport dynamicDataReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate) {
        long mmsiStatic = staticDataReport != null ? ((AISMessage) staticDataReport).getSourceMmsi().getMMSI() : -1;
        long mmsiDynamic = dynamicDataReport != null ? ((AISMessage) dynamicDataReport).getSourceMmsi().getMMSI() : -1;

        if (mmsiStatic == -1 && mmsiDynamic == -1)
            throw new IllegalArgumentException();
        if (mmsiStatic != -1 && mmsiDynamic != -1 && mmsiStatic != mmsiDynamic)
            throw new IllegalArgumentException("Provided constructor arguments must have same MMSI, not " + mmsiStatic + " and " + mmsiDynamic);

        this.mmsi = mmsiStatic;
        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;

        validateState();
    }

    private void validateState() {
        if (staticDataReport == null && dynamicDataReport == null)
            throw new IllegalArgumentException("A ShipStaticDataReport or BasicShipDynamicDataReport must be provided");
        if (staticDataReport != null && timeOfStaticUpdate == null)
            throw new IllegalArgumentException("timeOfStaticUpdate cannot be null when staticDataReport is not");
        if (dynamicDataReport != null && timeOfDynamicUpdate == null)
            throw new IllegalArgumentException("timeOfDynamicUpdate cannot be null when dynamicDataReport is not");
        if (timeOfStaticUpdate != null && staticDataReport == null)
            throw new IllegalArgumentException("timeOfStaticUpdate cannot be provided when staticDataReport is not");
        if (timeOfDynamicUpdate != null && dynamicDataReport == null)
            throw new IllegalArgumentException("timeOfDynamicUpdate cannot be provided when dynamicDataReport is not");
    }

    @Override
    public String toString() {
        return "AisTrack{" +
                "mmsi=" + mmsi +
                ", callsign='" + getCallsign() + '\'' +
                ", shipName='" + getShipName() + '\'' +
                ", shipType=" + getShipType() +
                ", toBow=" + getToBow() +
                ", toStern=" + getToStern() +
                ", toStarboard=" + getToStarboard() +
                ", toPort=" + getToPort() +
                ", latitude=" + getLatitude() +
                ", longitude=" + getLongitude() +
                ", speedOverGround=" + getSpeedOverGround() +
                ", courseOverGround=" + getCourseOverGround() +
                ", trueHeading=" + getTrueHeading() +
                ", second=" + getSecond() +
                '}';
    }

    public long getMmsi() {
        return mmsi;
    }

    public Instant getTimeOfStaticUpdate() {
        return timeOfStaticUpdate;
    }

    public Instant getTimeOfDynamicUpdate() {
        return timeOfDynamicUpdate;
    }

    public ShipStaticDataReport getStaticDataReport() {
        return staticDataReport;
    }

    public BasicShipDynamicDataReport getDynamicDataReport() {
        return dynamicDataReport;
    }

    public String getCallsign() {
        return staticDataReport != null ? staticDataReport.getCallsign() : null;
    }

    public String getShipName() {
        return staticDataReport != null ? staticDataReport.getShipName() : null;
    }

    public ShipType getShipType()  {
        return staticDataReport != null ? staticDataReport.getShipType() : null;
    }

    public Integer getToBow()  {
        return staticDataReport != null ? staticDataReport.getToBow() : null;
    }

    public Integer getToStern()  {
        return staticDataReport != null ? staticDataReport.getToStern() : null;
    }

    public Integer getToStarboard()  {
        return staticDataReport != null ? staticDataReport.getToStarboard() : null;
    }

    public Integer getToPort()  {
        return staticDataReport != null ? staticDataReport.getToPort() : null;
    }

    public Float getLatitude()  {
        return dynamicDataReport != null ? dynamicDataReport.getLatitude() : null;
    }

    public Float getLongitude()  {
        return dynamicDataReport != null ? dynamicDataReport.getLongitude() : null;
    }

    public Float getSpeedOverGround()  {
        return dynamicDataReport != null ? dynamicDataReport.getSpeedOverGround() : null;
    }

    public Float getCourseOverGround()  {
        return dynamicDataReport != null ? dynamicDataReport.getCourseOverGround() : null;
    }

    public Integer getTrueHeading()  {
        return dynamicDataReport instanceof ShipDynamicDataReport ? ((ShipDynamicDataReport) dynamicDataReport).getTrueHeading() : null;
    }

    public Integer getSecond()  {
        return dynamicDataReport instanceof ShipDynamicDataReport ? ((ShipDynamicDataReport) dynamicDataReport).getSecond() : null;
    }

    private final long mmsi;
    private final ShipStaticDataReport staticDataReport;
    private final BasicShipDynamicDataReport dynamicDataReport;
    private final Instant timeOfStaticUpdate;
    private final Instant timeOfDynamicUpdate;

}
