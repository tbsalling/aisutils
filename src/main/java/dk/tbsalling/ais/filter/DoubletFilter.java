/*
 * AISUtils
 * - a java-based library for processing of AIS messages received from digital
 * VHF radio traffic related to maritime navigation and safety in compliance with ITU 1371.
 *
 * (C) Copyright 2011- by S-Consult ApS, DK31327490, http://s-consult.dk, Denmark.
 *
 * Released under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * For details of this license see the nearby LICENCE-full file, visit http://creativecommons.org/licenses/by-nc-sa/3.0/
 * or send a letter to Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 *
 * NOT FOR COMMERCIAL USE!
 * Contact sales@s-consult.dk to obtain a commercially licensed version of this software.
 *
 */

package dk.tbsalling.ais.filter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.tbsalling.aismessages.ais.messages.AISMessage;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * DoubletFilter is a filter which removes duplicate AISmessages. It is intended
 * for use in systems where several AIS receivers have overlapping geographical
 * receival area. In this a single transmission from a vessel can be picked up by
 * more than one receiver and thus cause doublets in the message stream.
 *
 * @author Thomas Borg Salling
 * @see FilterFactory
 */
class DoubletFilter implements Predicate<AISMessage> {

    /** Google's caching mechanism is not perfect for this - but adequate. A very few dupes may slip through. */
    private final Cache<BigInteger, Optional<AISMessage>> cache;

    /** Create a doublet filter with default window settings */
    DoubletFilter() {
        this(15, TimeUnit.SECONDS);
    }

    DoubletFilter(long duration, TimeUnit unit) {
        if (duration <= 0)
            throw new IllegalArgumentException("duration must be positive.");
        requireNonNull(unit);

        cache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(duration, unit)
            .maximumSize(100000)
            .build();
    }

    /**
     * Test an incoming aisMessage against the sliding time window. If the
     * message is already there it is rejected.
     *
     * @param aisMessage
     * @return true if the aisMessage is not a doublet.
     */
    @Override
    public boolean test(AISMessage aisMessage) {
        boolean filterPassed = true;

        try {
            BigInteger digest = new BigInteger(aisMessage.digest());

            if (cache.getIfPresent(digest) != null)
                filterPassed = false;
            else
                cache.put(digest, Optional.empty());
        } catch (NoSuchAlgorithmException e) {
        }

        return filterPassed;
    }

}
