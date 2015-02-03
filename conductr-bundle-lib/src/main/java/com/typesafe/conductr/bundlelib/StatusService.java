/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib;

import java.io.IOException;
import java.net.URL;
import static com.typesafe.conductr.bundlelib.Common.*;

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
public class StatusService {

    private StatusService() {
    }

    private static final String CONDUCTR_STATUS = System.getenv("CONDUCTR_STATUS");

    /**
     * Create the HttpPayload necessary to signal that a bundle has started.
     *
     * Any 2xx response code is considered a success. Any other response code is considered a failure.
     *
     * @return An HttpPayload describing how to signal that a bundle has started
     * @throws IOException
     */
    public static HttpPayload createSignalStartedPayload() throws IOException {
        // if we have no BUNDLE_ID, we are in development mode and do nothing
        if (BUNDLE_ID == null)
            return null;
        else
            return createSignalStartedPayload(CONDUCTR_STATUS, BUNDLE_ID);
    }

    static HttpPayload createSignalStartedPayload(String conductrControl, String bundleId) throws IOException {
        URL controlUrl = new URL(conductrControl + "/bundles/" + bundleId + "?isStarted=true");
        return new HttpPayload(controlUrl, "PUT");
    }
}
