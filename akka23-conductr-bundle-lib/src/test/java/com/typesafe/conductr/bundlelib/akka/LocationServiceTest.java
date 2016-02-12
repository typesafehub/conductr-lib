package com.typesafe.conductr.bundlelib.akka;

import akka.actor.ActorSystem;
import akka.japi.Option;
import akka.util.Timeout;
import com.typesafe.conductr.bundlelib.scala.LocationCache;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import com.typesafe.conductr.lib.akka.ConnectionContext;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class LocationServiceTest extends JUnitSuite {
    @Test
    public void return_the_fallback_when_running_in_development_mode() throws Exception {
        ActorSystem system = ActorSystem.create("MySystem");

        URI fallback = new URI("/fallback");
        LocationCache cache = new LocationCache();
        ConnectionContext cc = ConnectionContext.create(system);
        Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        assertEquals(
            Await.result(LocationService.getInstance().lookupWithContext("/whatever", fallback, cache, cc), timeout.duration()),
            Option.some(fallback)
        );
    }
}
