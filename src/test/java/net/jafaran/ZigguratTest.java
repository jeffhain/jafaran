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

import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;

public class ZigguratTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final boolean USE_RANDOM_SEED = false;
    private static final long SEED = USE_RANDOM_SEED ? new Random().nextLong() : 123456789L;
    static {
        if (USE_RANDOM_SEED) {
            System.out.println("SEED = "+SEED);
        }
    }

    private static final long NBR_OF_CALLS = 10L * 1000L * 1000L;

    /**
     * Max acceptable |x| (= 13.707591184795413).
     * 
     * Our algorithm can't return a value of larger magnitude,
     * cf. implementation for comments.
     * 
     * g_normal(13.707591184795413) = 6.300363485251426E-42
     * so very little chance to obtain it in practice,
     * but since we use random longs of special magnitudes
     * for sturdiness tests, we typically approach it.
     */
    private static final double MAX_X = Ziggurat.R_256 - StrictMath.log(1.0/(1L<<53)) * (1.0/Ziggurat.R_256);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MySturdinessRandom extends Random {
        private static final long serialVersionUID = 1L;
        final NumbersTestUtils utils;
        final ArrayList<Integer> usedRandomInts = new ArrayList<Integer>();
        final ArrayList<Long> usedRandomLongs = new ArrayList<Long>();
        public MySturdinessRandom() {
            this.utils = new NumbersTestUtils(newSource(SEED));
        }
        public void clearMemory() {
            this.usedRandomInts.clear();
            this.usedRandomLongs.clear();
        }
        @Override
        public int nextInt() {
            int bits = this.utils.randomIntWhatever();
            this.usedRandomInts.add(bits);
            return bits;
        }
        @Override
        public long nextLong() {
            long bits = this.utils.randomLongWhatever();
            this.usedRandomLongs.add(bits);
            return bits;
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Testing nextGaussian(Random) sturdiness,
     * with whatever sorts of nextLong().
     */
    public void test_nextGaussian_Random_sturdiness() {
        if (DEBUG) {
            System.out.println("--- test_nextGaussian_Random_sturdiness() ---");
            System.out.println("SEED = "+SEED);
        }
        
        final MySturdinessRandom random = new MySturdinessRandom();
        
        for (long i=0;i<NBR_OF_CALLS;i++) {
            random.clearMemory();
            final double value = Ziggurat.nextGaussian(random);
            if (!((value >= -MAX_X) && (value <= MAX_X))) {
                System.out.println("SEED = "+SEED);
                System.out.println("usedRandomInts = "+random.usedRandomInts);
                System.out.println("usedRandomLongs = "+random.usedRandomLongs);
                throw new AssertionError("out of range: "+value);
            }
        }
    }
    
    /**
     * Testing nextGaussianFast(Random) sturdiness,
     * with whatever sorts of nextInt().
     */
    public void test_nextGaussianFast_Random_sturdiness() {
        if (DEBUG) {
            System.out.println("--- test_nextGaussianFast_Random_sturdiness() ---");
            System.out.println("SEED = "+SEED);
        }

        final MySturdinessRandom random = new MySturdinessRandom();
        
        for (long i=0;i<NBR_OF_CALLS;i++) {
            random.clearMemory();
            final double value = Ziggurat.nextGaussianFast(random);
            if (!((value >= -MAX_X) && (value <= MAX_X))) {
                System.out.println("SEED = "+SEED);
                System.out.println("usedRandomInts = "+random.usedRandomInts);
                System.out.println("usedRandomLongs = "+random.usedRandomLongs);
                throw new AssertionError("out of range: "+value);
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_nextGaussian_Random() {
        if (DEBUG) {
            System.out.println("--- test_nextGaussian_Random() ---");
            System.out.println("SEED = "+SEED);
        }
        final Random source = newSource(SEED);
        final GaussianTester tester = new GaussianTester(
                NBR_OF_CALLS,
                MAX_X);
        tester.test_XXX_nextGaussian(new Random() {
            @Override
            public String toString() {
                return "Ziggurat.nextGaussian(Random)";
            }
            @Override
            public double nextGaussian() {
                return Ziggurat.nextGaussian(source);
            }
        });
    }

    public void test_nextGaussianFast_Random() {
        if (DEBUG) {
            System.out.println("--- test_nextGaussianFast_Random() ---");
            System.out.println("SEED = "+SEED);
        }
        final Random source = newSource(SEED);
        final GaussianTester tester = new GaussianTester(
                NBR_OF_CALLS,
                MAX_X);
        tester.test_XXX_nextGaussian(new Random() {
            @Override
            public String toString() {
                return "Ziggurat.nextGaussianFast(Random)";
            }
            @Override
            public double nextGaussian() {
                return Ziggurat.nextGaussianFast(source);
            }
        });
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * A Random source good enough not to fail a test because of it.
     */
    private static Random newSource(long seed) {
        // Random seems ok.
        return new Random(seed);
    }
}
