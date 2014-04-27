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
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

public class RandomsTest extends TestCase {

    /*
     * Not quite as good as Diehard tests: just checking for obvious problems
     * (our RNGs should be simple enough not to have too weird ones).
     * 
     * Still, was able to show a strong weakness in linear congruential RNG
     * (int seed, seed = seed * 69069 + 5: LSBit of nextLong() would always be 0,
     * due to LSBit of nextInt() being alternatively 1 or 0), so we deleted it.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean USE_RANDOM_SEED = false;
    private static final long SEED = USE_RANDOM_SEED ? new Random().nextLong() : 123456789L;
    static {
        if (USE_RANDOM_SEED) {
            System.out.println("SEED = "+SEED);
        }
    }

    /**
     * To avoid creating too slow tests.
     */
    private static final int DEFAULT_MAX_NBR_OF_CALLS = 100 * 1000;

    /**
     * Small enough for tests to be fast (ZigguratTest uses larger amount;
     * no need to repeat a heavy test multiple times).
     */
    private static final long NBR_OF_CALLS_GAUSSIAN = 1000L * 1000L;

    /**
     * Minimum number of rolls for a measure.
     * Large enough to have only a small chance for a test fail.
     */
    private static final int MIN_NBR_OF_ROLLS = 1000;

    /**
     * Large enough to have only a small chance for a test fail.
     */
    private static final double RELATIVE_TOLERANCE = 0.2;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private interface MyInterfaceRandomFactory  {
        public Random newRandom();
        /**
         * To test constructor that uses seed
         * (otherwise could just use setSeed(long)
         * after calling newRandom()), or to
         * avoid having to use setSeed(long).
         * 
         * @throws UnsupportedOperationException if the Random can't use a seed
         *         (i.e. if it's a pure (non-pseudo) RNG).
         */
        public Random newRandom(long seed);
        public Random newRandom(Void dummy);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Tests that each new instance has a different seed.
     */
    public void test_constructor() {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random1 = factory.newRandom();
            final Random random2 = factory.newRandom();

            boolean diff = false;
            for (int i=0;i<DEFAULT_MAX_NBR_OF_CALLS;i++) {
                if (random1.nextInt() != random2.nextInt()) {
                    diff = true;
                    break;
                }
            }

            if (!diff) {
                System.out.println("random1 = "+random1);
                System.out.println("random2 = "+random2);
                assertTrue(false);
            }
        }
    }

