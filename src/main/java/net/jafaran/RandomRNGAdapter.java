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

/**
 * RNG based on a Random instance.
 * 
 * Is thread-safe if and only if the backing Random instance is.
 * 
 * Useful if needing to use an existing Random instance.
 * 
 * setSeed(long) calls backing Random's setSeed(long).
 * 
 * State get/set is not supported (no clean access to Random's state;
 * could call nextXXX, and then figure out what the seed was, and then
 * set it back, but would be messy).
 */
public class RandomRNGAdapter extends AbstractRNG {

    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;

    private final Random random;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param random Random to use.
     * @throws NullPointerException if the specified Random is null.
     */
    public RandomRNGAdapter(Random random) {
        // Super constructor that doesn't bother to set seed.
        super((Void)null);
        if (random == null) {
            throw new NullPointerException();
        }
        this.random = random;
    }

    @Override
    public int nextInt() {
        return this.random.nextInt();
    }

    @Override
    public long nextLong() {
        return this.random.nextLong();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setSeedImpl(long seed) {
        // No need to handle case where this.random is null,
        // since the constructor we use doesn't cause
        // current method to be called.
        this.random.setSeed(seed);
    }
}
