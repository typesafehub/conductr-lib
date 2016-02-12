package com.typesafe.conductr.bundlelib.java;

import com.typesafe.conductr.lib.java.Tuple;

import java.net.URI;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * A cache like class as a default implementation.
 */
public class LocationCache implements CacheLike {

    private final ConcurrentMap<String, CompletionStage<Optional<URI>>> cache = new ConcurrentHashMap<>();

    private final Timer reaperTimer = new Timer();

    @Override
    public CompletionStage<Optional<URI>> getOrElseUpdate(String serviceName, Supplier<CompletionStage<Optional<Tuple<URI, Optional<Duration>>>>> op) {
        return cache.computeIfAbsent(serviceName, sn ->
            op
                .get()
                .whenCompleteAsync((locationAndMaxAge, e) -> {
                    try {
                        Duration maxAge = locationAndMaxAge.get()._2.get();
                        reaperTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                cache.remove(sn);
                            }
                        }, maxAge.toMillis());
                    } catch (NoSuchElementException nse) {
                        cache.remove(sn);
                    }
                })
                .thenApply(o -> o.map(o1 -> Optional.of(o1._1)).orElse(Optional.empty())));
    }

    @Override
    public Optional<CompletionStage<Optional<URI>>> remove(String serviceName) {
        return Optional.ofNullable(cache.remove(serviceName));
    }
}
