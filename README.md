# AISutils

AISutils is a valuable package of utility software to process AIS messages.

Currently it consist of a Tracker which can maintain the state of individual vessels between AIS messages received over time. Soon there will also be
free-text filter expressions for AIS messages, event triggering, message archiving in Big Data stores, export of KML-files for Google Earth, and more.

## AIS Tracker

### The problem
Different types of AIS messages carry different types of information about the same physical
vessel. For instance a vessel's name is carried in messages of type 5, whereas its position
can be carried in messages of type 1.

In order to collect the complete information about a vessel, information split across different
AIS messages must therefore be collected and consolidated. This is what the tracker does. Feed it
with a AIS messages in NMEA format - and you will get Tracks in return.

### Demo
So, consider that you have a finite or continuous stream of AIS data in NMEA format.
Something like this:

```
    ...
    !AIVDM,1,1,,A,15Mv5v?P00IS0J`A86KTROvN0<5k,0*12
    !AIVDM,1,1,,A,15Mwd<PP00ISfGpA7jBr??vP0<3:,0*04
    !AIVDM,2,1,4,B,55MwW7P00001L@?;GS0<51B08Thj0TdpE800000P0hD556IE07RlSm6P0000,0*0B
    !AIVDM,2,2,4,B,00000000000,2*23
    !AIVDM,1,1,,A,15N7th0P00ISsi4A5I?:fgvP2<40,0*06
    !AIVDM,1,1,,A,15NIEcP000ISrjPA8tEIBq<P089=,0*63
    !AIVDM,1,1,,B,15MuS0PP00IS00HA8gEtSgvN0<3U,0*61
    !AIVDM,1,1,,A,16K8Au@000awU:FD9l?JcnrP289D,0*15
    !AIVDM,1,1,,A,15MwP=0000ISS5lA7=qJ57jL05Ip,0*0D
    ...
```

Then you feed it into the tracker from a regular Java InputStream:

``` java

    public class DemoApp {

        public static void main(String [] args) throws IOException {
            InputStream inputStream = ...

            // Start tracking
            AisTracker tracker = new AisTracker();
            tracker.update(inputStream);
            ...
        }

    }
```

As the tracker is reading the stream, it builds and maintains the state of each track. Information about the tracker and each
track can be read out at any time from any thread:

``` java

    public class DemoApp {

        public printSomething {
            // Get stats from tracker
            System.out.println("No. of current tracks in tracker: " + tracker.getNumberOfAisTracks());

            // Get all tracks from tracker
            Set<AisTrack> tracks = tracker.getAisTracks();
            AisTrack aRandomTrack = tracks.iterator().next();
            System.out.println("A random MMSI " + aRandomTrack.getMmsi());

            // Get a specific track from tracker
            AisTrack track = tracker.getAisTrack(219997000);
            System.out.println(
                "Tracking vessel with name: " + track.getShipName() +
                ", callsign: " + track.getCallsign() +
                " currently cruising at " + track.getSpeedOverGround() + " knots."
            );
        }

    }
```

In addition to this, it is possible register event listeners, so that your application is instantly notified
whenever a track is created, updated, or deleted. The mechanism works like this:

``` java

    import com.google.common.eventbus.Subscribe;
    import dk.tbsalling.ais.tracker.AisTrack;
    import dk.tbsalling.ais.tracker.AisTracker;
    import dk.tbsalling.ais.tracker.events.AisTrackCreatedEvent;
    import dk.tbsalling.ais.tracker.events.AisTrackDeletedEvent;
    import dk.tbsalling.ais.tracker.events.AisTrackDynamicsUpdatedEvent;
    import dk.tbsalling.ais.tracker.events.AisTrackUpdatedEvent;
    ...

    public class EventDemoApp {

        public static void main(String [] args) throws IOException, InterruptedException {

            // Create the tracker
            AisTracker tracker = new AisTracker();

            // Register event listeners
            tracker.registerSubscriber(new Object() {
                @Subscribe
                public void handleEvent(AisTrackCreatedEvent event) {
                    System.out.println("CREATED: " + event.getAisTrack());
                }

                @Subscribe
                public void handleEvent(AisTrackUpdatedEvent event) {
                    System.out.println("UPDATED: " + event.getAisTrack());
                }

                @Subscribe
                public void handleEvent(AisTrackDynamicsUpdatedEvent event) {
                    System.out.println("UPDATED DYNAMICS: " + event.getAisTrack());
                }

                @Subscribe
                public void handleEvent(AisTrackDeletedEvent event) {
                    System.out.println("DELETED: " + event.getAisTrack());
                }
            });

            // Feed AIS Data into tracker
            InputStream aisInputStream = ...;
            tracker.update(aisInputStream);
        }
    }
