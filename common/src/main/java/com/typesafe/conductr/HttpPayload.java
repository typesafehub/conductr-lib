package com.typesafe.conductr;

import java.net.URL;

/**
 * Class representing an HttpPayload used to communicate to a Typesafe ConductR Server.
 */
public class HttpPayload {

    private final URL url;
    private final String requestMethod;
    private final boolean followRedirects;

    // valid HTTP methods
    private static final String[] requestMethods = {
        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
    };

    public HttpPayload(URL url, String requestMethod, boolean followRedirects) {
        String rm = requestMethod.toUpperCase();
        boolean validMethod = false;
        for (String vm: requestMethods) {
            if (rm.equals(vm)) {
                validMethod = true;
                break;
            }
        }
        if (!validMethod)
            throw new IllegalArgumentException("Invalid request method " + requestMethod);
        this.url = url;
        this.requestMethod = rm;
        this.followRedirects = followRedirects;
    }

    public HttpPayload(URL url, String requestMethod) {
        this(url, requestMethod, false);
    }

    public HttpPayload(URL url) {
        this(url, "GET", false);
    }

    /**
     * @return The URL to connect to
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @return The request method to use
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * @return Should the request follow redirects
     */
    public boolean getFollowRedirects() {
        return followRedirects;
    }
}
