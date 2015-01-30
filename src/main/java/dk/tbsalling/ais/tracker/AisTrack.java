package dk.tbsalling.ais.tracker;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.ExtendedDynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import dk.tbsalling.aismessages.ais.messages.types.ShipType;
import dk.tbsalling.aismessages.ais.messages.types.TransponderClass;

import javax.annotation.concurrent.Immutable;
import java.time.Instant;

/**
 * The AisTrack class contains the consolidated information known about a given target, normally as the result
 * of several received AIS messages.
 */
@Immutable
public final class AisTrack {

    AisTrack(StaticDataReport staticDataReport, Instant timeOfStaticUpdate) {
        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = null;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = null;
        validateState();
    }

    AisTrack(DynamicDataReport dynamicDataReport, Instant timeOfDynamicUpdate) {
        this.staticDataReport = null;
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = null;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        validateState();
    }

    AisTrack(StaticDataReport staticDataReport, DynamicDataReport dynamicDataReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate) {
        long mmsiStatic = staticDataReport != null ? ((AISMessage) staticDataReport).getSourceMmsi().getMMSI() : -1;
        long mmsiDynamic = dynamicDataReport != null ? ((AISMessage) dynamicDataReport).getSourceMmsi().getMMSI() : -1;
        if (mmsiStatic == -1 && mmsiDynamic == -1)
            throw new IllegalArgumentException();
        if (mmsiStatic != -1 && mmsiDynamic != -1 && mmsiStatic != mmsiDynamic)
            throw new IllegalArgumentException("Provided constructor arguments must have same MMSI, not " + mmsiStatic + " and " + mmsiDynamic);
        if (staticDataReport != null && dynamicDataReport != null && ! staticDataReport.getTransponderClass().equals(dynamicDataReport.getTransponderClass())) {
            throw new IllegalArgumentException("staticDataReport is from transponder class " + staticDataReport.getTransponderClass() + ", dynamicDataReport is from transponder class " + dynamicDataReport.getTransponderClass() + ". They must be the same.");
        }

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;

        validateState();
    }

    private void validateState() {
        if (staticDataReport == null && dynamicDataReport == null)
            throw new IllegalArgumentException("A StaticDataReport or BasicDynamicDataReport must be provided");
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
                "mmsi=" + getMmsi() +
                ", transponderClass=" + getTransponderClass() +
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
        return dynamicDataReport != null ? ((AISMessage) dynamicDataReport).getSourceMmsi().getMMSI() : ((AISMessage) staticDataReport).getSourceMmsi().getMMSI();
    }

    public TransponderClass getTransponderClass() {
        return dynamicDataReport != null ? dynamicDataReport.getTransponderClass() : staticDataReport.getTransponderClass();
    }

    public Instant getTimeOfLastUpdate() {
        if (timeOfStaticUpdate == null)
            return timeOfDynamicUpdate;
        else if (timeOfDynamicUpdate == null)
            return timeOfStaticUpdate;
        else
            return timeOfStaticUpdate.compareTo(timeOfDynamicUpdate) < 0 ? timeOfDynamicUpdate : timeOfStaticUpdate;
    }

    public Instant getTimeOfStaticUpdate() {
        return timeOfStaticUpdate;
    }

    public Instant getTimeOfDynamicUpdate() {
        return timeOfDynamicUpdate;
    }

    public StaticDataReport getStaticDataReport() {
        return staticDataReport;
    }

    public DynamicDataReport getDynamicDataReport() {
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
        return dynamicDataReport instanceof ExtendedDynamicDataReport ? ((ExtendedDynamicDataReport) dynamicDataReport).getTrueHeading() : null;
    }

    public Integer getSecond()  {
        return dynamicDataReport instanceof ExtendedDynamicDataReport ? ((ExtendedDynamicDataReport) dynamicDataReport).getSecond() : null;
    }

    private final StaticDataReport staticDataReport;
    private final DynamicDataReport dynamicDataReport;
    private final Instant timeOfStaticUpdate;
    private final Instant timeOfDynamicUpdate;

}
