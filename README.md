# Needleman-Wunsch Sequence Alignment

This project contains sequential and parallel Java implementations of
global DNA sequence alignment. Both use match `+1`, mismatch `-1`, and gap
`-2` scores. The sequential implementation builds the full dynamic-programming
matrix and reconstructs an alignment. The parallel implementation computes
the alignment score using wavefront (anti-diagonal) processing.

## Data and Tests

`src/RealDatasetProcessor.java` reads `ecoli.fasta`, ignores FASTA headers,
keeps only A, T, C, and G, and writes paired files in `datasets/` for lengths
100, 500, 1000, 2000, 5000, and 10000. For each length, sequence A starts at
base 0 and sequence B starts at base 500.

`src/CorrectnessTester.java` reads those pairs, compares sequential and
parallel scores at thread counts 1, 2, 4, and 8, and overwrites
`results/correctness.csv`. It compares scores; it does not validate dataset
characters or lengths.

## Requirements

- A Java JDK
- Python 3, pandas, and matplotlib to generate graphs

Run commands from the project root.

## Compile

```sh
javac -d bin src/NeedlemanWunschSequential.java src/NeedlemanWunschParallel.java src/CorrectnessTester.java src/PerformanceAnalysis.java src/RealDatasetProcessor.java
```

## Run

Generate the dataset files from `ecoli.fasta`:

```sh
java -cp bin RealDatasetProcessor
```

Run the score comparisons on those files:

```sh
java -cp bin CorrectnessTester
```

Run the sequential implementation's built-in examples, or benchmark random
sequences with the two lengths supplied as arguments:

```sh
java -cp bin NeedlemanWunschSequential
java -cp bin NeedlemanWunschSequential 1000 1000
```

Run the parallel benchmark on random sequences. The optional second argument
is a comma-separated thread list; if omitted, it uses 1, 2, 4, and 8 threads.
The `-f` form reads two plain sequence files (not FASTA files):

```sh
java -cp bin NeedlemanWunschParallel 1000 1,2,4,8
java -cp bin NeedlemanWunschParallel -f datasets/data_100_A.txt datasets/data_100_B.txt 1,2,4,8
```

## Performance Results

`NeedlemanWunschParallel` prints CSV-formatted measurements to standard output;
it does not write `results/performance.csv`. `PerformanceAnalysis` reads that
file, prints its rows and best speedup, and overwrites
`results/performance_summary.csv`:

```sh
java -cp bin PerformanceAnalysis
```

`performance.py` also reads `results/performance.csv` and writes
`execution_time.png`, `speedup.png`, and `efficiency.png` to
`results/graphs/`:

```sh
python performance.py
```