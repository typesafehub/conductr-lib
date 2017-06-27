package com.typesafe.conductr.bundlelib.akka;

import akka.actor.ActorSystem;
import akka.japi.Option;
import akka.util.Timeout;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import com.typesafe.conductr.lib.akka.ConnectionContext;
import static org.junit.Assert.assertEquals;

public class StatusServiceTest extends JUnitSuite {
    @Test
    public void return_None_when_running_in_development_mode() throws Exception {
        ActorSystem system = ActorSystem.create("MySystem");

        ConnectionContext cc = ConnectionContext.create(system);
        Timeout timeout = new Timeout(Duration.create(5, "seconds"));

        assertEquals(
            Await.result(StatusService.getInstance().signalStartedOrExitWithContext(cc), timeout.duration()),
            Option.none()
        );

        assertEquals(
            Await.result(StatusService.getInstance().signalStartedWithContext(cc), timeout.duration()),
            Option.none()
        );
    }
}
