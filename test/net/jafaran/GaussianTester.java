/*
 * Copyright 2014 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jafaran;

import java.util.Random;

import junit.framework.TestCase;

/**
 * To test implementations of Random.nextGaussian().
 */
public class GaussianTester extends TestCase {

    /*
     * Normal distribution:
     * y = 1/sqrt(2*PI) * exp(-x^2/2)
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final double NORMAL_SIGMA = 1.0;
    private static final double NORMAL_KURTOSIS = 3.0;
    
    /*
     * 
     */

    private static final long DEFAULT_NBR_OF_CALLS = 10L * 1000L * 1000L;
    
    private static final int DEFAULT_HALF_NBR_OF_RANGES = 100 * 1000;

    private static final int DEFAULT_MIN_SIGNIFICANT_NBR = 1000;

    /**
     * g_normal(10.0) = 7.69459862670642E-23
     */
    private static final double DEFAULT_MAX_X = 10.0;

    private static final double DEFAULT_RELATIVE_TOLERANCE = 0.15;

    private static final double DEFAULT_MEAN_ABS_TOLERANCE = 1e-2;
    private static final double DEFAULT_SIGMA_ABS_TOLERANCE = 1e-3;
    
    private static final double DEFAULT_SKEWNESS_ABS_TOLERANCE = 1e-2;
    private static final double DEFAULT_KURTOSIS_ABS_TOLERANCE = 1e-2;

    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final double G_K = 1.0/Math.sqrt(2*Math.PI);

    private final long nbrOfCalls;

    /**
     * Max acceptable |x|.
     * 
     * Must be high enough so that we can consider having
     * a superior value to be a bug (or very bad luck),
     * and small enough so that we can detect such bugs.
     */
    private final double maxX;

    /**
     * Half number of ranges in which we count values.
     * Each range width is maxX/halfNbrOfRanges.
     * 
     * Only downsides if large: uses more memory.
     */
    private final int halfNbrOfRanges;
    private final double rangeWidth;
    
    /**
     * Minimum number of values, over one or multiple consecutive ranges,
     * for amount test to be relevant.
     * Linked to relativeTolerance.
     */
    private final int minSignificantNbr;

    /**
     * Must be large enough to have only a small chance for a test fail.
     */
    private final double relativeTolerance;

    private final double meanAbsTolerance;
    private final double sigmaAbsTolerance;
    
    private final double skewnessAbsTolerance;
    private final double kurtosisAbsTolerance;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default parameters.
     */
    public GaussianTester() {
        this(DEFAULT_NBR_OF_CALLS);
    }

    /**
     * Uses some default parameters.
     * 
     * @param nbrOfCalls Should be at least 100L * 1000L.
     */
    public GaussianTester(long nbrOfCalls) {
        this(
                nbrOfCalls,
                DEFAULT_MAX_X);
    }
    
    /**
     * Uses some default parameters.
     * 
     * @param nbrOfCalls Should be at least 100L * 1000L.
     * @param maxX Absolute value of max acceptable result.
     */
    public GaussianTester(
            long nbrOfCalls,
            double maxX) {
        this(
                nbrOfCalls,
                maxX,
                DEFAULT_HALF_NBR_OF_RANGES,
                DEFAULT_MIN_SIGNIFICANT_NBR,
                DEFAULT_RELATIVE_TOLERANCE,
                DEFAULT_MEAN_ABS_TOLERANCE * (DEFAULT_NBR_OF_CALLS/(double)nbrOfCalls),
                DEFAULT_SIGMA_ABS_TOLERANCE * (DEFAULT_NBR_OF_CALLS/(double)nbrOfCalls),
                DEFAULT_SKEWNESS_ABS_TOLERANCE * (DEFAULT_NBR_OF_CALLS/(double)nbrOfCalls),
                DEFAULT_KURTOSIS_ABS_TOLERANCE * (DEFAULT_NBR_OF_CALLS/(double)nbrOfCalls));
    }

