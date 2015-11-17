package com.typesafe.conductr.bundlelib.java;

import java.util.Optional;

/**
 * Standard ConductR environment vars.
 */
public class Env extends com.typesafe.conductr.bundlelib.Env {
    /**
     * The bundle id of the current bundle
     */
    public static final Optional<String> BUNDLE_ID =
            Optional.ofNullable(com.typesafe.conductr.bundlelib.Env.BUNDLE_ID);

    /**
     * The URL associated with reporting status back to ConductR
     */
    public static final Optional<String> CONDUCTR_STATUS =
            Optional.ofNullable(com.typesafe.conductr.bundlelib.Env.CONDUCTR_STATUS);

    /**
     * The URL associated with locating services known to ConductR
     */
    public static final Optional<String> SERVICE_LOCATOR =
            Optional.ofNullable(com.typesafe.conductr.bundlelib.Env.SERVICE_LOCATOR);
}
