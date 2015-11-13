package dk.tbsalling.ais.tracker.demo;

import com.google.common.eventbus.Subscribe;
import dk.tbsalling.ais.tracker.AISTracker;
import dk.tbsalling.ais.tracker.events.AisTrackCreatedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackDeletedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackDynamicsUpdatedEvent;
import dk.tbsalling.ais.tracker.events.AisTrackUpdatedEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class EventDemoApp {

    public static void main(String [] args) throws IOException, InterruptedException {

        // Create the tracker
        AISTracker tracker = new AISTracker();

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

        // Read AIS data forever
        do {
            tracker.update(new ByteArrayInputStream(nmea.getBytes()));
            Thread.sleep(1000);
        } while (true);

    }

    private static String nmea =
        "!AIVDM,2,1,5,B,53AkSB02=:9TuaaR2210uDj0htELDptE8r22221J40=5566d0822DU4j0C4p,0*07\n" +
        "!AIVDM,2,2,5,B,88888888880,2*22\n" +
        "!AIVDM,1,1,,A,15Mv5v?P00IS0J`A86KTROvN0<5k,0*12\n" +
        "!AIVDM,1,1,,A,15Mwd<PP00ISfGpA7jBr??vP0<3:,0*04\n" +
        "!AIVDM,1,1,,A,13HOI:0P0000VOHLCnHQKwvL05Ip,0*23\n" +
        "!AIVDM,1,1,,A,133sVfPP00PD>hRMDH@jNOvN20S8,0*7F\n" +
        "!AIVDM,1,1,,B,100h00PP0@PHFV`Mg5gTH?vNPUIp,0*3B\n" +
        "!AIVDM,1,1,,B,13eaJF0P00Qd388Eew6aagvH85Ip,0*45\n" +
        "!AIVDM,1,1,,A,14eGrSPP00ncMJTO5C6aBwvP2D0?,0*7A\n" +
        "!AIVDM,1,1,,A,15MrVH0000KH<:V:NtBLoqFP2H9:,0*2F\n" +
        "!AIVDM,1,1,,A,15N9NLPP01IS<RFF7fLVmgvN00Rv,0*7F\n" +
        "!AIVDM,1,1,,A,133w;`PP00PCqghMcqNqdOvPR5Ip,0*65\n" +
        "!AIVDM,1,1,,B,35Mtp?0016J5ohD?ofRWSF2R0000,0*28\n" +
        "!AIVDM,1,1,,A,133REv0P00P=K?TMDH6P0?vN289>,0*46\n" +
        "!AIVDM,2,1,4,B,55MwW7P00001L@?;GS0<51B08Thj0TdpE800000P0hD556IE07RlSm6P0000,0*0B\n" +
        "!AIVDM,2,2,4,B,00000000000,2*23\n" +
        "!AIVDM,1,1,,B,139eb:PP00PIHDNMdd6@0?vN2D2s,0*43\n" +
        "!AIVDM,1,1,,B,33aDqfhP00PD2OnMDdF@QOvN205A,0*13\n" +
        "!AIVDM,1,1,,A,33AkSB0PAKPhQ@dPo@3BiQsP011Q,0*4E\n" +
        "!AIVDM,1,1,,B,B43JRq00LhTWc5VejDI>wwWUoP06,0*29\n" +
        "!AIVDM,1,1,,B,133hGvP0000CjLHMG0u==:VN05Ip,0*61\n" +
        "!AIVDM,1,1,,A,13aEOK?P00PD2wVMdLDRhgvL289?,0*26\n" +
        "!AIVDM,1,1,,B,16S`2cPP00a3UF6EKT@2:?vOr0S2,0*00\n" +
        "!AIVDM,2,1,9,B,53nFBv01SJ<thHp6220H4heHTf2222222222221?50:454o<`9QSlUDp,0*09\n" +
        "!AIVDM,2,2,9,B,888888888888880,2*2E\n" +
        "!AIVDM,1,1,,A,13AwPr00000pFa0P7InJL5JP2<0I,0*79\n" +
        "!AIVDM,1,1,,A,14eGKMhP00rkraHJPivPFwvL0<0<,0*23\n" +
        "!AIVDM,1,1,,B,13P:`4hP00OwbPRMN8p7ggvN0<0h,0*69\n" +
        "!AIVDM,1,1,,A,16:=?;0P00`SstvFnFbeGH6L088h,0*44\n" +
        "!AIVDM,1,1,,A,16`l:v8P0W8Vw>fDVB0t8OvJ0H;9,0*0A\n" +
        "!AIVDM,1,1,,A,169a:nP01g`hm4pB7:E0;@0L088i,0*5E\n" +
        "!AIVDM,1,1,,A,169F<h0P1S8hsm0B:H9o4gvN2@8o,0*5E\n" +
        "!AIVDM,1,1,,A,139f0`0P00PFDVvMag8a`gvP20T;,0*67\n" +
        "!AIVDM,1,1,,B,17u>=L001KR><?EfhW37iVFL05Ip,0*1D\n" +
        "!AIVDM,1,1,,A,16:>Pv002B8hjC6AjP9SCBNN05Ip,0*10\n" +
        "!AIVDM,1,1,,B,B6:io8@0=21k=`3C:eDJSww4SP00,0*68\n" +
        "!AIVDM,1,1,,B,36:RS:001?87bnt=:rq68TnN00nh,0*20\n" +
        "!AIVDM,2,1,6,B,56:fS:D0000000000008v0<QD4r0`T4v3400000t0`D147?ps1P00000,0*3D\n" +
        "!AIVDM,2,2,6,B,000000000000008,2*29\n" +
        "!AIVDM,1,1,,A,369AM`1P028d;40Aohk1EgvN2000,0*72\n" +
        "!AIVDM,1,1,,B,16:fRwOP1=87S3R=<JbLMwvL0<3v,0*59\n";
}
