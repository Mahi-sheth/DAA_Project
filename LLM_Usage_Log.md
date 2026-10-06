# LLM Usage Log

## Mahi

### Dataset Generation
Prompt:
"Generate a Java program to create DNA sequences containing
A, T, C and G for dataset sizes 100, 500, 1000, 2000,
5000 and 10000."

### Dataset Validation
Prompt:
"Generate Java code to validate DNA datasets by checking
sequence length and valid DNA characters."

### Documentation
Prompts used for preparing project documentation and README.

---

## Pranjali

Add the actual prompts used for the sequential implementation.

---

## Fatima

Add the actual prompts used for the parallel wavefront implementation.

---

## Maitreyi

Add the actual prompts used for performance analysis.

## Rejected Approach

Parallelizing every row independently was rejected.

Reason:

Cells in the Needleman-Wunsch DP matrix depend on neighboring
cells and cells from the previous row.

Therefore, independent row execution can violate the dependency
structure.

Wavefront/diagonal processing was selected instead.