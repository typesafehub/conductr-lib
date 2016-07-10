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

    private final ConcurrentMap<String, CompletionStage<Optional<Tuple<URI, Optional<Duration>>>>> cache = new ConcurrentHashMap<>();

    private final Timer reaperTimer = new Timer();

    @Override
    public CompletionStage<Optional<URI>> getOrElseUpdate(String serviceName, Supplier<CompletionStage<Optional<Tuple<URI, Optional<Duration>>>>> op) {
        return cache
                .computeIfAbsent(serviceName, sn -> op.get())
                .whenCompleteAsync((result, error) -> {
                    /*
                      IMPORTANT: always check the presence of error before result.
                      If error is present, and result is accessed (e.g. calling `isPresent()`), the whole future will
                      fail with an exception.
                     */
                    if (error != null || !result.isPresent())
                        cache.remove(serviceName);
                    else if (result.isPresent()) {
                        if (result.get()._2.isPresent())
                            reaperTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    cache.remove(serviceName);
                                }
                            }, result.get()._2.get().toMillis());
                        else
                            cache.remove(serviceName);
                    }
                })
                .thenApply(r -> r.map(t -> t._1));
    }

    @Override
    public Optional<CompletionStage<Optional<URI>>> remove(String serviceName) {
        return Optional.ofNullable(
                cache.remove(serviceName)
                    .thenApply(r -> r.map(t -> t._1))
        );
    }
}
