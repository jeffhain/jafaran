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

import junit.framework.TestCase;

public class GaussianTesterTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final boolean USE_RANDOM_SEED = false;
    private static final long SEED = USE_RANDOM_SEED ? new Random().nextLong() : 123456789L;
    static {
        if (USE_RANDOM_SEED) {
            System.out.println("SEED = "+SEED);
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public GaussianTesterTest() {
    }

    /**
     * Tests our tests against Random.nextGaussian().
     */
    public void test_Random_nextGaussian() {
        if (DEBUG) {
            System.out.println("--- test_Random_nextGaussian() ---");
            System.out.println("SEED = "+SEED);
        }
        final GaussianTester tester = new GaussianTester();
        tester.test_XXX_nextGaussian(new Random(SEED));
    }
}
