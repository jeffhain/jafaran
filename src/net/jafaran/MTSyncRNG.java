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

/**
 * Thread-safe and blocking.
 * 
 * Can be more efficient than MTConcRNG (no garbage, no CAS-retries, low
 * synchronization overhead).
 * 
 * RNG based on Mersenne-Twister algorithm.
 */
public class MTSyncRNG extends SyncRNG {
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor using a random seed.
     */
    public MTSyncRNG() {
        super(new MTSeqRNG());
    }

    /**
     * Constructor using a specified seed.
     * 
     * Only the 32 LSBits of the specified seed are used (as in the original
     * implementation).
     */
    public MTSyncRNG(long seed) {
        super(new MTSeqRNG(seed));
    }

    /**
     * @throws IllegalArgumentException if keyLength <= 0.
     */
    public MTSyncRNG(int[] initKey, int keyLength) {
        super(new MTSeqRNG(initKey, keyLength));
    }

    /**
     * Similar to SplittableRandom.split().
     * 
     * @return A new instance, seeded with random bits from this instance.
     */
    public MTSyncRNG split() {
        final MTSeqRNG rng = (MTSeqRNG)this.getBackingRNG();
        final MTSeqRNG newRng;
        synchronized (rng) {
            newRng = rng.split();
        }
        return new MTSyncRNG(newRng);
    }
    
    /**
     * @throws IllegalArgumentException if keyLength <= 0.
     */
    public void setSeed(int[] initKey, int keyLength) {
        final MTSeqRNG rng = (MTSeqRNG)this.getBackingRNG();
        synchronized (rng) {
            rng.setSeed(initKey, keyLength);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates an instance backed by the specified RNG.
     */
    private MTSyncRNG(MTSeqRNG rng) {
        super(rng);
    }
}
