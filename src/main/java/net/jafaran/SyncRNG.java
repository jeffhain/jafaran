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
 * Thread-safe RNG, as a wrapper synchronizing on a backing (and typically
 * sequential) RNG.
 * 
 * Can be more efficient than concurrent RNGs using CAS and such (no garbage,
 * no CAS-retries, low synchronization overhead).
 */
public class SyncRNG extends AbstractRNG {
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Guarded by synchronization on itself.
     */
    private final AbstractRNG rng;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param rng The backing (and typically sequential) RNG instance.
     */
    public SyncRNG(AbstractRNG rng) {
        super(0L); // Avoiding seed generation.
        if (rng == null) {
            throw new NullPointerException();
        }
        this.rng = rng;
    }

    /*
     * uniform
     */
    
    @Override
    public int nextBit() {
        synchronized (this.rng) {
            return this.rng.nextBit();
        }
    }

    @Override
    public byte nextByte() {
        synchronized (this.rng) {
            return this.rng.nextByte();
        }
    }
    
    @Override
    public short nextShort() {
        synchronized (this.rng) {
            return this.rng.nextShort();
        }
    }

    @Override
    public int nextInt() {
        synchronized (this.rng) {
            return this.rng.nextInt();
        }
    }
    
    @Override
    public long nextLong() {
        synchronized (this.rng) {
            return this.rng.nextLong();
        }
    }
    
    @Override
    public float nextFloat() {
        synchronized (this.rng) {
            return this.rng.nextFloat();
        }
    }

    @Override
    public double nextDouble() {
        synchronized (this.rng) {
            return this.rng.nextDouble();
        }
    }

    @Override
    public double nextDoubleFast() {
        synchronized (this.rng) {
            return this.rng.nextDoubleFast();
        }
    }

    @Override
    public int nextInt(int n) {
        synchronized (this.rng) {
            return this.rng.nextInt(n);
        }
    }
    
    @Override
    public long nextLong(long n) {
        synchronized (this.rng) {
            return this.rng.nextLong(n);
        }
    }
    
    /*
     * gaussian
     */

    @Override
    public double nextGaussian() {
        synchronized (this.rng) {
            return this.rng.nextGaussian();
        }
    }

    @Override
    public double nextGaussianFast() {
        synchronized (this.rng) {
            return this.rng.nextGaussianFast();
        }
    }
    
    /*
     * state get/set
     */
    
    @Override
    public byte[] getState() {
        synchronized (this.rng) {
            return this.rng.getState();
        }
    }

    @Override
    public void setState(byte[] state) {
        synchronized (this.rng) {
            this.rng.setState(state);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected final void construct() {
    }

    @Override
    protected final void setSeedImpl(long seed) {
        final AbstractRNG rng = this.rng;
        if (rng == null) {
            // Call from super constructor.
            return;
        }
        synchronized (rng) {
            // We can bypass setSeed(long), since we know that the object has
            // been created (and set).
            rng.setSeedImpl(seed);
        }
    }
    
    /**
     * @return The backing RNG, or null if called before it is set (which can't
     *         happen for extending classes, since construct() and
     *         setSeedImpl(long) are final).
     */
    protected AbstractRNG getBackingRNG() {
        return this.rng;
    }
}
