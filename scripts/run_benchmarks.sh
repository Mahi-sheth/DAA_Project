#!/bin/bash
# Benchmark Script for Linux / macOS (Fatima - Parallel Wavefront)

echo "Compiling Java source files..."
mkdir -p bin
javac -d bin src/*.java

mkdir -p results

echo "Running Tiled Parallel Benchmarks..."
echo "threads,dataset_size,time_ms,score" > results/fatima_parallel_tiled.csv
for t in 1 2 4 8; do
    for s in 100 200 500 1000 5000 10000; do
        java -classpath bin NeedlemanWunschParallel -Dmode=tiled -Dthreads=$t -Dsize=$s >> results/fatima_parallel_tiled.csv
    done
done

echo "Running Cell-Level Diagonal Benchmarks..."
echo "threads,dataset_size,time_ms,score" > results/fatima_parallel_diag.csv
for t in 1 2 4 8; do
    for s in 100 200 500 1000 5000 10000; do
        java -classpath bin NeedlemanWunschParallel -Dmode=diag -Dthreads=$t -Dsize=$s >> results/fatima_parallel_diag.csv
    done
done

echo "Benchmarking complete. Results saved in results/ directory."
