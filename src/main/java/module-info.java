module dk.tbsalling.ais.utils {
    requires com.google.common;
    requires aismessages;
    requires jsr305;
    requires org.antlr.antlr4.runtime;

    exports dk.tbsalling.ais.filter;
    exports dk.tbsalling.ais.tracker.events;
    exports dk.tbsalling.ais.tracker;

    opens dk.tbsalling.ais.tracker to com.google.common;
}
