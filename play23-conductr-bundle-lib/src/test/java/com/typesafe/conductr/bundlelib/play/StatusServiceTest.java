package com.typesafe.conductr.bundlelib.play;

import akka.util.Timeout;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import play.libs.F;
import play.libs.HttpExecution;
import scala.concurrent.duration.Duration;
import com.typesafe.conductr.lib.play.ConnectionContext;

import static org.junit.Assert.assertEquals;

public class StatusServiceTest extends JUnitSuite {
    @Test
    public void return_None_when_running_in_development_mode() throws Exception {
        ConnectionContext cc = ConnectionContext.create(HttpExecution.defaultContext());
        Timeout timeout = new Timeout(Duration.create(5, "seconds"));

        assertEquals(
            StatusService.getInstance().signalStartedOrExitWithContext(cc).get(timeout.duration().toMillis()),
            F.None()
        );

        assertEquals(
            StatusService.getInstance().signalStartedWithContext(cc).get(timeout.duration().toMillis()),
            F.None()
        );
    }
}