```

As the input stream is read, this will produce output like this:

```
CREATED: AisTrack{mmsi=258315000, transponderClass=A, callsign='LFNA', shipName='FALKVIK', shipType=CargoNoAdditionalInfo, toBow=40, toStern=10, toStarboard=5, toPort=4, latitude=null, longitude=null, speedOverGround=null, courseOverGround=null, trueHeading=null, second=null}
CREATED: AisTrack{mmsi=440009390, transponderClass=A, callsign='null', shipName='null', shipType=null, toBow=null, toStern=null, toStarboard=null, toPort=null, latitude=37.452908, longitude=126.611946, speedOverGround=0.0, courseOverGround=55.2, trueHeading=511, second=15}
UPDATED: AisTrack{mmsi=366970360, transponderClass=A, callsign='null', shipName='null', shipType=null, toBow=null, toStern=null, toStarboard=null, toPort=null, latitude=29.93085, longitude=-90.2198, speedOverGround=0.0, courseOverGround=116.1, trueHeading=511, second=15}
UPDATED DYNAMICS: AisTrack{mmsi=366970360, transponderClass=A, callsign='null', shipName='null', shipType=null, toBow=null, toStern=null, toStarboard=null, toPort=null, latitude=29.93085, longitude=-90.2198, speedOverGround=0.0, courseOverGround=116.1, trueHeading=511, second=15}
CREATED: AisTrack{mmsi=244670316, transponderClass=A, callsign='null', shipName='null', shipType=null, toBow=null, toStern=null, toStarboard=null, toPort=null, latitude=51.89475, longitude=4.379285, speedOverGround=0.0, courseOverGround=70.6, trueHeading=511, second=14}
CREATED: AisTrack{mmsi=205264890, transponderClass=A, callsign='null', shipName='null', shipType=null, toBow=null, toStern=null, toStarboard=null, toPort=null, latitude=51.309635, longitude=4.3227935, speedOverGround=0.0, courseOverGround=338.0, trueHeading=339, second=15}
...
```

## AIS Filter

The AIS filter package offers a number of different filters, which can be used to filter AIS messages in a stream of messages.
All filters are implemented as Java Predicates, like this:

```
Predicate<AISMessage> filter = ...;
```

Filters can be used as-is/standalone by applying them to AISMessages, or they can supplied to e.g. the AISTracker 
to filter the AISMessages used to update tracks by e.g. geography, ship type or to avoid duplicate messages. An AISTracker
using a filter, can be instantiated like this:

```
Predicate<AISMessage> filter = ...;
AisTracker tracker = new AisTracker(filter);
```

Filters can be chained by applying the 'and', 'or' and 'negate' features of the Java Predicate<> interface.

### Expression filter
The expression filter class allows the programmer (or end user) to express a filter expression in free text, 
and apply this against AISMessages.

The filter is instantiated from the FilterFactory like this:

```
Predicate<AISMessage> expressionFilter = FilterFactory.newExpressionFilter("msgid=3");
```

This expressionFilter will return true only for AISMessages of type 3.

The expression grammar supports a number of free text expressions. Other possible expressions are:

```
FilterFactory.newExpressionFilter("msgid=3 or msgid=5");
FilterFactory.newExpressionFilter("msgid in (1, 2, 3, 5)");
FilterFactory.newExpressionFilter("msgid not in (1, 2, 3, 5");
FilterFactory.newExpressionFilter("mmsi > 100000000 and mmsi < 219000000 and msgid in (1, 2, 3, 5)");
FilterFactory.newExpressionFilter("sog > 5.0");
FilterFactory.newExpressionFilter("cog < 180.0");
FilterFactory.newExpressionFilter("lat > 55.0 and lat < 55.5 and lng > 10.0 and lng < 10.5");
etc.
```

### Doublet filter
In some systems it is normal to observe duplicate AIS messages in the message stream. This
happens for instance in systems where several AIS receivers have overlapping geographical
receival areas. In this case a transmission from a vessel can be picked up by more than one 
receiver and thus cause doublets in the message stream.

The doublet filter works by appying a sliding time window over the message stream. If a message
occurs more than one time inside a time window of, say, 15 seconds, then only the first message
will pass through the filter.

Duplicate filters are also instantiated through the FilterFactory class, like this:

```
Predicate<AISMessage> doubletFilter = FilterFactory.newDoubletFilter(15, TimeUnit.SECONDS);
```

## How to get, build and include AISutils in your project
There's no formal release yet. But you can download AISutils from Github and and build it using maven:

```
$ git clone git@github.com:tbsalling/aisutils.git
...
$ cd aisutils/
$ mvn install
```

Then add this to the pom.xml file of your own Java Maven project:

```
<dependency>
    <groupId>dk.tbsalling</groupId>
    <artifactId>aisutils</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Roadmap
-------
More advanced free-text filter expressions for AIS messages
event triggering
message archiving in Big Data stores
export of KML-files for Google Earth