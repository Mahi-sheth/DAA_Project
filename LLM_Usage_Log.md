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

Prompt record: Not yet provided for the parallel wavefront implementation in
`src/NeedlemanWunschParallel.java`.

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