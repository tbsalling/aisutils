package dk.tbsalling.ais.tracker;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import dk.tbsalling.aismessages.ais.messages.*;
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
        validateArgs(staticDataReport, null, null, timeOfStaticUpdate, null, null);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = null;
        this.aidToNavigationReport = null;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = null;
        this.timeOfAtonUpdate = null;
        this.dynamicDataHistory = null;

        validateState();
    }

    AISTrack(DynamicDataReport dynamicDataReport, Instant timeOfDynamicUpdate) {
        requireNonNull(dynamicDataReport);
        requireNonNull(timeOfDynamicUpdate);
        validateArgs(null, dynamicDataReport, null, null, timeOfDynamicUpdate, null);

        this.staticDataReport = null;
        this.dynamicDataReport = dynamicDataReport;
        this.aidToNavigationReport = null;
        this.timeOfStaticUpdate = null;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.timeOfAtonUpdate = null;
        this.dynamicDataHistory = null;

        validateState();
    }

    AISTrack(AidToNavigationReport aidToNavigationReport, Instant timeOfAtonUpdate) {
        requireNonNull(aidToNavigationReport);
        requireNonNull(timeOfAtonUpdate);
        validateArgs(null, null, aidToNavigationReport, null, null, timeOfAtonUpdate);

        this.staticDataReport = null;
        this.dynamicDataReport = null;
        this.aidToNavigationReport = aidToNavigationReport;
        this.timeOfStaticUpdate = null;
        this.timeOfDynamicUpdate = null;
        this.timeOfAtonUpdate = timeOfAtonUpdate;
        this.dynamicDataHistory = null;

        validateState();
    }

    AISTrack(StaticDataReport staticDataReport, DynamicDataReport dynamicDataReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate) {
        validateArgs(staticDataReport, dynamicDataReport, null, timeOfStaticUpdate, timeOfDynamicUpdate, null);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = dynamicDataReport;
        this.aidToNavigationReport = null;
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.timeOfAtonUpdate = null;
        this.dynamicDataHistory = null;

        validateState();
    }

    /**
     * Create a new AisTrack using another track to build history.
     */
    AISTrack(AISTrack oldTrack, StaticDataReport staticDataReport, Instant timeOfStaticUpdate) {
        requireNonNull(staticDataReport);
        requireNonNull(timeOfStaticUpdate);
        validateArgs(staticDataReport, null,null, timeOfStaticUpdate, null, null);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = oldTrack.getDynamicDataReport();
        this.aidToNavigationReport = oldTrack.getAidToNavigationReport();
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = oldTrack.getTimeOfDynamicUpdate();
        this.timeOfAtonUpdate = oldTrack.getTimeOfAtonUpdate();
        validateState();

        dynamicDataHistory = oldTrack.dynamicDataHistory;
    }

    /**
     * Create a new AisTrack using another track to build history.
     */
    AISTrack(AISTrack oldTrack, DynamicDataReport dynamicDataReport, Instant timeOfDynamicUpdate) {
        requireNonNull(dynamicDataReport);
        requireNonNull(timeOfDynamicUpdate);
        validateArgs(null, dynamicDataReport, null, null, timeOfDynamicUpdate, null);

        this.staticDataReport = oldTrack.getStaticDataReport();
        this.dynamicDataReport = dynamicDataReport;
        this.aidToNavigationReport = oldTrack.getAidToNavigationReport();
        this.timeOfStaticUpdate = oldTrack.getTimeOfStaticUpdate();
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.timeOfAtonUpdate = oldTrack.getTimeOfAtonUpdate();
        this.dynamicDataHistory = copyDynamicHistory(oldTrack);

        validateState();
    }

    /**
     * Create a new AisTrack using another track to build history.
     */
    AISTrack(AISTrack oldTrack, StaticDataReport staticDataReport, DynamicDataReport dynamicDataReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate) {
        validateArgs(staticDataReport, dynamicDataReport, null, timeOfStaticUpdate, timeOfDynamicUpdate, null);

        this.staticDataReport = staticDataReport;
        this.dynamicDataReport = dynamicDataReport;
        this.aidToNavigationReport = oldTrack.getAidToNavigationReport();
        this.timeOfStaticUpdate = timeOfStaticUpdate;
        this.timeOfDynamicUpdate = timeOfDynamicUpdate;
        this.timeOfAtonUpdate = oldTrack.getTimeOfAtonUpdate();
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
        this.aidToNavigationReport = originalTrack.aidToNavigationReport;

        dynamicDataHistory = new ImmutableSortedMap.Builder<Instant, DynamicDataReport>(Comparator.<Instant>naturalOrder())
            .putAll(
                originalTrack.dynamicDataHistory.entrySet().stream()
                .filter(entry -> keepInstantPredicate.test(entry.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
            )
            .build();

        this.timeOfStaticUpdate = originalTrack.timeOfStaticUpdate;
        this.timeOfDynamicUpdate = dynamicDataHistory.lastKey();
        this.timeOfAtonUpdate = originalTrack.timeOfAtonUpdate;
    }

    private void validateArgs(StaticDataReport staticDataReport, DynamicDataReport dynamicDataReport, AidToNavigationReport aidToNavigationReport, Instant timeOfStaticUpdate, Instant timeOfDynamicUpdate, Instant timeOfAtonUpdate) {
        final long mmsiStatic = staticDataReport != null ? ((AISMessage) staticDataReport).getSourceMmsi().getMMSI() : -1;
        final long mmsiDynamic = dynamicDataReport != null ? ((AISMessage) dynamicDataReport).getSourceMmsi().getMMSI() : -1;
        final long mmsiAton = aidToNavigationReport != null ? ((AISMessage) aidToNavigationReport).getSourceMmsi().getMMSI() : -1;
        if (mmsiStatic == -1 && mmsiDynamic == -1 && mmsiAton == -1)
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
            if (timeOfAtonUpdate != null && !timeOfAtonUpdate.isAfter(timeOfLastUpdate))
                throw new IllegalArgumentException("Constructor arg timeOfAtonUpdate (" + timeOfAtonUpdate + ") must be after time of last update (" + timeOfLastUpdate + ")");
        }
    }

    private void validateState() {
        if (staticDataReport == null && dynamicDataReport == null && aidToNavigationReport == null)
            throw new IllegalArgumentException("A StaticDataReport or BasicDynamicDataReport or AidToNavigationReport must be provided");
        if (staticDataReport != null && timeOfStaticUpdate == null)
            throw new IllegalArgumentException("timeOfStaticUpdate cannot be null when staticDataReport is not");
        if (dynamicDataReport != null && timeOfDynamicUpdate == null)
            throw new IllegalArgumentException("timeOfDynamicUpdate cannot be null when dynamicDataReport is not");
        if (aidToNavigationReport != null && timeOfAtonUpdate == null)
            throw new IllegalArgumentException("timeOfAtonUpdate cannot be null when aidToNavigationReport is not");
        if (timeOfStaticUpdate != null && staticDataReport == null)
            throw new IllegalArgumentException("timeOfStaticUpdate cannot be provided when staticDataReport is not");
        if (timeOfDynamicUpdate != null && dynamicDataReport == null)
            throw new IllegalArgumentException("timeOfDynamicUpdate cannot be provided when dynamicDataReport is not");
        if (timeOfAtonUpdate != null && aidToNavigationReport == null)
            throw new IllegalArgumentException("timeOfAtonUpdate cannot be provided when aidToNavigationReport is not");
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
        result = 31*result + (aidToNavigationReport != null ? aidToNavigationReport.hashCode() : 0);
        result = 31*result + (timeOfStaticUpdate != null ? timeOfStaticUpdate.hashCode() : 0);
        result = 31*result + (timeOfDynamicUpdate != null ? timeOfDynamicUpdate.hashCode() : 0);
        return result;
    }

    public long getMmsi() {
        return dynamicDataReport != null ? ((AISMessage) dynamicDataReport).getSourceMmsi().getMMSI() :
                staticDataReport != null ? ((AISMessage) staticDataReport).getSourceMmsi().getMMSI() :
                ((AISMessage) aidToNavigationReport).getSourceMmsi().getMMSI();
    }

    public TransponderClass getTransponderClass() {
        return dynamicDataReport != null ? dynamicDataReport.getTransponderClass() : staticDataReport != null ? staticDataReport.getTransponderClass() : null;
    }

    public Instant getTimeOfLastUpdate() {
        if (timeOfStaticUpdate == null && timeOfAtonUpdate == null)
            return timeOfDynamicUpdate;
        else if (timeOfDynamicUpdate == null && timeOfAtonUpdate == null)
            return timeOfStaticUpdate;
        else if (timeOfStaticUpdate == null && timeOfDynamicUpdate == null)
            return timeOfAtonUpdate;

        else if (timeOfStaticUpdate != null && timeOfDynamicUpdate != null && timeOfAtonUpdate != null)
            return timeOfStaticUpdate.isBefore(timeOfDynamicUpdate) ?
                    timeOfDynamicUpdate.isBefore(timeOfAtonUpdate) ? timeOfAtonUpdate : timeOfDynamicUpdate :
                    timeOfStaticUpdate;
        else if (timeOfStaticUpdate != null && timeOfDynamicUpdate != null)
            return timeOfStaticUpdate.isBefore(timeOfDynamicUpdate) ? timeOfDynamicUpdate : timeOfStaticUpdate;
        else if (timeOfDynamicUpdate != null)
            return timeOfDynamicUpdate.isBefore(timeOfAtonUpdate) ? timeOfAtonUpdate : timeOfDynamicUpdate;
        else
            return timeOfStaticUpdate.isBefore(timeOfAtonUpdate) ? timeOfAtonUpdate : timeOfStaticUpdate;
    }

    public Instant getTimeOfStaticUpdate() {
        return timeOfStaticUpdate;
    }

    public Instant getTimeOfDynamicUpdate() {
        return timeOfDynamicUpdate;
    }

    public Instant getTimeOfAtonUpdate() {
        return timeOfAtonUpdate;
    }

    public StaticDataReport getStaticDataReport() {
        return staticDataReport;
    }

    public DynamicDataReport getDynamicDataReport() {
        return dynamicDataReport;
    }

    public AidToNavigationReport getAidToNavigationReport() {
        return aidToNavigationReport;
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
        return staticDataReport != null ? staticDataReport.getToBow() : aidToNavigationReport != null ? aidToNavigationReport.getToBow() : null;
    }

    public Integer getToStern()  {
        return staticDataReport != null ? staticDataReport.getToStern() : aidToNavigationReport != null ? aidToNavigationReport.getToStern() : null;
    }

    public Integer getToStarboard()  {
        return staticDataReport != null ? staticDataReport.getToStarboard() : aidToNavigationReport != null ? aidToNavigationReport.getToStarboard() : null;
    }

    public Integer getToPort()  {
        return staticDataReport != null ? staticDataReport.getToPort() : aidToNavigationReport != null ? aidToNavigationReport.getToPort() : null;
    }

    public Float getLatitude()  {
        return dynamicDataReport != null ? dynamicDataReport.getLatitude() : aidToNavigationReport != null ? aidToNavigationReport.getLatitude() : null;
    }

    public Float getLongitude()  {
        return dynamicDataReport != null ? dynamicDataReport.getLongitude() : aidToNavigationReport != null ? aidToNavigationReport.getLongitude() : null;
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
        return dynamicDataReport instanceof ExtendedDynamicDataReport ? ((ExtendedDynamicDataReport) dynamicDataReport).getSecond() :
                aidToNavigationReport != null ? aidToNavigationReport.getSecond() : null;
    }

    /* Return an immutable and sorted map of this track's dynamic history. */
    public ImmutableSortedMap<Instant, DynamicDataReport> getDynamicDataHistory() {
        return dynamicDataHistory == null ? ImmutableSortedMap.copyOf(Maps.newTreeMap()) : dynamicDataHistory;
    }

    private final StaticDataReport staticDataReport;
    private final DynamicDataReport dynamicDataReport;
    private final AidToNavigationReport aidToNavigationReport;
    private final Instant timeOfStaticUpdate;
    private final Instant timeOfDynamicUpdate;
    private final Instant timeOfAtonUpdate;

    /* Dynamic history of the track excluding the most recent, current value */
    private final ImmutableSortedMap<Instant, DynamicDataReport> dynamicDataHistory;
}