    /**
     * Tests that the specified seed is used.
     */
    public void test_constructor_long() {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random1;
            try {
                random1 = factory.newRandom(SEED);
            } catch (UnsupportedOperationException e) {
                // Not supported.
                continue;
            }
            final Random random2 = factory.newRandom(SEED);

            boolean diff = false;
            for (int i=0;i<DEFAULT_MAX_NBR_OF_CALLS;i++) {
                if (random1.nextInt() != random2.nextInt()) {
                    diff = true;
                    break;
                }
            }

            if (diff) {
                System.out.println("random1 = "+random1);
                System.out.println("random2 = "+random2);
                assertTrue(false);
            }
        }
    }

    /**
     * Tests that implementation created without seeding, once seeded,
     * behave the same as implementations created with a random seed
     * and then seeded with the same seed.
     */
    public void test_constructor_Object() {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random1;
            try {
                random1 = factory.newRandom((Void)null);
            } catch (UnsupportedOperationException e) {
                // Not supported.
                continue;
            }
            final Random random2 = factory.newRandom();
            
            random1.setSeed(SEED);
            random2.setSeed(SEED);

            boolean diff = false;
            for (int i=0;i<DEFAULT_MAX_NBR_OF_CALLS;i++) {
                if (random1.nextInt() != random2.nextInt()) {
                    diff = true;
                    break;
                }
            }
            
            if (diff) {
                System.out.println("random1 = "+random1);
                System.out.println("random2 = "+random2);
                assertTrue(false);
            }
        }
    }

    /**
     * Tests that the specified seed is used,
     * and that setSeed(long) resets stored bits.
     */
    public void test_setSeed_long() {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random1 = factory.newRandom();
            final Random random2 = factory.newRandom();

            try {
                random1.setSeed(SEED);
            } catch (UnsupportedOperationException e) {
                // Not supported.
                continue;
            }
            random2.setSeed(SEED);

            boolean diff = false;
            for (int i=0;i<DEFAULT_MAX_NBR_OF_CALLS;i++) {
                if (random1.nextInt() != random2.nextInt()) {
                    diff = true;
                    break;
                }
            }

            if (diff) {
                System.out.println("random1 = "+random1);
                System.out.println("random2 = "+random2);
                assertTrue(false);
            }
            
            /*
             * Test that clears stored bits.
             */
            
            {
                final Random storeless = factory.newRandom(SEED);
                final Random storeful = factory.newRandom(SEED);
                // Might cause stored bits.
                storeful.nextBoolean();
                // Must clear stored bits.
                storeful.setSeed(SEED);
                storeless.setSeed(SEED);
                for (int i=0;i<1000;i++) {
                    assertEquals(storeless.nextBoolean(), storeful.nextBoolean());
                }
            }
        }
    }

    /*
     * uniform
     */

    public void test_nextBoolean() {
        this.test_bitsUniformity(-1);
    }

    public void test_nextBit() {
        this.test_bitsUniformity(1);
    }

    public void test_nextByte() {
        this.test_bitsUniformity(8);
    }

    public void test_nextShort() {
        this.test_bitsUniformity(16);
    }

    public void test_nextInt() {
        this.test_bitsUniformity(32);
    }

    public void test_nextLong() {
        this.test_bitsUniformity(64);
    }

    /**
     * @param bitSizeOrHack Bit size, or -1 for "nextBoolean()".
     */
    public void test_bitsUniformity(int bitSizeOrHack) {
        final int bitSize = Math.abs(bitSizeOrHack);
        final int nbrOfCalls = computeNbrOfCalls(bitSize, MIN_NBR_OF_ROLLS);

        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random = factory.newRandom(SEED);
            if (((bitSizeOrHack == 1) || (bitSizeOrHack == 8) || (bitSizeOrHack == 16)) && !(random instanceof AbstractRNG)) {
                // Irrelevant.
                continue;
            }

            /*
             * Counting occurrences of each bit,
             * and how many times consecutive bits
             * were different.
             */

            final int[] bitCount = new int[bitSize];
            long changeCount = 0;

            boolean previousBit = false;

            for (int i=0;i<nbrOfCalls;i++) {
                final long bits;
                if (bitSizeOrHack == -1) {
                    bits = (random.nextBoolean() ? 1 : 0);
                } else if (bitSizeOrHack == 1) {
                    bits = (((AbstractRNG)random).nextBit() & 1);
                } else if (bitSizeOrHack == 8) {
                    bits = (((AbstractRNG)random).nextByte() & 0xFF);
                } else if (bitSizeOrHack == 16) {
                    bits = (((AbstractRNG)random).nextShort() & 0xFFFF);
                } else if (bitSizeOrHack == 32) {
                    bits = random.nextInt() & 0xFFFFFFFFL;
                } else {
                    bits = random.nextLong();
                }

                for (int b=0;b<bitSize;b++) {
                    final boolean bit = (((bits>>b)&1) != 0);
                    if (bit) {
                        bitCount[b]++;
                    }
                    if (bit != previousBit) {
                        changeCount++;
                    }
                    previousBit = bit;
                }
            }

            boolean ok = true;
            for (int i=0;i<bitSize;i++) {
                ok &= isAboutHalf(bitCount[i],nbrOfCalls);
            }
            // Bit should change half the time.
            ok &= isAboutHalf(changeCount/bitSize,nbrOfCalls);
            if (!ok) {
                System.out.println("random = "+random);
                System.out.println("nbrOfCalls = "+nbrOfCalls);
                for (int i=0;i<bitCount.length;i++) {
                    System.out.println("bitCount["+i+"] = "+bitCount[i]);
                }
                System.out.println("changeCount = "+changeCount);
            }
            assertTrue(ok);
        }
    }

    public void test_nextInt_int() {
        test_mathematicalIntegerUniformity_0_maxExcl(
                32,
                new Integer[]{Integer.MIN_VALUE,Integer.MIN_VALUE/2,-3,-2,-1,0},
                // Some powers of two, to test our special case.
                new Integer[]{2,3,4,5,100,(1<<15),(1<<29),(1<<30),Integer.MAX_VALUE/2,Integer.MAX_VALUE});
    }

    public void test_nextLong_long() {
        test_mathematicalIntegerUniformity_0_maxExcl(
                64,
                new Long[]{Long.MIN_VALUE,Long.MIN_VALUE/2,-3L,-2L,-1L,0L},
                new Long[]{2L,3L,4L,5L,100L,Long.MAX_VALUE/2,Long.MAX_VALUE});
    }

    public <T extends Number> void test_mathematicalIntegerUniformity_0_maxExcl(
            int bitSize,
            T[] illegalMaxExclTab,
            T[] maxExclTab) {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random = factory.newRandom(SEED);
            if ((bitSize == 64) && !(random instanceof AbstractRNG)) {
                // Irrelevant.
                continue;
            }

            for (Number maxExcl : illegalMaxExclTab) {
                try {
                    if (bitSize == 32) {
                        random.nextInt(maxExcl.intValue());
                    } else if (bitSize == 64) {
                        ((AbstractRNG)random).nextLong(maxExcl.longValue());
                    } else {
                        throw new AssertionError();
                    }
                    System.out.println("bad: no exception for "+maxExcl);
                    System.out.println("random = "+random);
                    System.out.println("bitSize = "+bitSize);
                    assertTrue(false);
                } catch (IllegalArgumentException e) {
                    // ok
                }
            }

            for (int i=0;i<DEFAULT_MAX_NBR_OF_CALLS;i++) {
                final long value;
                if (bitSize == 32) {
                    value = random.nextInt(1);
                } else if (bitSize == 64) {
                    value = ((AbstractRNG)random).nextLong(1L);
                } else {
                    throw new AssertionError();
                }
                if (value != 0L) {
                    System.out.println("bad: expected 0 but got "+value);
                    System.out.println("random = "+random);
                    System.out.println("bitSize = "+bitSize);
                    assertTrue(false);
                }
            }

            for (Number maxExcl : maxExclTab) {
                /*
                 * Counting occurrences in ranges of same lengths,
                 * and directions changes.
                 */
                final int nbrOfRangesForLargeMax = 10;
                final int nbrOfRanges;
                if (maxExcl.longValue() > MIN_NBR_OF_ROLLS * nbrOfRangesForLargeMax) {
                    // Enough possible values to have many of them per range,
                    // which allows to smooth the fact that they are mathematical
                    // integers, and that some ranges might contain more slots
                    // of possible values.
                    nbrOfRanges = nbrOfRangesForLargeMax;
                } else {
                    // One range per value.
                    nbrOfRanges = asInt(maxExcl.longValue());
                }
                final double rangeWidth = maxExcl.longValue()/(double)nbrOfRanges;

                final int nbrOfCalls = computeNbrOfCalls(nbrOfRanges, MIN_NBR_OF_ROLLS);

                final int[] counts = new int[nbrOfRanges];

                long previousValue = -1;
                int directionCount = 0;

                for (int i=0;i<nbrOfCalls;i++) {
                    final long value;
                    if (bitSize == 32) {
                        value = random.nextInt(maxExcl.intValue());
                    } else if (bitSize == 64) {
                        value = ((AbstractRNG)random).nextLong(maxExcl.longValue());
                    } else {
                        throw new AssertionError();
                    }
                    int rangeIndex = (int)Math.floor(value/rangeWidth);
                    if (rangeIndex == counts.length) {
                        rangeIndex--;
                    }

                    counts[rangeIndex]++;

                    if ((previousValue >= 0) && (value != previousValue)) {
                        if (value > previousValue) {
                            directionCount++;
                        } else {
                            directionCount--;
                        }
                    }
                    previousValue = value;
                }

                boolean ok = true;
                for (int i=0;i<counts.length;i++) {
                    ok &= isAboutEqual((long)(counts[i] * nbrOfRanges),nbrOfCalls);
                }
                // Direction should change half the time.
                // Only considering if maxExcl is large,
                // else case where value is identical
                // would bias the measure too much.
                if (maxExcl.longValue() >= MIN_NBR_OF_ROLLS) {
                    ok &= isAboutZero(directionCount,nbrOfCalls);
                }
                if (!ok) {
                    System.out.println("random = "+random);
                    System.out.println("maxExcl = "+maxExcl);
                    System.out.println("nbrOfCalls = "+nbrOfCalls);
                    System.out.println("nbrOfRanges = "+nbrOfRanges);
                    System.out.println("rangeWidth = "+rangeWidth);
                    for (int i=0;i<counts.length;i++) {
                        System.out.println("counts["+i+"] = "+counts[i]);
                    }
                    System.out.println("directionCount = "+directionCount);
                }
                assertTrue(ok);
            }
        }
    }

    public void test_nextFloat() {

        /*
         * Testing bounds for AbstractRNG.nextFloat() implementation.
         */

        {
            final AbstractRNG rng = new AbstractRNG() {
                @Override
                public int nextInt() {
                    return 0;
                }
            };
            float expected = 0.0f;
            float actual = rng.nextFloat();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        {
            final AbstractRNG rng = new AbstractRNG() {
                @Override
                public int nextInt() {
                    return -1;
                }
            };
            float expected = 1.0f-1.0f/(1<<24);
            float actual = rng.nextFloat();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        /*
         * 
         */

        this.test_floatingPointUniformity(32, false);
    }

    public void test_nextDouble() {

        /*
         * Testing bounds for AbstractRNG.nextDouble() implementation.
         */

        {
            final AbstractRNG rng = new AbstractRNG() {
                @Override
                public long nextLong() {
                    return 0L;
                }
            };
            double expected = 0.0;
            double actual = rng.nextDouble();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        {
            final AbstractRNG rng = new AbstractRNG() {
                @Override
                public long nextLong() {
                    return -1L;
                }
            };
            double expected = 1.0-1.0/(1L<<53);
            double actual = rng.nextDouble();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        /*
         * 
         */

        this.test_floatingPointUniformity(64, false);
    }

    public void test_nextDoubleFast() {

        /*
         * Testing bounds for AbstractRNG.nextDoubleFast() implementation.
         */

        {
            final AbstractRNG rng = new AbstractRNG() {
                @Override
                public int nextInt() {
                    return 0;
                }
            };
            double expected = 0.0;
            double actual = rng.nextDoubleFast();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        {
            final AbstractRNG rng = new AbstractRNG() {
                @Override
                public int nextInt() {
                    return -1;
                }
            };
            double expected = 1.0-1.0/(1L<<31);
            double actual = rng.nextDoubleFast();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        /*
         * Testing bounds for AbstractSeqRNG.nextDoubleFast() implementation.
         */

        {
            final AbstractRNG rng = new AbstractSeqRNG() {
                @Override
                public int nextInt() {
                    return 0;
                }
            };
            double expected = 0.0;
            double actual = rng.nextDoubleFast();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        {
            final AbstractRNG rng = new AbstractSeqRNG() {
                @Override
                public int nextInt() {
                    return -1;
                }
            };
            double expected = 1.0-1.0/(1L<<31);
            double actual = rng.nextDoubleFast();
            boolean ok = (expected == actual);
            if (!ok) {
                System.out.println("expected = "+expected);
                System.out.println("actual =   "+actual);
            }
            assertTrue(ok);
        }

        /*
         * 
         */

        this.test_floatingPointUniformity(64, true);
    }

    public void test_floatingPointUniformity(
            int bitSize,
            boolean fast) {

        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random = factory.newRandom(SEED);
            if (fast && !(random instanceof AbstractRNG)) {
                // Irrelevant.
                continue;
            }

            /*
             * Counting occurrences in ranges of same lengths,
             * and directions changes.
             */

            final int nbrOfRanges = 10;
            final double rangeWidth = 1.0/(double)nbrOfRanges;

            final int nbrOfCalls = computeNbrOfCalls(nbrOfRanges, MIN_NBR_OF_ROLLS);

            final int[] counts = new int[nbrOfRanges];

            double previousValue = -1;
            int directionCount = 0;

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (int i=0;i<nbrOfCalls;i++) {
                final double value;
                if (bitSize == 32) {
                    if (fast) {
                        throw new AssertionError();
                    } else {
                        value = random.nextFloat();
                    }
                } else if (bitSize == 64) {
                    if (fast) {
                        value = ((AbstractRNG)random).nextDoubleFast();
                    } else {
                        value = random.nextDouble();
                    }
                } else {
                    throw new AssertionError();
                }
                min = Math.min(min, value);
                max = Math.max(max, value);

                int rangeIndex = (int)Math.floor(value/rangeWidth);
                if (rangeIndex == counts.length) {
                    rangeIndex--;
                }

                counts[rangeIndex]++;

                if ((previousValue >= 0) && (value != previousValue)) {
                    if (value > previousValue) {
                        directionCount++;
                    } else {
                        directionCount--;
                    }
                }
                previousValue = value;
            }

            boolean ok = (min >= 0.0) && (max < 1.0);
            for (int i=0;i<counts.length;i++) {
                ok &= isAboutEqual((long)(counts[i] * nbrOfRanges),nbrOfCalls);
            }
            // Direction should change half the time.
            ok &= isAboutZero(directionCount,nbrOfCalls);
            if (!ok) {
                System.out.println("random = "+random);
                System.out.println("min = "+min);
                System.out.println("max = "+max);
                System.out.println("nbrOfCalls = "+nbrOfCalls);
                System.out.println("nbrOfRanges = "+nbrOfRanges);
                System.out.println("rangeWidth = "+rangeWidth);
                for (int i=0;i<counts.length;i++) {
                    System.out.println("counts["+i+"] = "+counts[i]);
                }
                System.out.println("directionCount = "+directionCount);
            }
            assertTrue(ok);
        }
    }

    /*
     * gaussian
     */

    public void test_nextGaussian() {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random = factory.newRandom(SEED);

            final GaussianTester tester = new GaussianTester(NBR_OF_CALLS_GAUSSIAN);
            tester.test_XXX_nextGaussian(new Random() {
                @Override
                public String toString() {
                    return random+".nextGaussian()";
                }
                @Override
                public double nextGaussian() {
                    return random.nextGaussian();
                }
            });
        }
    }

    public void test_nextGaussianFast() {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random = factory.newRandom(SEED);
            if (!(random instanceof AbstractRNG)) {
                // Irrelevant.
                continue;
            }

            final GaussianTester tester = new GaussianTester(NBR_OF_CALLS_GAUSSIAN);
            tester.test_XXX_nextGaussian(new Random() {
                @Override
                public String toString() {
                    return random+".nextGaussianFast()";
                }
                @Override
                public double nextGaussian() {
                    return ((AbstractRNG)random).nextGaussianFast();
                }
            });
        }
    }
    
    /*
     * state get/set
     */
    
    public void test_getState_setState()  {
        for (MyInterfaceRandomFactory factory : newFactories(true)) {
            final Random random = factory.newRandom(SEED);
            if (!(random instanceof AbstractRNG)) {
                // Irrelevant.
                continue;
            }
            
            AbstractRNG rng1 = (AbstractRNG)random;
            try {
                rng1.getState();
            } catch (UnsupportedOperationException e) {
                // Not supported.
                continue;
            }
            
            // Supposing setState doesn't throw UnsupportedOperationException
            // if getState didn't, i.e. we can always expect NPE if null arg.
            try {
                rng1.setState(null);
                assertTrue(false);
            } catch (NullPointerException e) {
                // ok
            }
            
            for (int k=0;k<100;k++) {
                final AbstractRNG rng2 = (AbstractRNG)factory.newRandom();
                rng2.setState(rng1.getState());
                final int milthPrime = 7919;
                for (int i=0;i<milthPrime;i++) {
                    assertEquals(rng1.nextBit(), rng2.nextBit());
                    assertEquals(rng1.nextInt(), rng2.nextInt());
                    assertEquals(rng1.nextLong(), rng2.nextLong());
                }
            }
        }
    }

    /*
     * specific tests
     */
    
    /**
     * Tests that AbstractSeqRNG.next(int) (through nextBit())
     * returns same bits than nextInt(),
     * i.e. that is correctly store and reuses generated
     * random bits.
     */
    public void test_AbstractSeqRNG_nextBit_bits() {
        final AbstractSeqRNG ref = new MXSIntSeqRNG(SEED);
        final AbstractSeqRNG res = new MXSIntSeqRNG(SEED);
        for (int i=0;i<1000;i++) {
            final int bits = ref.nextInt();
            for (int b=32;--b>=0;) {
                assertEquals((bits>>b)&1, res.nextBit());
            }
        }
    }
    
    /**
     * Tests that int and long output are same as Random.
     */
    public void test_RandomRNG_int_long_output() {
        Random ref = new Random(SEED);
        Random res = new RandomConcRNG(SEED);
        for (int i=0;i<1000;i++) {
            assertEquals(ref.nextInt(), res.nextInt());
            assertEquals(ref.nextLong(), res.nextLong());
        }
    }

    public void test_RandomRNGAdapter_Random() {
        
        /*
         * Exceptions.
         */
        
        try {
            new RandomRNGAdapter(null);
            assertTrue(false);
        } catch (NullPointerException e) {
            // ok
        }
        
        /*
         * Test that it doesn't set a seed into the specified Random.
         */
        
        for (long seed : new long[]{12345L,123456789L}) {
            Random ref = new Random(seed);
            Random impl = new Random(seed);
            Random res = new RandomRNGAdapter(impl);
            for (int i=0;i<1000;i++) {
                assertEquals(ref.nextInt(), res.nextInt());
            }
        }
    }
    
    public void test_RandomRNGAdapter_setSeed_long() {
        
        /*
         * Test that it calls backing Random's setSeed(long).
         */
        
        for (long seed : new long[]{12345L,123456789L}) {
            Random ref = new Random(seed);
            Random impl = new Random();
            Random res = new RandomRNGAdapter(impl);
            res.setSeed(seed);
            for (int i=0;i<1000;i++) {
                assertEquals(ref.nextInt(), res.nextInt());
            }
        }
    }
    
    public void test_MersenneTwisters_constructor_intArray_int() {
        for (boolean conc : new boolean[]{false,true}) {
            
            /*
             * Exceptions.
             */
            
            for (int keyLength : new int[]{Integer.MIN_VALUE,-1,0}) {
                try {
                    new_mt(conc, new int[1], keyLength);
                    assertTrue(false);
                } catch (IllegalArgumentException e) {
                    // ok
                }
            }
            
            try {
                new_mt(conc, null, 1);
                assertTrue(false);
            } catch (NullPointerException e) {
                // ok
            }
            
            /*
             * Test that gives same output than MT RNG identically seeded
             * but with setSeed(int[],int) method.
             */
            
            {
                final int[] initKey = new int[]{1,2,3,5,7,11};
                final int keyLength = initKey.length/2;
                
                final Random ref = new_mt(conc);
                setSeed_mt(ref, initKey, keyLength);
                
                final Random res = new_mt(conc, initKey, keyLength);
                
                for (int i=0;i<1000;i++) {
                    assertEquals(ref.nextInt(), res.nextInt());
                    assertEquals(ref.nextLong(), res.nextLong());
                }
            }
        }
    }
    
    public void test_MersenneTwisters_setSeed_intArray_int() {
        for (boolean conc : new boolean[]{false,true}) {
            final Random random = new_mt(conc);
            
            /*
             * Exceptions.
             */
            
            for (int keyLength : new int[]{Integer.MIN_VALUE,-1,0}) {
                try {
                    setSeed_mt(random, new int[1], keyLength);
                    assertTrue(false);
                } catch (IllegalArgumentException e) {
                    // ok
                }
            }
            
            try {
                setSeed_mt(random, null, 1);
                assertTrue(false);
            } catch (NullPointerException e) {
                // ok
            }
            
            /*
             * Test that clears stored bits.
             */
            
            {
                final Random storeless = new_mt(conc, SEED);
                final Random storeful = new_mt(conc, SEED);
                // Might cause stored bits.
                storeful.nextBoolean();
                // Must clear stored bits.
                setSeed_mt(storeful, new int[]{3*(int)SEED}, 1);
                setSeed_mt(storeless, new int[]{3*(int)SEED}, 1);
                for (int i=0;i<1000;i++) {
                    assertEquals(storeless.nextBoolean(), storeful.nextBoolean());
                }
            }
            
            /*
             * Sturdiness (long keys).
             */
            
            {
                final int[] initKey = new int[10*1000];
                for (int keyLength=1;keyLength<=initKey.length;keyLength*=2) {
                    setSeed_mt(random, initKey, keyLength);
                }
            }
        }
    }
    
    /**
     * Tests Mersenne-Twisters nextInt() output.
     */
    public void test_MersenneTwisters_nextInt() {
        for (boolean conc : new boolean[]{false,true}) {
            final Random random = new_mt(conc);
            
            /*
             * Output from
             * http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/MT2002/emt19937ar.html.
             * 
             * This output uses unsigned 32 integers, to for use with Java int,
             * which are signed, we might have to cast long value into int type.
             * 
             * Only checking first 10 and last 10 values among first 1000 values.
             */

            setSeed_mt(random, new int[]{0x123, 0x234, 0x345, 0x456}, 4);

            int[] expected_0001_to_0010 = new int[]{
                    1067595299, 955945823, 477289528, (int)4107218783L, (int)4228976476L,
                    (int)3344332714L, (int)3355579695L, 227628506, 810200273, (int)2591290167L};

            int[] expected_0991_to_1000 = new int[]{
                    988064871, (int)3515461600L, (int)4089077232L, (int)2225147448L, 1249609188,
                    (int)2643151863L, (int)3896204135L, (int)2416995901L, 1397735321, (int)3460025646L};

            int k;
            k = 0;
            for (int i=1;i<=10;i++) {
                assertEquals(expected_0001_to_0010[k++], random.nextInt());
            }
            for (int i=11;i<=990;i++) {
                random.nextInt();
            }
            k = 0;
            for (int i=991;i<=1000;i++) {
                assertEquals(expected_0991_to_1000[k++], random.nextInt());
            }
        }
    }

    /**
     * Tests than concurrent Mersenne-Twisters, when used sequentially,
     * give same output than sequential ones.
     * This also tests that stored bits are properly propagated from
     * a state object to the next in concurrent implementations.
     */
    public void test_MersenneTwisters_homogeneity_sequential() {
        final Random random = new Random(SEED);
        final MTSeqRNG seq = new MTSeqRNG(SEED);
        final MTConcRNG conc = new MTConcRNG(SEED);
        final int[] bitSizes = new int[]{1,8,16,32,64};
        for (int i=0;i<DEFAULT_MAX_NBR_OF_CALLS;i++) {
            final int bitSize = bitSizes[random.nextInt(bitSizes.length)];
            final long ref = nextBits(seq, bitSize);
            final long res = nextBits(conc, bitSize);
            assertEquals(ref, res);
        }
    }

    /**
     * Tests than concurrent Mersenne-Twisters, when used concurrently,
     * give same booleans, or same bytes, or same shorts, or same ints,
     * same or longs, than sequential ones.
     * For test not to use huge and heavy memory structures,
     * generated bits are only counted by chunks of 8 bits.
     * This tests that stored bits are properly propagated from
     * a state object to the next in concurrent implementations,
     * even in case of retries.
     */
    public void test_MersenneTwisters_homogeneity_concurrent() {
        final int nbrOfThreads = 4;
        final int nbrOfCallsPerThread = 1000*1000;

        for (final int bitSize : new int[]{1,8,16,32,64}) {
            final MTSeqRNG seq = new MTSeqRNG(SEED);
            final MTConcRNG conc = new MTConcRNG(SEED);

            // If bitSize <= 16, used treatment does storing,
            // so no need to pre-store.
            if (bitSize > 16) {
                // Calling nextBit() to have 31 bits stored.
                seq.nextBit();
                conc.nextBit();
            }

            final long[] refCounts = new long[256];
            final long[][] resCountsByThreadIndex = new long[nbrOfThreads][256];

            final int nbrOfSeqCalls = nbrOfCallsPerThread * nbrOfThreads;

            for (int i=0;i<nbrOfSeqCalls;i++) {
                final long bits = nextBits(seq, bitSize);
                for (int c=0;c<8;c++) {
                    refCounts[((byte)(bits>>(8*c))) + 128]++;
                }
            }

            final ExecutorService executor = Executors.newCachedThreadPool();
            for (int n=0;n<nbrOfThreads;n++) {
                final long[] threadResCounts = resCountsByThreadIndex[n];
                executor.execute(new Runnable() {
                    //@Override
                    public void run() {
                        for (int i=0;i<nbrOfCallsPerThread;i++) {
                            final long bits = nextBits(conc, bitSize);
                            for (int c=0;c<8;c++) {
                                threadResCounts[((byte)(bits>>(8*c))) + 128]++;
                            }
                        }
                    }
                });
            }
            TestUtils.shutdownAndAwaitTermination(executor);

            final long[] resCounts = new long[256];
            for (int n=0;n<nbrOfThreads;n++) {
                final long[] threadResCounts = resCountsByThreadIndex[n];
                for (int i=0;i<256;i++) {
                    resCounts[i] += threadResCounts[i];
                }
            }

            // Also tests stored bits propagation for bitSize <= 16.
            for (int i=0;i<256;i++) {
                final long ref = refCounts[i];
                final long res = resCounts[i];
                if (ref != res) {
                    System.out.println("bitSize = "+bitSize);
                    System.out.println("ref = "+ref);
                    System.out.println("res = "+res);
                    assertTrue(false);
                }
            }

            // Especially tests stored bits propagation for bitSize > 16.
            for (int i=0;i<32;i++) {
                final long ref = seq.nextBit();
                final long res = conc.nextBit();
                if (ref != res) {
                    System.out.println("bitSize = "+bitSize);
                    System.out.println("ref = "+ref);
                    System.out.println("res = "+res);
                    assertTrue(false);
                }
            }
        }
    }

    public void test_MTSeqRNG_split() {
        MTSeqRNG random1 = new MTSeqRNG(SEED);
        MTSeqRNG random2 = random1.split();
        
        // Small enough for test to be likely to success,
        // large enough to test a few values.
        final int nbrOfValues = 1000;
        HashSet<Integer> set1 = new HashSet<Integer>();
        for (int i=0;i<nbrOfValues;i++) {
            set1.add(random1.nextInt());
        }
        for (int i=0;i<nbrOfValues;i++) {
            assertFalse(set1.contains(random2.nextInt()));
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @param a A long value.
     * @return The specified value as int.
     * @throws ArithmeticException if the specified value is not in [Integer.MIN_VALUE,Integer.MAX_VALUE] range.
     */
    private static int asInt(long a) {
        if (a != (int)a) {
            throw new ArithmeticException("overflow: "+a);
        }
        return (int)a;
    }

    /**
     * @param a An int value.
     * @param b An int value.
     * @return The mathematical result of a*b.
     * @throws ArithmeticException if the mathematical result of a*b is not in [Integer.MIN_VALUE,Integer.MAX_VALUE] range.
     */
    private static int timesExact(int a, int b) {
        final long prod = a * (long)b;
        if (prod != (int)prod) {
            throw new ArithmeticException("overflow: "+a+"*"+b);
        }
        return (int)prod;
    }
    
    /*
     * 
     */

    /**
     * @return Random bits as LSBits, other bits being 0.
     */
    private static long nextBits(AbstractRNG rng, int bitSize) {
        final long bits;
        if (bitSize == 1) {
            bits = rng.nextBit();
        } else if (bitSize == 8) {
            bits = (rng.nextByte() & 0xFFL);
        } else if (bitSize == 16) {
            bits = (rng.nextShort() & 0xFFFFL);
        } else if (bitSize == 32) {
            bits = (rng.nextInt() & 0xFFFFFFFFL);
        } else if (bitSize == 64) {
            bits = rng.nextLong();
        } else {
            throw new AssertionError();
        }
        return bits;
    }

    /**
     * Does some checks.
     */
    private static int computeNbrOfCalls(int a, int b) {
        final int nbrOfCalls = timesExact(a, b);
        if (nbrOfCalls > DEFAULT_MAX_NBR_OF_CALLS) {
            throw new AssertionError(nbrOfCalls);
        }
        return nbrOfCalls;
    }

    private static boolean isAboutZero(double error, double magnitude) {
        final double relDelta = Math.abs(error/magnitude);
        final boolean ok = (relDelta < RELATIVE_TOLERANCE);
        if (!ok) {
            System.out.println("magnitude = "+magnitude);
            System.out.println("error = "+error);
            System.out.println("relDelta = "+relDelta);
        }
        return ok;
    }

    private static boolean isAboutEqual(long count, int magnitude) {
        return isAboutZero(count - magnitude, magnitude);
    }

    private static boolean isAboutHalf(long count, int magnitude) {
        return isAboutZero(count - magnitude/2, magnitude);
    }
    
    private static AbstractRNG new_mt(boolean conc) {
        return conc ? new MTConcRNG() : new MTSeqRNG();
    }
    
    private static AbstractRNG new_mt(boolean conc, long seed) {
        return conc ? new MTConcRNG(seed) : new MTSeqRNG(seed);
    }

    private static AbstractRNG new_mt(boolean conc, int[] initKey, int keyLength) {
        return conc ? new MTConcRNG(initKey, keyLength) : new MTSeqRNG(initKey, keyLength);
    }

    private static void setSeed_mt(Random random, int[] initKey, int keyLength) {
        if (random instanceof MTSeqRNG) {
            ((MTSeqRNG)random).setSeed(initKey, keyLength);
        } else {
            ((MTConcRNG)random).setSeed(initKey, keyLength);
        }
    }

    private static ArrayList<MyInterfaceRandomFactory> newFactories(boolean sequentialAllowed) {
        ArrayList<MyInterfaceRandomFactory> result = new ArrayList<MyInterfaceRandomFactory>();
        // Testing random, to test our tests.
        result.add(new MyInterfaceRandomFactory() {
            public Random newRandom(){return new Random();}
            public Random newRandom(long seed){return new Random(seed);}
            public Random newRandom(Void dummy){throw new UnsupportedOperationException();}
        });
        result.add(new MyInterfaceRandomFactory() {
            public Random newRandom(){return new RandomConcRNG();}
            public Random newRandom(long seed){return new RandomConcRNG(seed);}
            public Random newRandom(Void dummy){throw new UnsupportedOperationException();}
        });
        result.add(new MyInterfaceRandomFactory() {
            public Random newRandom(){return new RandomRNGAdapter(new Random());}
            public Random newRandom(long seed){return new RandomRNGAdapter(new Random(seed));}
            public Random newRandom(Void dummy){throw new UnsupportedOperationException();}
        });
        result.add(new MyInterfaceRandomFactory() {
            public Random newRandom(){return new MTConcRNG();}
            public Random newRandom(long seed){return new MTConcRNG(seed);}
            public Random newRandom(Void dummy){throw new UnsupportedOperationException();}
        });
        if (sequentialAllowed) {
            result.add(new MyInterfaceRandomFactory() {
                public Random newRandom(){return new MTSeqRNG();}
                public Random newRandom(long seed){return new MTSeqRNG(seed);}
                public Random newRandom(Void dummy){return new MTSeqRNG(dummy);}
            });
            result.add(new MyInterfaceRandomFactory() {
                public Random newRandom(){return new MXSIntSeqRNG();}
                public Random newRandom(long seed){return new MXSIntSeqRNG(seed);}
                public Random newRandom(Void dummy){throw new UnsupportedOperationException();}
            });
            result.add(new MyInterfaceRandomFactory() {
                public Random newRandom(){return new MXSLongSeqRNG();}
                public Random newRandom(long seed){return new MXSLongSeqRNG(seed);}
                public Random newRandom(Void dummy){throw new UnsupportedOperationException();}
            });
        }
        return result;
    }
}
