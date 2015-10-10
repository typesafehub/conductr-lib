package com.typesafe.conductr.bundlelib.java;

import com.typesafe.conductr.java.Tuple;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A structure that describes what we require from a cache specifically for locations in relation to service names.
 * This interface describes an expiring entry cache inspired by Spray Cache in its elements being captured as Futures, thus
 * being able to cope with the thundering herds issue:
 * http://ehcache.org/documentation/2.8/recipes/thunderingherd.
 *
 * Entries that provide a max age duration are scheduled to be removed at that time. The
 * expectation is that this cache is used with such durations. Where there is no duration
 * (this should be rare) then the cache entry is quickly removed after it has been determined.
 * This removal also occurs when the entry cannot be established successfully
 */
public interface CacheLike {
    /**
     * Retrieve a service uri from the cache. If the service is not cached then perform an operation to obtain its
     * address along with an optional TTL in the cache.
     */
    CompletionStage<Optional<URI>> getOrElseUpdate(String serviceName, Supplier<CompletionStage<Optional<Tuple<URI, Optional<Duration>>>>> op);

    /**
     * Remove a service uri from the cache if it exists. The operation is benign if there is no entry.
     */
    Optional<CompletionStage<Optional<URI>>> remove(String serviceName);
}
