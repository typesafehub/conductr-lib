package com.typesafe.conductr.bundlelib.play;

import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import com.typesafe.conductr.bundlelib.scala.LocationCache;
import com.typesafe.conductr.lib.play.ConnectionContext;
import org.junit.Test;
import play.libs.concurrent.HttpExecution;
import play.test.WithApplication;
import scala.concurrent.duration.Duration;
import java.util.Optional;
import java.net.URI;

import static org.junit.Assert.assertEquals;

public class LocationServiceTest extends WithApplication {
    @Test
    public void return_None_when_running_in_development_mode() throws Exception {
        URI fallback = new URI("");
        LocationCache cache = new LocationCache();
        ConnectionContext cc = ConnectionContext.create(HttpExecution.defaultContext());
        Timeout timeout = new Timeout(Duration.create(5, "seconds"));

        assertEquals(
            LocationService.getInstance().lookupWithContext("/whatever", fallback, cache, cc).toCompletableFuture().get(timeout.duration().toMillis(), TimeUnit.MILLISECONDS),
            Optional.ofNullable(fallback)
        );
    }
}
