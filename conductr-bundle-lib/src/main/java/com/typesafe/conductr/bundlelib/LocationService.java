/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib;

import java.net.MalformedURLException;
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
     * @throws MalformedURLException
     */
    public static HttpPayload createLookupPayload(String serviceName) throws MalformedURLException {
        if (isRunByConductR())
            return createLookupPayload(SERVICE_LOCATOR, serviceName);
        else
            return null;
    }

    /**
     * A convenience function for [[createLookupPayload]] where the payload url is created when this bundle component
     * is running in the context of ConductR. If it is not then a fallbackUrl is returned.
     * @param serviceName The name of the service
     * @param fallbackUrl The url to use when not running with ConductR
     * @throws MalformedURLException
     */
    public static String getLookupUrl(String serviceName, String fallbackUrl) throws MalformedURLException {
        HttpPayload payload = createLookupPayload(serviceName);
        if (payload == null) {
            return fallbackUrl;
        } else {
            return payload.getUrl().toString();
        }
    }

    static HttpPayload createLookupPayload(String serviceLocator, String serviceName) throws MalformedURLException {
        URL locatorUrl = new URL(serviceLocator + serviceName);
        return new HttpPayload(locatorUrl);
    }
}
