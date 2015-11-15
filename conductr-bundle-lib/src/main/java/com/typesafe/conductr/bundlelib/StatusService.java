package com.typesafe.conductr.bundlelib;

import java.io.IOException;
import java.net.URL;
import com.typesafe.conductr.HttpPayload;
import static com.typesafe.conductr.bundlelib.Env.*;

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
public class StatusService {

    private StatusService() {
    }

    /**
     * Create the HttpPayload necessary to signal that a bundle has started.
     *
     * Any 2xx response code is considered a success. Any other response code is considered a failure.
     *
     * @return An HttpPayload describing how to signal that a bundle has started or null if
     * this program is not running within ConductR
     * @throws IOException
     */
    public static HttpPayload createSignalStartedPayload() throws IOException {
        if (isRunByConductR())
            return createSignalStartedPayload(CONDUCTR_STATUS, BUNDLE_ID);
        else
            return null;
    }

    static HttpPayload createSignalStartedPayload(String conductrControl, String bundleId) throws IOException {
        URL controlUrl = new URL(conductrControl + "/bundles/" + bundleId + "?isStarted=true");
        return new HttpPayload(controlUrl, "PUT");
    }
}
