################################################################################
Jafaran 1.1, 2015/12/13

Changes since version 1.0 :

- Using a more standard layout for files.

- Added SyncRNG, which adds synchronization on top of a RNG, and MTSyncRNG
  which makes use of it, and is apparently faster than MTConcRNG.
  "Sync" is now a new keyword, that identifies implementations that are
  thread-safe and blocking through synchronization.

- Optimized MTConcRNG.nextLong(), by removing the loop, and moving temperings
  after CASes (less time spent between CASes, so less chance of failed CAS).

################################################################################
Jafaran 1.0, 2014/04/27

- License: Apache License V2.0.

- Requires Java 1.5 or later.

- Jafaran (Java Fast Random) provides fast, and for some more random,
  implementations of java.util.Random, with additional nextXXX() methods, and
  methods to retrieve and restore state.
  
- The names of implementations contain "Conc" (for concurrent) if they are
  thread-safe and non-blocking, or "Seq" (for sequential) if they are not
  thread-safe.
  
- Also provides an implementation of Ziggurat algorithm (based on J. A. Doornik
  paper, 2005), used by nextGaussian() methods of the provided implementations.
  
- Principal classes:
  - Implementations using Mersenne-Twister algorithm (good pseudo-randomness):
    - MTConcRNG
    - MTSeqRNG
  - Implementations using Marsaglia Xor-Shift (fast):
    - MXSIntSeqRNG (32 bits)
    - MXSLongSeqRNG (64 bits) (nextLong() faster, larger period)
  - RandomConcRNG: Pseudo-RNG using the same algorithm than Random.
  - RandomRNGAdapter: Pseudo-RNG backed by a Random instance.
  - Ziggurat: Random-based implementation of Ziggurat algorithm.

################################################################################
