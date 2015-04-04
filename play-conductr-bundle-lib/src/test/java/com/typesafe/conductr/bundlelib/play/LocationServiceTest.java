package com.typesafe.conductr.bundlelib.play;

import akka.util.Timeout;
import com.typesafe.conductr.bundlelib.scala.LocationCache;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import play.libs.F;
import play.libs.HttpExecution;
import scala.concurrent.duration.Duration;

import static org.junit.Assert.assertEquals;

public class LocationServiceTest extends JUnitSuite {
    @Test
    public void return_None_when_running_in_development_mode() throws Exception {
        ConnectionContext cc = ConnectionContext.create(HttpExecution.defaultContext());
        LocationCache cache = new LocationCache();
        Timeout timeout = new Timeout(Duration.create(5, "seconds"));

        assertEquals(
            LocationService.getInstance().lookupWithContext("/whatever", cc, cache).get(timeout.duration().toMillis()),
            F.None()
        );
    }
}
