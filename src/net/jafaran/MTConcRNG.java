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
 * Concurrent RNG based on Mersenne-Twister algorithm.
 * 
 * When used sequentially, gives same output than MTSeqRNG.
 * 
 * State is stored in objects that are garbaged once exhausted.
 * For no garbage, a (thread-)local instance of MTSeqRNG
 * can be used instead (which should also be faster).
 */
public class MTConcRNG extends AbstractRNG {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Atomic long contains:
     * - 10 LSBits: mti (unsigned in [0,1023])
     * - 6 next bits: number of stored bits (unsigned in [0,63])
     * - 16 next bits: unused
     * - 32 MSBits: stored bits
     * 
     * Could have a volatile (or not?) reference to next
     * state holder, which could prevent useless next state
     * computations by concurrent threads, but that might cause
     * memory leak if a thread blocks with a reference to an old
     * state holder.
     */
    private static class MyStateHolder extends PostPaddedAtomicLong {
        private static final long serialVersionUID = 1L;
        final int[] mt = new int[N+1];
        /**
         * Creates an invalid state.
         */
        public MyStateHolder() {
            super(meta(0,0,INVALID_MTI));
        }
    }
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    private static final int N = MTUtils.N;
    private static final int TEMPERING_MASK_B = MTUtils.TEMPERING_MASK_B;
    private static final int TEMPERING_MASK_C = MTUtils.TEMPERING_MASK_C;

    /**
     * Generates exception if used prior to initialization,
     * which allows us not to eventually ensure initialization
     * when mti is 0.
     */
    private static final int INVALID_MTI = N+1;

    private static final int MTI_BIT_SIZE = 10;
    private static final int NBR_OF_STORED_BITS_BIT_SIZE = 6;
    
    /**
     * Not final because constructed in construct().
     */
    private PostPaddedAtomicReference<MyStateHolder> holderRef;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor using a random seed.
     */
    public MTConcRNG() {
    }

    /**
     * Constructor using a specified seed.
     * 
     * Only the 32 LSBits of the specified seed are used (as in the original
     * implementation).
     */
    public MTConcRNG(long seed) {
        super(seed);
    }
    
    /**
     * @throws IllegalArgumentException if keyLength <= 0.
     */
    public MTConcRNG(int[] initKey, int keyLength) {
        super((Void)null);
        this.setSeed(initKey, keyLength);
    }

    /**
     * @throws IllegalArgumentException if keyLength <= 0.
     */
    public void setSeed(int[] initKey, int keyLength) {
        final MyStateHolder holder = new MyStateHolder();
        final int mti = MTUtils.setSeed(holder.mt, initKey, keyLength);
        holder.set(meta(0,0,mti));
        this.holderRef.set(holder);
    }

    @Override
    public int nextInt() {
        MyStateHolder newHolder = null;
        MyStateHolder holder = this.holderRef.get();
        do {
            final long meta = holder.get();

            // Getting random bits.
            final int mti = mti(meta);
            int s;
            int newMti;
            if (mti == 0) {
                // Can't generate more random bits from this holder.
                // Creating a new holder, including current stored bits if any.
                if (newHolder == null) {
                    newHolder = new MyStateHolder();
                }
                System.arraycopy(holder.mt, 0, newHolder.mt, 0, N+1);
                s = MTUtils.toNextState(newHolder.mt);
                newMti = N-1;
            } else {
                s = holder.mt[mti];
                newMti = mti-1;
            }

            // Tempering.
            s ^= (s>>>11);
            s ^= ((s<<7) & TEMPERING_MASK_B);
            s ^= ((s<<15) & TEMPERING_MASK_C);
            s ^= (s>>>18);

            final long newMeta = metaMtiUpdate(meta, newMti);

            if (mti == 0) {
                if (setNewMetaAndCompareAndSet(newMeta, this.holderRef, holder, newHolder)) {
                    return s;
                }
                // Since holder get, someone else successfully used this holder:
                // will try to use current one.
                holder = this.holderRef.get();
            } else {
                if (holder.compareAndSet(meta, newMeta)) {
                    return s;
                }
                // Since meta get, someone else successfully used this holder:
                // will try to use it again.
            }
        } while (true);
    }
    
