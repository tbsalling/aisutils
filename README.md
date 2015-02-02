# AISutils

AISutils is a valuable package of utility software to process AIS messages.

Currently it consist of a Tracker which can maintain the state of individual vessels between AIS messages received over time. Soon there will also be
free-text filter expressions for AIS messages, event triggering, message archiving in Big Data stores, export of KML-files for Google Earth, and more.

## AIS Tracker

### The problem
Some types of AIS messages each carry a fraction of information about a single physical track.

### Demo
Consider that you have a finite or continuous stream of AIS data in NMEA format. Something like this:

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

The NMEA stream must feed NMEA string like this:

```

    !AIVDM,1,1,,A,15Mv5v?P00IS0J`A86KTROvN0<5k,0*12
    !AIVDM,1,1,,A,15Mwd<PP00ISfGpA7jBr??vP0<3:,0*04
    !AIVDM,2,1,4,B,55MwW7P00001L@?;GS0<51B08Thj0TdpE800000P0hD556IE07RlSm6P0000,0*0B
    !AIVDM,2,2,4,B,00000000000,2*23
    !AIVDM,1,1,,A,15N7th0P00ISsi4A5I?:fgvP2<40,0*06
    !AIVDM,1,1,,A,15NIEcP000ISrjPA8tEIBq<P089=,0*63
    !AIVDM,1,1,,B,15MuS0PP00IS00HA8gEtSgvN0<3U,0*61
    !AIVDM,1,1,,A,16K8Au@000awU:FD9l?JcnrP289D,0*15
    !AIVDM,1,1,,A,15MwP=0000ISS5lA7=qJ57jL05Ip,0*0D
    !AIVDM,1,1,,B,15NF:5g007qTHIhA:wWrK2nN089A,0*3A
    !AIVDM,1,1,,A,15N8<TPP00IRlor@ktvdDgvN0D56,0*77
    !AIVDM,1,1,,B,15MmOb?P00ISh=hA8e5:rgvP25Ip,0*7A
    !AIVDM,1,1,,A,15N0vCPP00ITM6>@rCmcr?vN00SB,0*2A
    !AIVDM,1,1,,A,15NF8f0P00ISHSt@nv3c3OvP2D5>,0*4E
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
whenever a track is created, updated, or deleted.

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
free-text filter expressions for AIS messages
event triggering
message archiving in Big Data stores
export of KML-files for Google Earth