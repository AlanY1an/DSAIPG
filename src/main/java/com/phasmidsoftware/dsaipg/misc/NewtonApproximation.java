/*
 * Copyright (c) 2017-2024. Robin Hillyard
 */

package com.phasmidsoftware.dsaipg.misc;

import java.util.function.Function;

class NewtonApproximation {
    public static double newtonMethod(Function<Double, Double> f, Function<Double, Double> fPrime,
                                      double x0, double tol, int maxIter){
        double x = x0;
        int iter = 0;
        while(iter < maxIter){
            double fx = f.apply(x);
            double fpx = fPrime.apply(x);
            if (Math.abs(fpx) < 1e-10) {
                System.out.println("Try another x0");
                break;
            }
            double newx = x - fx/fpx;
            if (Math.abs(newx - x) < tol) {
                System.out.println("Iter: " + (iter + 1));
                return newx;
            }
            x = newx;
            iter ++;
        }
        System.out.println("Reach MaxIter");
        return x;
    }


    public static void main(String[] args) {
        Function<Double, Double> f = x -> Math.cos(x) - x;
        Function<Double, Double> fPrime = x -> -Math.sin(x) - 1;

        double initialGuess = 0.5;
        double tolerance = 1e-7;
        int maxIterations = 100;
        double solution = newtonMethod(f, fPrime, initialGuess, tolerance, maxIterations);
        System.out.println("x = " + solution);
    }
}