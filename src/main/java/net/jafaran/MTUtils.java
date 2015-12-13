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
/*
 * =============================================================================
 * License of the C implementation this one is derived from:
 *
 * A C-program for MT19937, with initialization improved 2002/1/26.
 * Coded by Takuji Nishimura and Makoto Matsumoto.
 *
 * Before using, initialize the state by using init_genrand(seed)  
 * or init_by_array(init_key, key_length).
 *
 * Copyright (C) 1997 - 2002, Makoto Matsumoto and Takuji Nishimura,
 * All rights reserved.                          
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 *   3. The names of its contributors may not be used to endorse or promote 
 *      products derived from this software without specific prior written 
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Any feedback is very welcome.
 * http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html
 * email: m-mat @ math.sci.hiroshima-u.ac.jp (remove space)
 */
package net.jafaran;

/**
 * Mersenne-Twister stuffs
 * (http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html).
 * 
 * Not public, as we use multiple hacks to make things faster:
 * - Reversing state vector elements order to allow for comparison against 0
 *   instead of N.
 * - State vector being reversed, it would need to be regenerated not when mti
 *   is N but when it is -1, i.e. negative.
 *   To allow for mti to always be >= 0, which can be handy when it comes
 *   to store it for example as bits in a long, we make state vector size
 *   N+1 instead of N, and never use the int at index 0.
 *   As a result, index "i" in original implementation becomes "N-i" for us,
 *   and "mti >= N" becomes "mti <= 0" (or "mti == 0").
 * 
 * Also, we use signed ints, so >>> instead of >>.
 */
class MTUtils {
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    static final int N = 624; // Length (-1 for us) of state vector.
    private static final int M = 397; // Period parameter.
    private static final int MAGIC_NUMBER = 1812433253;
    private static final int MATRIX_A = 0x9908B0DF;
    private static final int UPPER_MASK = 0x80000000;
    private static final int LOWER_MASK = 0x7FFFFFFF;
    private static final int TEMPERING_MASK_B = 0x9D2C5680;
    private static final int TEMPERING_MASK_C = 0xEFC60000;
    
    private static final int BIG_SEED_LITTLE_SEED = 19650218;
    private static final int BIG_SEED_FACTOR_1 = 1664525;
    private static final int BIG_SEED_FACTOR_2 = 1566083941;

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @return New mti.
     */
    static int setSeed(int[] mt, long seed) {
        // No need to do "& 0xFFFFFFFFL" as in original C code,
        // since the cast into int gets rid of the 32 MSBits
        // already.
        mt[N] = (int)seed;
        for (int i=N;--i>0;) {
            mt[i] = (MAGIC_NUMBER * (mt[i+1] ^ (mt[i+1] >>> 30)) + (N-i));
        }
        return 0;
    }

    /**
     * @return New mti.
     * @throws IllegalArgumentException if keyLength <= 0.
     */
    static int setSeed(
            int[] mt,
            int[] initKey,
            int keyLength) {
        if (keyLength <= 0) {
            throw new IllegalArgumentException();
        }
        final int newCond = setSeed(mt, BIG_SEED_LITTLE_SEED);

        int i = N-1;
        int j = 0;
        int k;
        for (k=Math.max(keyLength,N);k>0;k--) {
            mt[i] = (mt[i] ^ ((mt[i+1] ^ (mt[i+1] >>> 30)) * BIG_SEED_FACTOR_1)) + initKey[j] + j;
            if (--i == 0) {
                mt[N] = mt[1];
                i = N-1;
            }
            if (++j == keyLength) {
                j = 0;
            }
        }
        for (k=N-1;k>0;k--) {
            mt[i] = (mt[i] ^ ((mt[i+1] ^ (mt[i+1] >>> 30)) * BIG_SEED_FACTOR_2)) - (N-i);
            if (--i == 0) {
                mt[N] = mt[1];
                i = N-1;
            }
        }
        // Ensuring non-zero initial array.
        mt[N] = 0x80000000;
        
        return newCond;
    }

    /**
     * @return Value to temper, which is extracted from the new array content.
     */
    static int toNextState(int[] mt) {
        int s;
        int j;
        for (j=N+1;--j>M;) { // N..M+1 : N-M rounds
            s = (mt[j] & UPPER_MASK) | (mt[j-1] & LOWER_MASK);
            mt[j] = (mt[j-M] ^ (s>>>1)) ^ ((s & 1) * MATRIX_A);
        }
        for (j=M+1;--j>1;) { // M..2 : M-1 rounds
            s = (mt[j] & UPPER_MASK) | (mt[j-1] & LOWER_MASK);
            mt[j] = (mt[j+(N-M)] ^ (s>>>1)) ^ ((s & 1) * MATRIX_A);
        }
        s = (mt[1] & UPPER_MASK) | (mt[N] & LOWER_MASK);
        mt[1] = (mt[N-M+1] ^ (s>>>1)) ^ ((s & 1) * MATRIX_A);
        return mt[N];
    }
    
    static int tempered(int s) {
        s ^= (s>>>11);
        s ^= ((s<<7) & TEMPERING_MASK_B);
        s ^= ((s<<15) & TEMPERING_MASK_C);
        return s ^ (s>>>18);
    }
}
