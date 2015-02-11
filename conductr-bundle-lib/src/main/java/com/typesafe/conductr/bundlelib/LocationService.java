/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib;

import java.io.IOException;
import java.net.URL;
import static com.typesafe.conductr.bundlelib.Env.*;

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
public class LocationService {

    private LocationService() {
    }

    /**
     * Create the HttpPayload necessary to look up a service by name.
     *
     * If the service is available and can bee looked up the response for the HTTP request should be
     * 307 (Temporary Redirect), and the resulting URI to the service is in the "Location" header of the response.
     * A Cache-Control header may also be returned indicating the maxAge that the location should be cached for.
     * If the service can not be looked up the response should be 404 (Not Found).
     * All other response codes are considered illegal.
     *
     * @param serviceName The name of the service
     * @return An HttpPayload describing how to do the service lookup or null if
     * this program is not running within ConductR
     * @throws IOException
     */
    public static HttpPayload createLookupPayload(String serviceName) throws IOException {
        if (isRunByConductR())
            return createLookupPayload(SERVICE_LOCATOR, serviceName);
        else
            return null;
    }

    static HttpPayload createLookupPayload(String serviceLocator, String serviceName) throws IOException {
        URL locatorUrl = new URL(serviceLocator + serviceName);
        return new HttpPayload(locatorUrl);
    }
}