    @Override
    public long nextLong() {
        // Overriding to avoid encountering concurrency overhead twice.
        MyStateHolder newHolder = null;
        MyStateHolder holder = this.holderRef.get();
        do {
            final long meta = holder.get();
            final int mti = mti(meta);

            long result = 0L;
            
            int newMti = mti;
            
            boolean usedNewHolder = false;
            for (int i=0;i<2;i++) {
                // Getting random bits.
                int s;
                if (newMti == 0) {
                    // Can't generate more random bits from this holder.
                    // Creating a new holder, including current stored bits if
                    // any.
                    if (newHolder == null) {
                        newHolder = new MyStateHolder();
                    }
                    System.arraycopy(holder.mt, 0, newHolder.mt, 0, N+1);
                    s = MTUtils.toNextState(newHolder.mt);
                    newMti = N-1;
                    usedNewHolder = true;
                } else {
                    if (usedNewHolder) {
                        s = newHolder.mt[newMti];
                    } else {
                        s = holder.mt[newMti];
                    }
                    newMti = newMti-1;
                }

                // Tempering.
                s ^= (s>>>11);
                s ^= ((s<<7) & TEMPERING_MASK_B);
                s ^= ((s<<15) & TEMPERING_MASK_C);
                s ^= (s>>>18);

                result = (result<<32) + s;
            }

            final long newMeta = metaMtiUpdate(meta, newMti);

            if (usedNewHolder) {
                if (setNewMetaAndCompareAndSet(newMeta, this.holderRef, holder, newHolder)) {
                    return result;
                }
                // Since holder get, someone else successfully used this
                // holder: will try to use current one.
                holder = this.holderRef.get();
            } else {
                if (holder.compareAndSet(meta, newMeta)) {
                    return result;
                }
                // Since meta get, someone else successfully used this holder:
                // will try to use it again.
            }
        } while (true);
    }
    
    /*
     * 
     */
    
    @Override
    public byte[] getState() {
        final MyStateHolder holder = this.holderRef.get();
        byte[] tab = new byte[4*holder.mt.length + 4 + RandomUtilz.getEncodingByteSizeForStoredBits()];
        // Big endian.
        ByteBuffer bb = ByteBuffer.wrap(tab);
        // Putting mt in original order, for easier comparison
        // with other implementations that would not reverse
        // order as we do.
        for (int i=holder.mt.length;--i>=0;) {
            bb.putInt(holder.mt[i]);
        }
        long meta = holder.get();
        bb.putInt(mti(meta));
        RandomUtilz.encodeNbrOfStoredBits(nbrOfStoredBits(meta), bb);
        RandomUtilz.encodeStoredBits(storedBits(meta), bb);
        return tab;
    }

