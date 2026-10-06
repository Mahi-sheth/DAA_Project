# Needleman-Wunsch Parallelization Project

## Project Overview

This project implements the Needleman-Wunsch global sequence
alignment algorithm using Java.

The project contains:

1. Sequential Needleman-Wunsch implementation
2. Parallel wavefront Needleman-Wunsch implementation
3. DNA dataset generation
4. Correctness testing
5. Performance analysis

## Problem Definition

Needleman-Wunsch uses dynamic programming for global sequence
alignment.

As the sequence size increases, the number of DP matrix cells
increases significantly.

Therefore, the project studies whether parallel wavefront
processing can improve execution performance.

## Objectives

- Implement Needleman-Wunsch sequentially.
- Implement parallel wavefront processing.
- Generate DNA datasets of different sizes.
- Validate sequential and parallel results.
- Measure execution time.
- Calculate speedup and efficiency.
- Analyze parallel performance.

## Dataset Sizes

- 100
- 500
- 1000
- 2000
- 5000
- 10000

## DNA Characters

A, T, C, G

## Requirements

- Java JDK
- VS Code
- Python 3 (if Python dataset generation is used)

## Compilation

javac src/NeedlemanWunschSequential.java

javac src/NeedlemanWunschParallel.java

javac src/DatasetGenerator.java

javac src/DatasetValidator.java

## Dataset Generation

java -cp src DatasetGenerator

## Dataset Validation

java -cp src DatasetValidator

## Execution

Run the sequential and parallel programs
using the same datasets.

The sequential and parallel scores should match.