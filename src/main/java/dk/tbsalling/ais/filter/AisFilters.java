package dk.tbsalling.ais.filter;

import dk.tbsalling.aismessages.ais.messages.AISMessage;

import java.util.function.Predicate;

/**
 * Created by tbsalling on 03/02/15.
 */
public class AisFilters {

    Predicate<AISMessage> expressionFilter = {

        @Override
        public boolean test(AISMessage aisMessage) {
            return false;
        }
    }

}
