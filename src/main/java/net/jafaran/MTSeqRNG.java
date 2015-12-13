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

import java.nio.ByteBuffer;

/**
 * Not thread-safe.
 * 
 * RNG based on Mersenne-Twister algorithm.
 */
public class MTSeqRNG extends AbstractSeqRNG {
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    private static final int N = MTUtils.N;

    /**
     * Generates exception if used prior to initialization,
     * which allows us not to eventually ensure initialization
     * when mti is 0.
     */
    private static final int INVALID_MTI = N+1;

    /**
     * Not final because constructed in construct().
     */
    private int[] mt;
    private int mti;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor using a random seed.
     */
    public MTSeqRNG() {
    }

    /**
     * Constructor using a specified seed.
     * 
     * Only the 32 LSBits of the specified seed are used (as in the original
     * implementation).
     */
    public MTSeqRNG(long seed) {
        super(seed);
    }

    /**
     * @throws IllegalArgumentException if keyLength <= 0.
     */
    public MTSeqRNG(int[] initKey, int keyLength) {
        super((Void)null);
        this.setSeed_noBitsClearing(initKey, keyLength);
    }

    /**
     * Similar to SplittableRandom.split().
     * 
     * @return A new instance, seeded with random bits from this instance.
     */
    public MTSeqRNG split() {
        // 128 pseudo-random bits should be enough to produce
        // a sufficiently different and likely enough unique
        // new PRNG.
        final int keyLength = 4;
        final int[] initKey = new int[keyLength];
        for (int i=0;i<keyLength;i++) {
            initKey[i] = this.nextInt();
        }
        return new MTSeqRNG(initKey, keyLength);
    }
    
    /**
     * @throws IllegalArgumentException if keyLength <= 0.
     */
    public void setSeed(int[] initKey, int keyLength) {
        super.setSeedImpl(0L);
        this.setSeed_noBitsClearing(initKey, keyLength);
    }
    
    @Override
    public int nextInt() {
        final int s;
        if (this.mti != 0) {
            s = this.mt[this.mti--];
        } else {
            this.mti = N-1;
            s = MTUtils.toNextState(this.mt);
        }
        return MTUtils.tempered(s);
    }

    /*
     * 
     */
    
    @Override
    public byte[] getState() {
        byte[] tab = new byte[4*this.mt.length + 4 + RandomUtilz.getEncodingByteSizeForStoredBits()];
        // Big endian.
        ByteBuffer bb = ByteBuffer.wrap(tab);
        // Putting mt in original order, for easier comparison
        // with other implementations that would not reverse
        // order as we do.
        for (int i=this.mt.length;--i>=0;) {
            bb.putInt(this.mt[i]);
        }
        bb.putInt(this.mti);
        RandomUtilz.encodeNbrOfStoredBits(this.getCurrentNbrOfStoredBits(), bb);
        RandomUtilz.encodeStoredBits(this.getCurrentStoredBits(), bb);
        return tab;
    }

    @Override
    public void setState(byte[] state) {
        ByteBuffer bb = ByteBuffer.wrap(state);
        for (int i=this.mt.length;--i>=0;) {
            this.mt[i] = bb.getInt();
        }
        this.mti = bb.getInt();
        this.setCurrentNbrOfStoredBits(RandomUtilz.decodeNbrOfStoredBits(bb));
        this.setCurrentStoredBits(RandomUtilz.decodeStoredBits(bb));
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected MTSeqRNG(Void dummy) {
        super(dummy);
    }

    @Override
    protected void construct() {
        this.mt = new int[N+1];
        this.mti = INVALID_MTI;
    }

    @Override
    protected void setSeedImpl(long seed) {
        super.setSeedImpl(0L);
        this.mti = MTUtils.setSeed(this.mt, seed);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void setSeed_noBitsClearing(int[] initKey, int keyLength) {
        this.mti = MTUtils.setSeed(this.mt, initKey, keyLength);
    }
}