    public GaussianTester(
            long nbrOfCalls,
            double maxX,
            int halfNbrOfRanges,
            int minSignificantNbr,
            double relativeTolerance,
            double meanAbsTolerance,
            double sigmaAbsTolerance,
            double skewnessAbsTolerance,
            double kurtosisAbsTolerance) {
        this.nbrOfCalls = nbrOfCalls;
        this.maxX = maxX;
        this.halfNbrOfRanges = halfNbrOfRanges;
        this.rangeWidth = maxX/halfNbrOfRanges;
        this.minSignificantNbr = minSignificantNbr;
        this.relativeTolerance = relativeTolerance;
        this.meanAbsTolerance = meanAbsTolerance;
        this.sigmaAbsTolerance = sigmaAbsTolerance;
        this.skewnessAbsTolerance = skewnessAbsTolerance;
        this.kurtosisAbsTolerance = kurtosisAbsTolerance;
    }
    
    /*
     * 
     */

    /**
     * @param random Random which nextGaussian() method must be tested.
     */
    public void test_XXX_nextGaussian(Random random) {
        if (DEBUG) {
            System.out.println("--- test_XXX_nextGaussian("+random+") ---");
        }
        final long[] negCounts = new long[this.halfNbrOfRanges];
        final long[] posCounts = new long[this.halfNbrOfRanges];
        
        double previousValue = Double.NaN;
        long directionChangeCount = 0;

        double sumOfX = 0.0;
        double sumOfX2 = 0.0;
        double sumOfX3 = 0.0;
        double sumOfX4 = 0.0;

        for (long i=0;i<this.nbrOfCalls;i++) {
            final double value = random.nextGaussian();
            if (!((value >= -this.maxX) && (value <= this.maxX))) {
                // Bug or very bad luck.
                throw new AssertionError("out of range : "+value);
            }
            
            final double v2 = value*value;
            
            sumOfX += value;
            sumOfX2 += v2;
            sumOfX3 += (v2*value);
            sumOfX4 += (v2*v2);
            
            final int rangeIndex = (int)Math.floor(Math.abs(value)/this.rangeWidth);
            if (rangeIndex >= this.halfNbrOfRanges) {
                // Out of range or on edge: ignoring.
                continue;
            }

            if (value < 0.0) {
                negCounts[rangeIndex]++;
            } else {
                posCounts[rangeIndex]++;
            }

            if ((i != 0) && (value != previousValue)) {
                if (value > previousValue) {
                    directionChangeCount++;
                } else {
                    directionChangeCount--;
                }
            }
            previousValue = value;
        }
        
        /*
         * Checks.
         */
        
        boolean ok = true;
        
        double maxXDone = 0.0;
        
        for (boolean neg : new boolean[]{true,false}) {
            double minXMin = 0.0;
            int rangeCount = 0;
            long cumulatedCount = 0;
            double cumulatedMeanNbrExpected = 0.0;
            for (int i=0;i<this.halfNbrOfRanges;i++) {
                double xMin = i * this.rangeWidth;
                double xMax = (i+1) * this.rangeWidth;
                double xMean = (xMin+xMax) * 0.5;
                long count = (neg ? negCounts[i] : posCounts[i]);
                double meanNbrExpected = (this.nbrOfCalls * this.rangeWidth) * g_normal(xMean);
                rangeCount++;
                cumulatedCount += count;
                cumulatedMeanNbrExpected += meanNbrExpected;
                if (cumulatedMeanNbrExpected >= this.minSignificantNbr) {
                    boolean localOK = isAboutEqual(cumulatedCount,cumulatedMeanNbrExpected);
                    if (!localOK) {
                        System.out.println("bad count: neg = "+neg+", i = "+i+", x in ["+minXMin+","+xMax+"] (rangeCount = "+rangeCount+"), count = "+cumulatedCount+", expected = "+cumulatedMeanNbrExpected);
                        ok = false;
                    }
                    
                    minXMin = xMax;
                    rangeCount = 0;
                    cumulatedCount = 0;
                    cumulatedMeanNbrExpected = 0.0;
                    maxXDone = xMax;
                } else {
                    // Will test later if possible.
                }
            }
        }
        
        if (!isAboutZero(directionChangeCount,this.nbrOfCalls)) {
            System.out.println("bad direction change count: "+directionChangeCount);
            ok = false;
        }
        
        final double mean = sumOfX/this.nbrOfCalls;
        if (!(Math.abs(mean) < this.meanAbsTolerance)) {
            System.out.println("bad mean: "+mean+" (tolerance = "+this.meanAbsTolerance+")");
            ok = false;
        }
        
        final double sigma = Math.sqrt(sumOfX2/this.nbrOfCalls);
        if (!(Math.abs(sigma - NORMAL_SIGMA) < this.sigmaAbsTolerance)) {
            System.out.println("bad sigma: "+sigma+" (tolerance = "+this.sigmaAbsTolerance+")");
            ok = false;
        }
        
        // Actually pseudo-skewness, since we don't use mean,
        // which would require to store all the values as they
        // get generated.
        final double skewness = skewness(sumOfX2, sumOfX3, this.nbrOfCalls);
        if (!(Math.abs(skewness) < this.skewnessAbsTolerance)) {
            System.out.println("bad skewness: "+skewness+" (tolerance = "+this.skewnessAbsTolerance+")");
            ok = false;
        }
        
        // Actually pseudo-kurtosis, since we don't use mean,
        // which would require to store all the values as they
        // get generated.
        final double kurtosis = kurtosis(sumOfX2, sumOfX4, this.nbrOfCalls);
        if (!(Math.abs(kurtosis - NORMAL_KURTOSIS) < this.kurtosisAbsTolerance)) {
            System.out.println("bad kurtosis: "+kurtosis+" (tolerance = "+this.kurtosisAbsTolerance+")");
            ok = false;
        }
        
        if (DEBUG) {
            System.out.println("random = "+random);
            System.out.println("maxXDone = "+maxXDone);
            System.out.println("directionChangeCount = "+directionChangeCount);
            System.out.println("mean = "+mean);
            System.out.println("sigma = "+sigma);
            System.out.println("skewness = "+skewness);
            System.out.println("kurtosis = "+kurtosis);
        }
        assertTrue(ok);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private boolean isAboutZero(double error, double magnitude) {
        final double relDelta = Math.abs(error/magnitude);
        return (relDelta < this.relativeTolerance);
    }

    private boolean isAboutEqual(long count, double magnitude) {
        return isAboutZero(count - magnitude, magnitude);
    }

    private static double g_normal(double x) {
        return G_K * Math.exp(-0.5 * (x*x));
    }
    
    /*
     * Formula from "A Suggestion for Using Powerful and Informative Tests of Normality",
     * R. B. D'Agostino, A. Belanger, R. B. D'Agostino Jr.
     */
    
    /**
     * 0 for normal distribution (because it's symmetric).
     * If < 0, leans to the left.
     * If > 0, leans to the right.
     * 
     * @return Skewness.
     */
    private static double skewness(
            double sumXiMinusMeanPow2,
            double sumXiMinusMeanPow3,
            long n) {
        return (sumXiMinusMeanPow3/n) / Math.pow((sumXiMinusMeanPow2/n), 1.5);
    }

    /**
     * 3(n-1)/(n+1) for normal distribution (so 3 if n = +Infinity).
     * If < 3, max is lower, and tends to be a circle.
     * If > 3, max is higher, and tends to be a Dirac.
     * 
     * @return Kurtosis (curvature).
     */
    private static double kurtosis(
            double sumXiMinusMeanPow2,
            double sumXiMinusMeanPow4,
            long n) {
        return (sumXiMinusMeanPow4/n) / Math.pow((sumXiMinusMeanPow2/n), 2);
    }
}
