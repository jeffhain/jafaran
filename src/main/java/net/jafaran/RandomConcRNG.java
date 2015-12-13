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

import java.nio.ByteBuffer;

/**
 * Concurrent RNG using Random's algorithm, and providing
 * identical outputs for nextInt() and nextLong().
 * 
 * Useful if you need a concurrent RNG, but need speed or low memory usage
 * over the good randomness of MTConcRNG.
 */
public class RandomConcRNG extends AbstractRNG {
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L<<48)-1;
    
    private PostPaddedAtomicLong subSeed;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor using a random seed.
     */
    public RandomConcRNG() {
    }

    /**
     * Constructor using a specified seed.
     */
    public RandomConcRNG(long seed) {
        super(seed);
    }
    
    /*
     * 
     */

    @Override
    public int nextInt() {
        long oldseed;
        long nextseed;
        final PostPaddedAtomicLong seed = this.subSeed;
        do {
            oldseed = seed.get();
            nextseed = (oldseed * MULTIPLIER + ADDEND) & MASK;
        } while (!seed.compareAndSet(oldseed, nextseed));
        return (int)(nextseed >>> 16);
    }

    @Override
    public long nextLong() {
        // Overriding for performance, to avoid useless get+CAS.
        long seed0;
        long seed1;
        long seed2;
        final PostPaddedAtomicLong seed = this.subSeed;
        do {
            seed0 = seed.get();
            seed1 = (seed0 * MULTIPLIER + ADDEND) & MASK;
            seed2 = (seed1 * MULTIPLIER + ADDEND) & MASK;
        } while (!seed.compareAndSet(seed0, seed2));
        return ((seed1 >>> 16) << 32) + (int)(seed2 >>> 16);
    }
    
    /*
     * 
     */
    
    @Override
    public byte[] getState() {
        byte[] tab = new byte[8];
        // Big endian.
        ByteBuffer bb = ByteBuffer.wrap(tab);
        bb.putLong(this.subSeed.get());
        return tab;
    }

    @Override
    public void setState(byte[] state) {
        ByteBuffer bb = ByteBuffer.wrap(state);
        this.subSeed.set(bb.getLong());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void construct() {
        this.subSeed = new PostPaddedAtomicLong();
    }
    
    @Override
    protected void setSeedImpl(long seed) {
        this.subSeed.set(initialScramble(seed));
    }

    @Override
    protected int next(int bits) {
        // Overriding for performances, to avoid call to nextInt().
        long oldseed;
        long nextseed;
        final PostPaddedAtomicLong seed = this.subSeed;
        do {
            oldseed = seed.get();
            nextseed = (oldseed * MULTIPLIER + ADDEND) & MASK;
        } while (!seed.compareAndSet(oldseed, nextseed));
        return (int)(nextseed >>> (48 - bits));
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static long initialScramble(long seed) {
        return (seed ^ MULTIPLIER) & MASK;
    }
}
