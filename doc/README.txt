################################################################################
Jafaran 1.0, 2014/04/27


- License: Apache License V2.0.


- Requires Java 1.5 or later.


- Provides fast, and for some more random, implementations of java.util.Random,
  with additional nextXXX() methods, and methods to retrieve and restore state.
  
  The names of implementations contain "Conc" (for concurrent) if they are
  thread-safe, or "Seq" (for sequential) if they are not.
  
  Also provides a Random-based implementation of Ziggurat algorithm, used by
  nextGaussian() methods of the provided implementations.
  
  Principal classes:
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
