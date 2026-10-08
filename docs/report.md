# Needleman-Wunsch DNA Sequence Alignment

## Overview

This project implements global pairwise DNA sequence alignment with the
Needleman-Wunsch dynamic-programming algorithm. The sequential Java
implementation builds the full score matrix and reconstructs an alignment.
The parallel Java implementation calculates the alignment score using
wavefront (anti-diagonal) processing; it does not reconstruct aligned strings.

Both implementations use the same scoring scheme:

- Match: +1
- Mismatch: -1
- Gap: -2

## Algorithm and Implementation

For sequences `a` and `b`, each matrix cell takes the best of a match or
mismatch, a gap in either sequence, and the previous prefix scores:

$$
F_{i,j}=\max\left(F_{i-1,j-1}+s(a_i,b_j),\;F_{i-1,j}-2,\;F_{i,j-1}-2\right)
$$

The boundary conditions are `F[i,0] = -2i` and `F[0,j] = -2j`. The
sequential implementation takes $O(nm)$ time and stores an $O(nm)$ matrix.
The parallel implementation computes independent cells on each anti-diagonal,
using barriers between diagonals and three diagonal buffers. Its total work is
$O(nm)$ and its score-buffer storage is $O(n)$.

## Input Data and Correctness

`RealDatasetProcessor.java` reads `ecoli.fasta`, skips FASTA header lines, and
retains only A, T, C, and G. For each of the lengths 100, 500, 1000, 2000,
5000, and 10000, it writes two files under `datasets/`: sequence A begins at
base 0, and sequence B begins at base 500.

`CorrectnessTester.java` compares the sequential score with the parallel score
for each dataset pair. Its current source configures thread counts 1, 2, 4, and
8 and writes `results/correctness.csv`. The stored correctness CSV has one row
per size, records `true` for all six comparisons, and does not include a thread
count column; it therefore does not identify which thread counts produced
those stored rows.

## Recorded Performance

The current `results/performance.csv` contains one row per size, all with four
threads. Each row reports a parallel time greater than its sequential time:

- Size 100: 6.334 ms sequential, 57.480 ms parallel; speedup 0.110,
  efficiency 0.028.
- Size 500: 14.543 ms sequential, 105.681 ms parallel; speedup 0.138,
  efficiency 0.034.
- Size 1000: 7.985 ms sequential, 160.927 ms parallel; speedup 0.050,
  efficiency 0.012.
- Size 2000: 13.419 ms sequential, 376.416 ms parallel; speedup 0.036,
  efficiency 0.009.
- Size 5000: 102.732 ms sequential, 1091.349 ms parallel; speedup 0.094,
  efficiency 0.024.
- Size 10000: 403.991 ms sequential, 2623.884 ms parallel; speedup 0.154,
  efficiency 0.038.

Speedup is sequential time divided by parallel time; efficiency is speedup
divided by thread count. A speedup below 1 means the parallel run was slower.
The highest recorded speedup is 0.154 at size 10000, so these measurements do
not show a performance improvement from using four threads.

Both stored CSV files report equal sequential and parallel scores within each
row. However, their absolute scores differ at every matching size. For
example, size 100 has score 82 in `performance.csv` and 170 in
`correctness.csv`. The files do not identify their input sequences or run
provenance, so they cannot be treated as results from the same inputs.

## Limitations

The performance CSV does not record the machine, input identifiers, or timing
capture details. The current benchmark source uses the median of five timed
runs, but the CSV does not establish that its values were produced by that
source version. These missing details and the score differences between the
two result files limit reproducibility and direct comparison. The measured
slowdown is clear in the stored four-thread data; its cause is not established
by these results alone.

`PerformanceAnalysis.java` reads `results/performance.csv` and writes
`results/performance_summary.csv`. `performance.py` reads the same performance
CSV and generates execution-time, speedup, and efficiency plots under
`results/graphs/`.

## Conclusion

The implementation provides sequential and wavefront score computation, a
dataset-processing path, score comparisons, and performance-analysis tools.
The available correctness rows show matching scores within each recorded
comparison. The available four-thread timing rows show the parallel version
slower for every listed size. Because the correctness and performance files
contain different scores for the same nominal sizes and lack input provenance,
new measurements should be captured from the same dataset pairs before making
a stronger correctness-to-performance comparison.
