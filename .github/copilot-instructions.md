# GitHub Copilot Instructions for AISutils

## Project Overview

AISutils is a Java library for processing AIS (Automatic Identification System) messages used in maritime navigation and safety. The library provides utilities for tracking vessels, filtering AIS messages, and managing vessel state across different message types.

## Key Components

### 1. AIS Tracker (`dk.tbsalling.ais.tracker`)
- **Purpose**: Maintains vessel state by consolidating information from different AIS message types
- **Main Class**: `AISTracker` - receives AIS messages and updates vessel tracks
- **Track Representation**: `AISTrack` - represents the complete state of a vessel (position, name, callsign, speed, etc.)
- **Event System**: Uses Google Guava EventBus for track lifecycle events (created, updated, deleted)
- **Thread Safety**: The tracker is thread-safe and can be queried from multiple threads

### 2. AIS Filter (`dk.tbsalling.ais.filter`)
- **Purpose**: Filters AIS messages based on various criteria
- **Implementation**: Uses Java `Predicate<AISMessage>` interface
- **Expression Filter**: Allows free-text filter expressions (e.g., "msgid=3", "lat > 55.0 and lng < 10.0")
- **Doublet Filter**: Removes duplicate messages within a time window
- **Filter Factory**: `FilterFactory` - creates filter instances

## Technology Stack

- **Language**: Java 21
- **Build Tool**: Maven
- **Key Dependencies**:
  - `aismessages` (3.3.1) - AIS message parsing
  - Google Guava (31.1-jre) - Event bus and collections
  - ANTLR4 (4.12.0) - Expression parser for filter grammar
- **Testing**: JUnit Jupiter 5.9.2

## Code Style and Conventions

### Naming Conventions
- Use `AISTracker` and `AISTrack` (not `AisTracker`/`AisTrack`) for main classes
- Package names use lowercase: `dk.tbsalling.ais.tracker`, `dk.tbsalling.ais.filter`
- Event classes follow pattern: `AisTrack[Event]Event` (e.g., `AisTrackCreatedEvent`)

### Design Patterns
- **Builder Pattern**: Not heavily used; prefer constructor-based initialization
- **Factory Pattern**: `FilterFactory` for creating filter instances
- **Observer Pattern**: EventBus for track lifecycle notifications
- **Immutability**: Prefer immutable collections (use Guava's `ImmutableSet`)

### Thread Safety
- Mark thread-safe classes with `@ThreadSafe` annotation
- Use `@GuardedBy` to document lock protection
- Use `ReentrantLock` for complex synchronization scenarios

## Building and Testing

### Build Commands
```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Package (creates jar with dependencies)
mvn package

# Install to local repository
mvn install
```

### ANTLR Grammar
- Grammar files located in `src/main/antlr4/dk/tbsalling/ais/filter/`
- ANTLR generates parser/lexer code during build
- Used for expression filter parsing

## Common Development Tasks

### Adding a New Filter
1. Implement `Predicate<AISMessage>`
2. Add factory method to `FilterFactory`
3. Write unit tests following existing filter test patterns
4. Update README with usage examples

### Adding Track Event Types
1. Create event class extending `AisTrackEvent`
2. Emit event from `TrackEventEmitter`
3. Document subscription pattern in README
4. Add demo usage to `EventDemoApp`

### Working with AIS Messages
- AIS messages are from the `aismessages` library
- Message types: 1-3 (position), 5 (static data), 18-19 (Class B), etc.
- Use message type checking: `message instanceof PositionReport`
- Extract data via message-specific methods

## Input/Output

### Input Format
- NMEA-formatted AIS messages
- Example: `!AIVDM,1,1,,A,15Mv5v?P00IS0J\`A86KTROvN0<5k,0*12`
- Read from `InputStream` via `AISInputStreamReader`

### Output Format
- `AISTrack` objects with vessel information
- Events posted to EventBus subscribers
- Supports querying tracker state at any time

## Testing Guidelines

### Unit Tests
- Located in `src/test/java/`
- Use JUnit assertions and annotations
- Mock or use test data files in `src/test/resources/`

### Test Data
- Sample NMEA messages in test resources
- Cover various message types and scenarios
- Include edge cases (missing data, invalid messages)

## Documentation

### Javadoc
- Document public APIs with comprehensive Javadoc
- Include `@param`, `@return`, `@throws` tags
- Provide usage examples in class-level Javadoc

### README Updates
- Update README.md when adding major features
- Include code examples for new functionality
- Maintain version information

## License

- Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported (CC BY-NC-SA 3.0)
- NOT FOR COMMERCIAL USE
- Contact required for commercial licensing

## Important Notes

1. **No commercial use** without proper licensing
2. Maintain compatibility with Java 21+
3. Prefer new Java language features over old ones
4. Keep dependencies up to date but test thoroughly
5. Follow existing code patterns and style
6. Update version in pom.xml when releasing
7. AIS message parsing is handled by `aismessages` library - don't reimplement
8. Use existing utility classes from Guava where applicable
