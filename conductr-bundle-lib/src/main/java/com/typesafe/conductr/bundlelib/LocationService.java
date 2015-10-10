package com.typesafe.conductr.bundlelib;

import java.net.MalformedURLException;
import java.net.URL;
import com.typesafe.conductr.HttpPayload;
import static com.typesafe.conductr.bundlelib.Env.*;

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
public class LocationService {

    protected LocationService() {
    }

    /**
     * Create the HttpPayload necessary to look up a service by name.
     *
     * If the service is available and can be looked up the response for the HTTP request should be
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
     * A convenience function where the payload url is created when this bundle component
     * is running in the context of ConductR. If it is not then a fallback is returned.
     * @param serviceName The name of the service
     * @param fallback The url to use when not running with ConductR
     * @throws MalformedURLException
     */
    public static URL getLookupUrl(String serviceName, URL fallback) throws MalformedURLException {
        HttpPayload payload = createLookupPayload(serviceName);
        if (payload == null) {
            return fallback;
        } else {
            return payload.getUrl();
        }
    }

    static HttpPayload createLookupPayload(String serviceLocator, String serviceName) throws MalformedURLException {
        URL locatorUrl = new URL(serviceLocator + serviceName);
        return new HttpPayload(locatorUrl);
    }
}
