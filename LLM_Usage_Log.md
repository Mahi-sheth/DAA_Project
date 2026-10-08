# LLM Usage Log

## Mahi

### Dataset Generation
Prompt:
"Generate a Java program to create DNA sequences containing
A, T, C and G for dataset sizes 100, 500, 1000, 2000,
5000 and 10000."

Current implementation: `src/RealDatasetProcessor.java` extracts DNA bases
from `ecoli.fasta` and writes paired datasets for those sizes. The prompt above
records the earlier synthetic-data request.

### Dataset Validation
Prompt:
"Generate Java code to validate DNA datasets by checking
sequence length and valid DNA characters."

Current status: alignment-score correctness is checked by
`src/CorrectnessTester.java`. There is no standalone `DatasetValidator.java`
in the current project.

### Documentation
Prompts used for preparing project documentation and README.

---

## Pranjali

Prompt record: Not yet provided for the sequential implementation in
`src/NeedlemanWunschSequential.java`.

---

## Fatima

### LLM Prompts & Interactions

#### 1. Algorithm Understanding
> **Prompt:** Explain the recurrence dependencies of the Needleman-Wunsch DP table $D[i][j] = \max(D[i-1][j-1] + s(a[i], b[j]), D[i-1][j] + \text{gap}, D[i][j-1] + \text{gap})$. Why does row-wise or column-wise parallelization fail, and how does anti-diagonal processing ($d = i + j$) ensure that all cells on the same anti-diagonal can be evaluated concurrently without data dependencies?

#### 2. Parallel Implementation
> **Prompt:** Write a parallel Needleman-Wunsch implementation in Java using the anti-diagonal wavefront approach. Instead of submitting individual tasks per cell, use a fixed `ExecutorService` thread pool with $T$ threads and synchronize diagonal steps using a `CyclicBarrier`. Each thread should handle a contiguous chunk of cells on diagonal $d$.

#### 3. Boundary Debugging & Memory Optimization
> **Prompt:** When implementing the anti-diagonal wavefront using three rotating 1D array buffers (`pp`, `p`, `c`) for $O(N)$ memory space, how should boundary cells ($i = 0$ or $j = 0$) and gap penalties be initialized on each diagonal $d$? Explain how to map matrix coordinates $(i, j)$ into buffer array indices safely without encountering array out-of-bounds errors on small sequence lengths (e.g., $N = 1, 2, 7$).

#### 4. Performance Optimization
> **Prompt:** My cell-level parallel wavefront code produces correct scores, but the speedup is less than 1 (slower than sequential) due to thread synchronization overhead across $\approx 2N$ barriers. Explain why fine-grained barrier synchronization limits performance, and show how to implement a tiled/blocked wavefront algorithm ($B \times B$ blocks) to reduce the number of barriers to $\approx 2N / B$.

---

### Critical Evaluation & Design Decisions

1. **Rejection of Micro-Tasking (Task-Per-Cell / `CompletableFuture`):**
   * *Initial Suggestion:* Submitting an asynchronous task for every cell on the diagonal.
   * *Reason for Rejection:* Creating $O(N \times M)$ task objects caused extreme allocation and scheduling overhead, leading to execution times $6\text{--}28\times$ slower than sequential execution.

2. **Rejection of Row-Wise / Column-Wise Parallelization:**
   * *Initial Consideration:* Evaluating matrix rows in parallel.
   * *Reason for Rejection:* Cell $D[i][j]$ directly depends on $D[i][j-1]$ (left neighbor). Processing cells within the same row concurrently violates data dependencies and leads to race conditions.

3. **Modification from Cell-Level to Tiled Wavefront ($B \times B$ Blocks):**
   * *Refinement:* Cell-level wavefront required $\approx N + M$ `CyclicBarrier` synchronizations. By grouping cells into $B \times B$ tiles, barrier synchronization was reduced by a factor of $B$ (to $\approx (N + M) / B$), significantly mitigating thread contention.
---

## Maitreyi

Prompt record: Not yet provided for performance analysis in
`src/PerformanceAnalysis.java` and graph generation in `performance.py`.

## Rejected Approach

Parallelizing every row independently was rejected.

Reason:

Cells in the Needleman-Wunsch DP matrix depend on neighboring
cells and cells from the previous row.

Therefore, independent row execution can violate the dependency
structure.

Wavefront/diagonal processing was selected instead.
