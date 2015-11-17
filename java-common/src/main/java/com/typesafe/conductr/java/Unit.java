package com.typesafe.conductr.java;

/**
 * As per Scala's Unit, think of it as a Void that has a value.
 */
public class Unit {
    public final static Unit VALUE = new Unit();

    private Unit() {
    }
}
