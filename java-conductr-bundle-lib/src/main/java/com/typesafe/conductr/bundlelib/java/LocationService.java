package com.typesafe.conductr.bundlelib.java;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.typesafe.conductr.lib.HttpPayload;
import com.typesafe.conductr.lib.java.ConnectionHandler;
import com.typesafe.conductr.lib.java.Tuple;

/**
 * A Location Service is used to look up services using the Typesafe ConductR Service Locator.
 */
public class LocationService {
    /**
     * Create the HttpPayload necessary to look up a service by name.
     * <p>
     * If the service is available and can be looked up the response for the HTTP request should be
     * 307 (Temporary Redirect), and the resulting URI to the service is in the "Location" header of the response.
     * A Cache-Control header may also be returned indicating the maxAge that the location should be cached for.
     * If the service can not be looked up the response should be 404 (Not Found).
     * All other response codes are considered illegal.
     *
     * @param serviceName The name of the service
     * @return Some HttpPayload describing how to do the service lookup or None if
     * this program is not running within ConductR
     */
    public static Optional<HttpPayload> createLookupPayload(String serviceName) throws MalformedURLException {
        return Optional.ofNullable(com.typesafe.conductr.bundlelib.LocationService.createLookupPayload(serviceName));
    }

    /**
     * A convenience function for [[createLookupPayload]] where the payload url is created when this bundle component
     * is running in the context of ConductR. If it is not then a fallback is returned.
     */
    public static URL getLookupUrl(String serviceName, URL fallback) throws MalformedURLException {
        return com.typesafe.conductr.bundlelib.LocationService.getLookupUrl(serviceName, fallback);
    }

    /**
     * Look up a service by service name using a cache. Service names correspond to those declared in a Bundle
     * component's endpoint data structure i.e. within a bundle's bundle.conf. If the bundle component
     * has not been started by ConductR then the fallback will be used.
     * <p>
     * The executor that the request is performed on is an implementation concern. More control over the executor
     * can be provided with the other flavour of this method.
     * <p>
     * Returns some URI representing the service or None if the service is not found.
     */
    public static CompletionStage<Optional<URI>> lookup(String serviceName, URI fallback, CacheLike cache) throws MalformedURLException {
        return lookup(serviceName, fallback, cache, ForkJoinPool.commonPool());
    }

    /**
     * As per its other form only that an executor can be provided explicitly.
     */
    public static CompletionStage<Optional<URI>> lookup(String serviceName, URI fallback, CacheLike cache, Executor executor) throws MalformedURLException {
        if (Env.isRunByConductR())
            return cache.getOrElseUpdate(serviceName, () -> {
                try {
                    return ConnectionHandler.withConnectedRequest(createLookupPayload(serviceName), LocationService::handleLookup, executor);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            });
        else
            return CompletableFuture.completedFuture(Optional.of(fallback));
    }

    private static final Pattern MAX_AGE_SECS_PATTERN = Pattern.compile(".*max-age=(\\d+).*");

    @SuppressWarnings("unchecked")
    private static Optional<Tuple<URI, Optional<Duration>>> handleLookup(Integer responseCode, Map<String, Optional<String>> headers) {
        switch (responseCode) {
            case 307:
                return headers.get("Location").map(l -> {
                    try {
                        URI location = new URI(l);
                        Optional<Duration> duration = Optional.ofNullable(headers.get("Cache-Control"))
                            .flatMap(sd -> sd.map(d -> {
                                Matcher m = MAX_AGE_SECS_PATTERN.matcher(d);
                                return (Optional<Duration>)(
                                    m.matches()?
                                        Optional.of(Duration.of(new Integer(m.group(1)), ChronoUnit.SECONDS)) :
                                        Optional.empty()
                                );
                            })).orElse(Optional.empty());
                        return new Tuple<>(location, duration);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
            case 404:
                return Optional.empty();
            default:
                throw new RuntimeException(new IOException("Illegal response code " + responseCode));
        }
    }
}