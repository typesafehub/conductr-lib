/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib;

/**
 * ConductR environment vars.
 */
public class Env {
    private Env() {
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
}
