/*
 * Copyright 2014-2015 Jeff Hain
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RandomsPerf {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int NBR_OF_PROC = Runtime.getRuntime().availableProcessors();
    private static final int CEILED_NBR_OF_PROC = ceilingPowerOfTwo(NBR_OF_PROC);
    
    private static final int MIN_PARALLELISM = 1;
    private static final int MAX_PARALLELISM = 2*CEILED_NBR_OF_PROC;

    private static final int NBR_OF_RUNS = 2;
    
    private static final int NBR_OF_CALLS = 10 * 1000 * 1000;
    
    /**
     * Because Random.nextGaussian(), called concurrently, can be very slow
     * with Java5.
     */
    private static final int LOOP_DIVISOR_FOR_CONC_GAUSSIAN = 100;
    
    private static final boolean BENCH_INT_PRIMITIVES = true;
    private static final boolean BENCH_INT_PRIMITIVES_RANGES = true;
    private static final boolean BENCH_FLOATING_POINTS = true;
    private static final boolean BENCH_GAUSSIANS = true;
    /**
     * To avoid log-and-time-spam with nextGaussian() and the like,
     * while we already have performances of the backing nextInt()
     * and nextLong().
     */
    private static final boolean IGNORE_SECONDARY_METHODS_ABOVE_2_THREADS = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Implementations must be stateless (can be used concurrently).
     */
    private static interface MyInterfaceUser {
        public String methodSignature();
        public void use(Random random, int nbrOfCalls);
    }
    
    private static class MyUser_nextBoolean implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextBoolean()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += (random.nextBoolean() ? 1 : 0);
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextBit implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextBit()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            final AbstractRNG rng = (AbstractRNG)random;
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += rng.nextBit();
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }
    
    private static class MyUser_nextByte implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextByte()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            final AbstractRNG impl = (AbstractRNG)random;
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += impl.nextByte();
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextShort implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextShort()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            final AbstractRNG rng = (AbstractRNG)random;
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += rng.nextShort();
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextInt implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextInt()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += random.nextInt();
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextLong implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextLong()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += (int)random.nextLong();
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextInt_int implements MyInterfaceUser {
        private final int maxExcl;
        public MyUser_nextInt_int(int maxExcl) {
            this.maxExcl = maxExcl;
        }
        //@Override
        public String methodSignature() {
            return "nextInt("+this.maxExcl+")";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += random.nextInt(this.maxExcl);
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextLong_long implements MyInterfaceUser {
        private final long maxExcl;
        public MyUser_nextLong_long(long maxExcl) {
            this.maxExcl = maxExcl;
        }
        //@Override
        public String methodSignature() {
            return "nextLong("+this.maxExcl+")";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            final AbstractRNG rng = (AbstractRNG)random;
            int dummy = Integer.MIN_VALUE;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += (int)rng.nextLong(this.maxExcl);
            }
            if (dummy == 0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextFloat implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextFloat()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            float dummy = 0.0f;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += random.nextFloat();
            }
            if (dummy == 0.0f) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextDouble implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextDouble()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            double dummy = 0.0;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += random.nextDouble();
            }
            if (dummy == 0.0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextDoubleFast implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextDoubleFast()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            final AbstractRNG rng = (AbstractRNG)random;
            double dummy = 0.0;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += rng.nextDoubleFast();
            }
            if (dummy == 0.0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextGaussian implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextGaussian()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            double dummy = 0.0;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += random.nextGaussian();
            }
            if (dummy == 0.0) {
                System.out.println("rare");
            }
        }
    }

    private static class MyUser_nextGaussianFast implements MyInterfaceUser {
        //@Override
        public String methodSignature() {
            return "nextGaussianFast()";
        }
        //@Override
        public void use(Random random, int nbrOfCalls) {
            final AbstractRNG rng = (AbstractRNG)random;
            double dummy = 0.0;
            for (int i=0;i<nbrOfCalls;i++) {
                dummy += rng.nextGaussianFast();
            }
            if (dummy == 0.0) {
                System.out.println("rare");
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final Random SEQ_PILL = new Random();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println(TestUtils.getJVMInfo());
        newRun(args);
    }

    public static void newRun(String[] args) {
        new RandomsPerf().run(args);
    }
    
    public RandomsPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @param a A value in [0,2^30].
     * @return The lowest power of two >= a.
     */
    private static int ceilingPowerOfTwo(int a) {
        return (a >= 2) ? Integer.highestOneBit((a-1)<<1) : 1;
    }
    
    /*
     * 
     */
    
    private static boolean handlePill(Random pillOrNot) {
        if (pillOrNot == SEQ_PILL) {
            System.out.println("seq:");
            return true;
        } else {
            return false;
        }
    }
    
    private void run(String[] args) {
        // XXX
        System.out.println("--- "+RandomsPerf.class.getSimpleName()+"... ---");
        System.out.println("number of calls = "+NBR_OF_CALLS);

        for (boolean concurrent : new boolean[]{false,true}) {
            final int minNbrOfThreads = Math.max(concurrent ? 2 : 1, MIN_PARALLELISM);
            final int maxNbrOfThreads = Math.min(concurrent ? MAX_PARALLELISM : 1, MAX_PARALLELISM);
            final boolean sequentialAllowed = (!concurrent);
            
            for (int nbrOfThreads=minNbrOfThreads;nbrOfThreads<=maxNbrOfThreads;nbrOfThreads*=2) {

                if (BENCH_INT_PRIMITIVES) {
                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextBoolean());
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        if (random instanceof AbstractRNG) {
                            bench(random, nbrOfThreads, new MyUser_nextBit());
                        }
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        if (random instanceof AbstractRNG) {
                            bench(random, nbrOfThreads, new MyUser_nextByte());
                        }
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        if (random instanceof AbstractRNG) {
                            bench(random, nbrOfThreads, new MyUser_nextShort());
                        }
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextInt());
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextLong());
                    }
                }
                
                /*
                 * 
                 */
                
                if (IGNORE_SECONDARY_METHODS_ABOVE_2_THREADS && (nbrOfThreads > 2)) {
                    continue;
                }

                /*
                 * 
                 */
                
                if (BENCH_INT_PRIMITIVES_RANGES) {
                    // Tiny.
                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextInt_int(7));
                    }

                    // Power of two.
                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextInt_int(1<<30));
                    }

                    // Tiny.
                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        if (random instanceof AbstractRNG) {
                            bench(random, nbrOfThreads, new MyUser_nextLong_long(7L));
                        }
                    }

                    // Tiny not in int range.
                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        if (random instanceof AbstractRNG) {
                            bench(random, nbrOfThreads, new MyUser_nextLong_long(Integer.MAX_VALUE+8L));
                        }
                    }
                }

                /*
                 * 
                 */
                
                if (BENCH_FLOATING_POINTS) {
                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextFloat());
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextDouble());
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        if (random instanceof AbstractRNG) {
                            bench(random, nbrOfThreads, new MyUser_nextDoubleFast());
                        }
                    }
                }

                /*
                 * 
                 */

                if (BENCH_GAUSSIANS) {
                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        bench(random, nbrOfThreads, new MyUser_nextGaussian(), ((nbrOfThreads > 1) ? LOOP_DIVISOR_FOR_CONC_GAUSSIAN : 1));
                    }

                    System.out.println();
                    for (Random random : newRandoms(sequentialAllowed)) {
                        if(handlePill(random))continue;
                        if (random instanceof AbstractRNG) {
                            bench(random, nbrOfThreads, new MyUser_nextGaussianFast());
                        }
                    }
                }
            }
        }

        System.out.println("--- ..."+RandomsPerf.class.getSimpleName()+" ---");
    }
    
    private void bench(
            final Random random,
            int nbrOfThreads,
            final MyInterfaceUser user) {
        final int loopDivisor = 1;
        bench(
                random,
                nbrOfThreads,
                user,
                loopDivisor);
    }
    
    /**
     * @param loopDivisor To run less loops, for slow cases.
     */
    private void bench(
            final Random random,
            int nbrOfThreads,
            final MyInterfaceUser user,
            int loopDivisor) {
        TestUtils.settle();
        
        final String csn = random.getClass().getSimpleName();
        
        final int usedNbrOfCalls = NBR_OF_CALLS/loopDivisor;
        
        final int nbrOfCallsPerThread = usedNbrOfCalls/nbrOfThreads;

        // For warmup (class load and code optim).
        user.use(random, NBR_OF_CALLS);
        
        for (int k=0;k<NBR_OF_RUNS;k++) {
            long a = System.nanoTime();
            if (nbrOfThreads == 1) {
                // Taking care to use current thread,
                // in case ThreadLocalRandom would not like otherwise.
                user.use(random, nbrOfCallsPerThread);
            } else {
                final ExecutorService executor = Executors.newCachedThreadPool();
                for (int i=0;i<nbrOfThreads;i++) {
                    executor.execute(new Runnable() {
                        //@Override
                        public void run() {
                            user.use(random, nbrOfCallsPerThread);
                        }
                    });
                }
                TestUtils.shutdownAndAwaitTermination(executor);
            }
            long b = System.nanoTime();
            final String loopDivision = ((loopDivisor == 1) ? "" : "(/"+loopDivisor+")");
            System.out.println("Loop"+loopDivision+" on "+csn+"."+user.methodSignature()+", "+nbrOfThreads+" caller(s), took "+TestUtils.nsToSRounded(b-a)+" s");
        }
    }
    
    private static ArrayList<Random> newRandoms(boolean sequentialAllowed) {
        ArrayList<Random> result = new ArrayList<Random>();
        result.add(new Random());
        result.add(new RandomConcRNG());
        result.add(new MTConcRNG());
        result.add(new MTSyncRNG());
        if (sequentialAllowed) {
            result.add(SEQ_PILL);
            // TODO Java7 result.add(ThreadLocalRandom.current());
            result.add(new MTSeqRNG());
            result.add(new MXSIntSeqRNG());
            result.add(new MXSLongSeqRNG());
        }
        return result;
    }
}