    @Override
    public void setState(byte[] state) {
        ByteBuffer bb = ByteBuffer.wrap(state);
        final MyStateHolder holder = new MyStateHolder();
        for (int i=holder.mt.length;--i>=0;) {
            holder.mt[i] = bb.getInt();
        }
        int mti = bb.getInt();
        int currentNbrOfStoredBits = RandomUtilz.decodeNbrOfStoredBits(bb);
        int currentStoredBits = RandomUtilz.decodeStoredBits(bb);
        holder.set(meta(currentStoredBits,currentNbrOfStoredBits,mti));
        this.holderRef.set(holder);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void construct() {
        // Can have null holder here, since must be initialized in any public
        // constructor, i.e. before use.
        this.holderRef = new PostPaddedAtomicReference<MyStateHolder>(null);
    }

    @Override
    protected void setSeedImpl(long seed) {
        final MyStateHolder holder = new MyStateHolder();
        final int mti = MTUtils.setSeed(holder.mt, seed);
        holder.set(meta(0,0,mti));
        this.holderRef.set(holder);
    }

    @Override
    protected int next(int nbrOfBits) {
        MyStateHolder newHolder = null;
        MyStateHolder holder = this.holderRef.get();
        do {
            final long meta = holder.get();
            
            final int storedBits = storedBits(meta);
            final int nbrOfStoredBits = nbrOfStoredBits(meta);
            
            if (nbrOfBits <= nbrOfStoredBits) {
                // Enough stored bits, and worth it.
                final int newNbrOfStoredBits = (nbrOfStoredBits-nbrOfBits);
                final int result = (storedBits>>>newNbrOfStoredBits);
                final int newStoredBits = ((storedBits & ((1<<newNbrOfStoredBits)-1)));
                final long newMeta = metaStoredBitsUpdate(meta, newStoredBits, newNbrOfStoredBits);
                if (holder.compareAndSet(meta, newMeta)) {
                    return result;
                }
                // Since meta get, someone else successfully used this holder:
                // will try to use it again.
            } else {
                // Not enough stored bits: getting more random bits.
                final int mti = mti(meta);
                int s;
                int newMti;
                if (mti == 0) {
                    // Can't generate more random bits from this holder.
                    // Creating a new holder, including current stored bits if
                    // any.
                    if (newHolder == null) {
                        newHolder = new MyStateHolder();
                    }
                    System.arraycopy(holder.mt, 0, newHolder.mt, 0, N+1);
                    s = MTUtils.toNextState(newHolder.mt);
                    newMti = N-1;
                } else {
                    s = holder.mt[mti];
                    newMti = mti-1;
                }
                
                // Tempering.
                s ^= (s>>>11);
                s ^= ((s<<7) & TEMPERING_MASK_B);
                s ^= ((s<<15) & TEMPERING_MASK_C);
                s ^= (s>>>18);
                
                // number of new random bits to add to the value
                final int nbrOfNewBitsUsed = nbrOfBits - nbrOfStoredBits;
                final int newNbrOfStoredBits = (32-nbrOfNewBitsUsed);
                // Using stored bits as MSBits, and MSBits of new bits as
                // LSBits.
                final int result = (storedBits<<nbrOfNewBitsUsed) | (s>>>newNbrOfStoredBits);

                final int newStoredBits = ((s & ((1<<newNbrOfStoredBits)-1)));
                final long newMeta = meta(newStoredBits, newNbrOfStoredBits, newMti);
                
                if (mti == 0) {
                    if (setNewMetaAndCompareAndSet(newMeta, this.holderRef, holder, newHolder)) {
                        return result;
                    }
                    // Since holder get, someone else successfully used this
                    // holder: will try to use current one.
                    holder = this.holderRef.get();
                } else {
                    if (holder.compareAndSet(meta, newMeta)) {
                        return result;
                    }
                    // Since meta get, someone else successfully used this
                    // holder: will try to use it again.
                }
            }
        } while (true);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static boolean setNewMetaAndCompareAndSet(
            long newMeta,
            PostPaddedAtomicReference<MyStateHolder> holderRef,
            MyStateHolder holder,
            MyStateHolder newHolder) {
        // Lazy ok for this holder is only known by current thread
        // in calling method, and a CAS follows.
        newHolder.set(newMeta); // TODO Java6 lazySet
        return holderRef.compareAndSet(holder, newHolder);
    }
    
    /*
     * 
     */
    
    private static long meta(int storedBits, int nbrOfStoredBits, int mti) {
        return (((long)storedBits)<<32) | (nbrOfStoredBits<<MTI_BIT_SIZE) | mti;
    }
    
    /**
     * To avoid having to retrieve mti to compute new meta.
     */
    private static long metaStoredBitsUpdate(long meta, int storedBits, int nbrOfStoredBits) {
        return (meta & (-1L>>>(64-MTI_BIT_SIZE))) | (((long)storedBits)<<32) | (nbrOfStoredBits<<MTI_BIT_SIZE);
    }

    /**
     * To avoid having to retrieve stored bits to compute new meta.
     */
    private static long metaMtiUpdate(long meta, int mti) {
        return (meta & (-1L<<MTI_BIT_SIZE)) | mti;
    }

    private static int storedBits(long meta) {
        return (int)(meta>>32);
    }
    
    private static int nbrOfStoredBits(long meta) {
        return (((int)meta)>>MTI_BIT_SIZE) & ((1<<NBR_OF_STORED_BITS_BIT_SIZE)-1);
    }
    
    private static int mti(long meta) {
        return (((int)meta) & ((1<<MTI_BIT_SIZE)-1));
    }
}
