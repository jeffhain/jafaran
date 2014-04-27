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

/**
 * Provides fast, and for some more random, implementations of java.util.Random,
 * with additional nextXXX() methods, and methods to retrieve and restore state.
 * 
 * The names of implementations contain "Conc" (for concurrent) if they are
 * thread-safe, or "Seq" (for sequential) if they are not.
 * 
 * Also provides a Random-based implementation of Ziggurat algorithm, used by
 * nextGaussian() methods of the provided implementations.
 * 
 * Principal classes:
 * - Implementations using Mersenne-Twister algorithm (good pseudo-randomness):
 *   - MTConcRNG
 *   - MTSeqRNG
 * - Implementations using Marsaglia Xor-Shift (fast):
 *   - MXSIntSeqRNG (32 bits)
 *   - MXSLongSeqRNG (64 bits) (nextLong() faster, larger period)
 * - RandomConcRNG: Pseudo-RNG using the same algorithm than Random.
 * - RandomRNGAdapter: Pseudo-RNG backed by a Random instance.
 * - Ziggurat: Random-based implementation of Ziggurat algorithm.
 */
package net.jafaran;
