package dk.tbsalling.ais.tracker;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.ExtendedDynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import dk.tbsalling.aismessages.ais.messages.types.ShipType;
import dk.tbsalling.aismessages.ais.messages.types.TransponderClass;

import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * The AisTrack class contains the consolidated information known about a given target, normally as the result
 * of several received AIS messages.
 */
@Immutable
public final class AISTrack {

    AISTrack(StaticDataReport staticDataReport, Instant timeOfStaticUpdate) {
        requireNonNull(staticDataReport);
        requireNonNull(timeOfStaticUpdate);
        validateArgs(staticDataReport, null, timeOfStaticUpdate, null);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = null;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = null;
        this.dynamicDataHistory = null;

        validateState();
    }

    AISTrack(DynamicDataReport dynamicDataReport, Instant timeOfDynamicUpdate) {
        requireNonNull(dynamicDataReport);
        requireNonNull(timeOfDynamicUpdate);
        validateArgs(null, dynamicDataReport, null, timeOfDynamicUpdate);

        this.staticDataReport = null;
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = null;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.dynamicDataHistory = null;

        validateState();
    }

    AISTrack(StaticDataReport staticDataReport, DynamicDataReport dynamicDataReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate) {
        validateArgs(staticDataReport, dynamicDataReport, timeOfStaticUpdate, timeOfDynamicUpdate);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.dynamicDataHistory = null;

        validateState();
    }

    /**
     * Create a new AisTrack using another track to build history.
     */
    AISTrack(AISTrack oldTrack, StaticDataReport staticDataReport, Instant timeOfStaticUpdate) {
        requireNonNull(staticDataReport);
        requireNonNull(timeOfStaticUpdate);
        validateArgs(staticDataReport, null, timeOfStaticUpdate, null);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = oldTrack.getDynamicDataReport();
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = oldTrack.getTimeOfDynamicUpdate();
        validateState();

        dynamicDataHistory = oldTrack.dynamicDataHistory;
    }

    /**
     * Create a new AisTrack using another track to build history.
     */
    AISTrack(AISTrack oldTrack, DynamicDataReport dynamicDataReport, Instant timeOfDynamicUpdate) {
        requireNonNull(dynamicDataReport);
        requireNonNull(timeOfDynamicUpdate);
        validateArgs(null, dynamicDataReport, null, timeOfDynamicUpdate);

        this.staticDataReport = oldTrack.getStaticDataReport();
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = oldTrack.getTimeOfStaticUpdate();
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.dynamicDataHistory = copyDynamicHistory(oldTrack);

        validateState();
    }

    /**
     * Create a new AisTrack using another track to build history.
     */
    AISTrack(AISTrack oldTrack, StaticDataReport staticDataReport, DynamicDataReport dynamicDataReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate) {
        validateArgs(staticDataReport, dynamicDataReport, timeOfStaticUpdate, timeOfDynamicUpdate);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = dynamicDataReport;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.dynamicDataHistory = copyDynamicHistory(oldTrack);

        validateState();
    }

    private ImmutableSortedMap<Instant, DynamicDataReport> copyDynamicHistory(AISTrack oldTrack) {
        ImmutableSortedMap<Instant, DynamicDataReport> dynamicHistory = null;

        if (oldTrack.timeOfDynamicUpdate != null && oldTrack.dynamicDataReport != null) {
            dynamicHistory = new ImmutableSortedMap.Builder<Instant, DynamicDataReport>(Comparator.<Instant>naturalOrder())
                .putAll(oldTrack.dynamicDataHistory == null ? Maps.newTreeMap() : oldTrack.dynamicDataHistory)
                .put(oldTrack.timeOfDynamicUpdate, oldTrack.dynamicDataReport)
                .build();
        }

        return dynamicHistory;
    }

