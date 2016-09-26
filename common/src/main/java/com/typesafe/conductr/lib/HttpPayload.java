package com.typesafe.conductr.lib;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class representing an HttpPayload used to communicate to a Typesafe ConductR Server.
 */
public class HttpPayload {

    private final URL url;
    private final String requestMethod;
    private final boolean followRedirects;
    private final Map<String, String> requestHeaders;

    // valid HTTP methods
    private static final String[] requestMethods = {
        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
    };

    public HttpPayload(URL url, String requestMethod, boolean followRedirects, Map<String, String> requestHeaders) {
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
        this.requestHeaders = requestHeaders;
    }

    public HttpPayload(URL url, String requestMethod, boolean followRedirects) {
        this(url, requestMethod, followRedirects, new LinkedHashMap<String, String>());
    }

    public HttpPayload(URL url, String requestMethod) {
        this(url, requestMethod, false, new LinkedHashMap<String, String>());
    }

    public HttpPayload(URL url) {
        this(url, "GET", false, new LinkedHashMap<String, String>());
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

    /**
     * @return HTTP Headers which for the request to set
     */
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Append HTTP header to the existing HttpPayload
     * @param header HTTP header name
     * @param value HTTP header value
     * @return a new instance of HttpPayload with updated header
     */
    public HttpPayload addRequestHeader(String header, String value) {
        Map<String, String> requestHeadersUpdated = new LinkedHashMap<String, String>(requestHeaders);
        requestHeadersUpdated.put(header, value);
        return new HttpPayload(url, requestMethod, followRedirects, requestHeadersUpdated);
    }
}
