package com.typesafe.conductr.lib.java;

import java.io.UncheckedIOException;
import java.util.concurrent.ForkJoinPool;

/**
 * Managed blocking functionality.
 */
public class ManagedBlocker {
    /**
     * Perform a block operation given a runnable operation.
     */
    public static void blocking(Runnable op) throws InterruptedException {
        ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
            @Override
            public boolean block() throws InterruptedException {
                try {
                    op.run();
                } catch (UncheckedIOException e) {
                    throw new InterruptedException("Interrupted due to an IO exception:" + e.getMessage());
                }
                return true;
            }

            @Override
            public boolean isReleasable() {
                return false;
            }
        });
    }
}