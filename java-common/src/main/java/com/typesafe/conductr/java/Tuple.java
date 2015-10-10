package com.typesafe.conductr.java;

/**
 * Play's Tuple class given Java 8's lack of it.
 */
public class Tuple<A, B> {
    public final A _1;
    public final B _2;

    public Tuple(A var1, B var2) {
        this._1 = var1;
        this._2 = var2;
    }

    public String toString() {
        return "(" + this._1 + ", " + this._2 + ")";
    }

    public int hashCode() {
        byte var2 = 1;
        int var3 = 31 * var2 + (this._1 == null?0:this._1.hashCode());
        var3 = 31 * var3 + (this._2 == null?0:this._2.hashCode());
        return var3;
    }

    public boolean equals(Object var1) {
        if(this == var1) {
            return true;
        } else if(var1 == null) {
            return false;
        } else if(!(var1 instanceof Tuple)) {
            return false;
        } else {
            Tuple var2 = (Tuple)var1;
            if(this._1 == null) {
                if(var2._1 != null) {
                    return false;
                }
            } else if(!this._1.equals(var2._1)) {
                return false;
            }

            if(this._2 == null) {
                if(var2._2 != null) {
                    return false;
                }
            } else if(!this._2.equals(var2._2)) {
                return false;
            }

            return true;
        }
    }
}