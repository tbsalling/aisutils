# Release Notes

## [1.1.5] – Unreleased

### New Features
- **KML Export**: New `KMLExporter` class allows exporting tracked vessel positions to KML format for visualization in Google Earth and other compatible applications. Supports exporting all tracks or a single track.

### Improvements
- **Java 21**: Updated minimum Java version requirement to Java 21, enabling use of modern language features.
- **Updated dependencies**: Upgraded `aismessages` to 4.1.2 and Guava to 32.0.0-jre.
- **Comprehensive test coverage**: Added unit tests using the Arrange-Act-Assert pattern with Mockito for mocked dependencies.

### Bug Fixes
- Removed Java module descriptor (`module-info.java`) to resolve compatibility issues with downstream projects.

---

## [1.1.4] – 2023-03-08

### Improvements
- Build tooling maintenance release.

---

## [1.1.3] – 2023-03-08

### Improvements
- Added NMEA sample data files for testing and development use.
- Restored Javadoc generation (reverted earlier skip) with `failOnError=false` to allow clean builds.
- Updated Maven release plugin to 3.0.0-M7.

---

## [1.1.2] – 2023-03-08

### Improvements
- **Removed logging dependencies**: Eliminated the `log4j` and `slf4j` dependencies. The library now uses no external logging framework, reducing dependency footprint and avoiding log4j vulnerability exposure.
- **Java module support**: Added `module-info.java` to make AISutils a proper Java module.
- **JUnit 5**: Migrated test suite from JUnit 4 to JUnit 5 (Jupiter).
- **Updated `aismessages`** to version 3.3.1.
- Resolved deprecation warnings across the codebase.

---

## [1.1.1] – 2022-01-29

### Bug Fixes
- Fixed `NullPointerException` errors that occurred when processing AtoN (Aid to Navigation) reports with missing optional fields.

---

## [1.1.0] – 2022-01-25

### New Features
- **AtoN tracking**: The `AISTracker` now handles AIS message type 21 (Aid to Navigation Reports). AtoN stations are tracked alongside vessel tracks.

### Improvements
- Updated `aismessages` library to 3.2.3.
- Updated all dependencies to current stable versions.

### Security
- Updated `log4j` to 2.17.1 to address CVE-2021-44228 (Log4Shell) and follow-on security advisories (CVE-2021-45046, CVE-2021-45105, CVE-2021-44832).

---

## [1.0.0] – 2018-08-01

### Initial Release

First public release of AISutils, a Java library for processing AIS (Automatic Identification System) messages.

#### AIS Tracker
- **`AISTracker`**: Consolidates information from multiple AIS message types (types 1, 2, 3, 5, 18, 19 and more) to build and maintain complete vessel state (`AISTrack`) over time.
- Tracks vessel name, callsign, MMSI, ship type, position, speed over ground (SOG), course over ground (COG), true heading, and vessel dimensions.
- Thread-safe: tracker state can be queried from multiple threads simultaneously.
- **Track lifecycle events** via Google Guava `EventBus`:
  - `AisTrackCreatedEvent` – fired when a new vessel is first seen.
  - `AisTrackUpdatedEvent` – fired when any track data is updated.
  - `AisTrackDynamicsUpdatedEvent` – fired when position or motion data changes.
  - `AisTrackDeletedEvent` – fired when a stale track is removed.
- Automatic pruning of stale tracks at configurable intervals.

#### AIS Filter
- **Expression Filter**: Free-text filter expressions evaluated against AIS messages. Examples:
  - `msgid=3`
  - `msgid in (1, 2, 3, 5)`
  - `mmsi > 100000000 and mmsi < 219000000`
  - `sog > 5.0`
  - `lat > 55.0 and lat < 55.5 and lng > 10.0 and lng < 10.5`
- **Doublet Filter**: Removes duplicate AIS messages within a configurable sliding time window (e.g. 15 seconds), useful for multi-receiver deployments with overlapping coverage areas.
- All filters implement `Predicate<AISMessage>` and can be composed using `and()`, `or()`, and `negate()`.
- Filters can be supplied directly to `AISTracker` to pre-filter incoming messages.
- `FilterFactory` provides a single entry point for creating filter instances.

#### Availability
- Published to [Maven Central](https://mvnrepository.com/artifact/dk.tbsalling/aisutils).
