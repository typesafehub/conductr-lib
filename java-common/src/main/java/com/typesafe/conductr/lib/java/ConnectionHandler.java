package com.typesafe.conductr.lib.java;

import com.typesafe.conductr.lib.HttpPayload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.typesafe.conductr.lib.java.ManagedBlocker.*;

/**
 * Connection handlers provide the means to establish a connection, issue a request and then finalize
 * the connection
 */
public class ConnectionHandler {

    protected final static String USER_AGENT = "TypesafeConductRLib";

    /**
     * Make a request to a ConductR service given a payload. Returns a future of an option. If there is some response
     * then there will be a result, otherwise empty indicates that this program is not running in the context
     * of ConductR.
     */
    public static <T> CompletionStage<Optional<T>> withConnectedRequest(
            Optional<HttpPayload> payload, BiFunction<Integer, Map<String, Optional<String>>, Optional<T>> op, Executor executor) {
        return payload
            .map(p -> CompletableFuture.supplyAsync(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) p.getUrl().openConnection();
                    connection.setRequestMethod(p.getRequestMethod());
                    connection.setInstanceFollowRedirects(p.getFollowRedirects());
                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    blocking(() -> {
                        try {
                            connection.connect();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                    Map<String, Optional<String>> headers =
                            connection.getHeaderFields()
                                    .entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().reduce((a, b) -> b)));
                    return op.apply(connection.getResponseCode(), headers);

                } catch (IOException e) {
                    throw new UncheckedIOException("Connection failed for " + p, e);
                } catch (InterruptedException e) {
                    throw new UncheckedIOException("Interrupt", new IOException("Connection failed for " + p));
                }
            }, executor))
            .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }
}
