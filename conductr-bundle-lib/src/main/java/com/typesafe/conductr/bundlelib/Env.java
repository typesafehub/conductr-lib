package com.typesafe.conductr.bundlelib;

/**
 * ConductR environment vars.
 */
public class Env {
    protected Env() {
    }

    /**
     * The bundle id of the current bundle
     */
    public static final String BUNDLE_ID = System.getenv("BUNDLE_ID");

    /**
     * The URL associated with reporting status back to ConductR
     */
    public static final String CONDUCTR_STATUS = System.getenv("CONDUCTR_STATUS");

    /**
     * The URL associated with locating services known to ConductR
     */
    public static final String SERVICE_LOCATOR = System.getenv("SERVICE_LOCATOR");

    /**
     * @return true if ConductR started this process.
     */
    public static boolean isRunByConductR() {
        return BUNDLE_ID != null;
    }
}
