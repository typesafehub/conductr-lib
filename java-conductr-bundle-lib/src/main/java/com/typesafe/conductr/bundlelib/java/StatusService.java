package com.typesafe.conductr.bundlelib.java;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletionStage;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import com.typesafe.conductr.java.ConnectionHandler;
import com.typesafe.conductr.HttpPayload;
import com.typesafe.conductr.java.Unit;

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
public class StatusService {
    /**
     * Create the HttpPayload necessary to signal that a bundle has started.
     *
     * Any 2xx response code is considered a success. Any other response code is considered a failure.
     *
     * @return Some HttpPayload describing how to signal that a bundle has started or None if
     *         this program is not running within ConductR
     */
    public static Optional<HttpPayload> createSignalStartedPayload() throws IOException {
        return Optional.ofNullable(com.typesafe.conductr.bundlelib.StatusService.createSignalStartedPayload());
    }

    /**
     * Signal that the bundle has started or exit the JVM if it fails.
     *
     * This will exit the JVM if it fails with exit code 70 (EXIT_SOFTWARE, Internal Software Error,
     * as defined in BSD sysexits.h).
     *
     * The returned future will complete successfully if the ConductR acknowledges the start signal.
     * A Future of None will be returned if this program is not running in the context of ConductR.
     */
    public static CompletionStage<Optional<Unit>> signalStartedOrExit() throws IOException {
        return signalStartedOrExit(ForkJoinPool.commonPool());
    }

    /**
     * As above but with an explicit executor.
     */
    @SuppressWarnings("ConstantConditions")
    public static CompletionStage<Optional<Unit>> signalStartedOrExit(Executor executor) throws IOException {
        return signalStarted(executor).exceptionally(t -> {
            System.exit(70);
            return Optional.of(Unit.VALUE);
        });
    }

    /**
     * Signal that the bundle has started or throw IOException if it fails. If the bundle fails to communicate that
     * it has started it will eventually be killed by the ConductR.
     *
     * The returned future will complete successfully if the ConductR acknowledges the start signal.
     * A Future of None will be returned if this program is not running in the context of ConductR.
     */
    public static CompletionStage<Optional<Unit>> signalStarted() throws IOException {
        return signalStarted(ForkJoinPool.commonPool());
    }

    /**
     * As above but with an explicit executor.
     */
    public static CompletionStage<Optional<Unit>> signalStarted(Executor executor) throws IOException {
        return ConnectionHandler.withConnectedRequest(createSignalStartedPayload(), StatusService::handleSignal, executor);
    }

    private static Optional<Unit> handleSignal(Integer responseCode, Map<String, Optional<String>> headers) {
        if (responseCode < 200 || responseCode >= 300)
            throw new RuntimeException(new IOException("Illegal response code " + responseCode));
        return Optional.of(Unit.VALUE);
    }
}