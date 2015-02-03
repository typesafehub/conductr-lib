/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib;

import java.io.IOException;
import java.net.URL;
import static com.typesafe.conductr.bundlelib.Common.*;

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
public class LocationService {

    private LocationService() {
    }

    private static final String SERVICE_LOCATOR = System.getenv("SERVICE_LOCATOR");

    /**
     * Create the HttpPayload necessary to look up a service by name.
     *
     * If the service is available and can bee looked up the response for the HTTP request should be
     * 308 (Permanent Redirect), and the resulting URL to the service is in the "Location" header of the response.
     * If the service can not be looked up the response should be 404 (Not Found).
     * All other response codes are considered illegal.
     *
     * @param serviceName The name of the service
     * @return An HttpPayload describing how to do the service lookup
     * @throws IOException
     */
    public static HttpPayload createLookupPayload(String serviceName) throws IOException {
        // if we have no BUNDLE_ID, we are in development mode and do nothing
        if (BUNDLE_ID == null)
            return null;
        else
            return createLookupPayload(SERVICE_LOCATOR, serviceName);
    }

    static HttpPayload createLookupPayload(String serviceLocator, String serviceName) throws IOException {
        URL locatorUrl = new URL(serviceLocator + serviceName);
        return new HttpPayload(locatorUrl);
    }
}