    /** Copy constructor with support for pruning */
    AISTrack(AISTrack originalTrack, Predicate<Instant> keepInstantPredicate) {
        this.staticDataReport = originalTrack.staticDataReport;
        this.dynamicDataReport = originalTrack.dynamicDataReport;

        dynamicDataHistory = new ImmutableSortedMap.Builder<Instant, DynamicDataReport>(Comparator.<Instant>naturalOrder())
            .putAll(
                originalTrack.dynamicDataHistory.entrySet().stream()
                .filter(entry -> keepInstantPredicate.test(entry.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
            )
            .build();

        this.timeOfStaticUpdate = originalTrack.timeOfStaticUpdate;
        this.timeOfDynamicUpdate = dynamicDataHistory.lastKey();
    }

    private void validateArgs(StaticDataReport staticDataReport, DynamicDataReport dynamicDataReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate) {
        final long mmsiStatic = staticDataReport != null ? ((AISMessage) staticDataReport).getSourceMmsi().getMMSI() : -1;
        final long mmsiDynamic = dynamicDataReport != null ? ((AISMessage) dynamicDataReport).getSourceMmsi().getMMSI() : -1;
        if (mmsiStatic == -1 && mmsiDynamic == -1)
            throw new IllegalArgumentException();
        if (mmsiStatic != -1 && mmsiDynamic != -1 && mmsiStatic != mmsiDynamic)
            throw new IllegalArgumentException("Provided constructor arguments must have same MMSI, not " + mmsiStatic + " and " + mmsiDynamic);
        if (staticDataReport != null && dynamicDataReport != null && !staticDataReport.getTransponderClass().equals(dynamicDataReport.getTransponderClass())) {
            throw new IllegalArgumentException("staticDataReport is from transponder class " + staticDataReport.getTransponderClass() + ", dynamicDataReport is from transponder class " + dynamicDataReport.getTransponderClass() + ". They must be the same.");
        }

        final Instant timeOfLastUpdate = getTimeOfLastUpdate();
        if (timeOfLastUpdate != null) {
            if (timeOfStaticUpdate != null && !timeOfStaticUpdate.isAfter(timeOfLastUpdate))
                throw new IllegalArgumentException("Constructor arg timeOfStaticUpdate (" + timeOfStaticUpdate + ") must be after time of last update (" + timeOfLastUpdate + ")");
            if (timeOfDynamicUpdate != null && !timeOfDynamicUpdate.isAfter(timeOfLastUpdate))
                throw new IllegalArgumentException("Constructor arg timeOfDynamicUpdate (" + timeOfDynamicUpdate + ") must be after time of last update (" + timeOfLastUpdate + ")");
        }
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
        if (getMmsi() <= 0) // TODO http://en.wikipedia.org/wiki/Maritime_Mobile_Service_Identity
            throw new IllegalArgumentException("MMSI " + getMmsi() + " is invalid.");
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

    @Override
    public int hashCode() {
        int result = staticDataReport != null ? staticDataReport.hashCode() : 0;
        result = 31*result + (dynamicDataReport != null ? dynamicDataReport.hashCode() : 0);
        result = 31*result + (timeOfStaticUpdate != null ? timeOfStaticUpdate.hashCode() : 0);
        result = 31*result + (timeOfDynamicUpdate != null ? timeOfDynamicUpdate.hashCode() : 0);
        return result;
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
            return timeOfStaticUpdate.isBefore(timeOfDynamicUpdate) ? timeOfDynamicUpdate : timeOfStaticUpdate;
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

    /* Return an immutable and sorted map of this track's dynamic history. */
    public ImmutableSortedMap<Instant, DynamicDataReport> getDynamicDataHistory() {
        return dynamicDataHistory == null ? ImmutableSortedMap.copyOf(Maps.newTreeMap()) : dynamicDataHistory;
    }

    private final StaticDataReport staticDataReport;
    private final DynamicDataReport dynamicDataReport;
    private final Instant timeOfStaticUpdate;
    private final Instant timeOfDynamicUpdate;

    /* Dynamic history of the track excluding the most recent, current value */
    private final ImmutableSortedMap<Instant, DynamicDataReport> dynamicDataHistory;
}
