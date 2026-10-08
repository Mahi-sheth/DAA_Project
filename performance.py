import pandas as pd
import matplotlib.pyplot as plt
import os

# Read performance results
file = "results/performance.csv"
data = pd.read_csv(file)
data.columns = (
    data.columns
    .str.strip()
    .str.lower()
    .str.replace(r"[^a-z0-9]+", "_", regex=True)
    .str.strip("_")
)
data = data.rename(
    columns={
        "sequential_time_ms": "seq_ms",
        "parallel_time_ms": "par_ms",
    }
)

required_columns = {"size", "threads", "seq_ms", "par_ms", "speedup", "efficiency"}
missing_columns = required_columns.difference(data.columns)
if missing_columns:
    raise ValueError(
        "Performance CSV is missing required columns: "
        + ", ".join(sorted(missing_columns))
    )

# Create graph folder
os.makedirs("results/graphs", exist_ok=True)


# -------------------------------------------------
# 1. Sequence Size vs Execution Time
# -------------------------------------------------

sequential = data.groupby("size", as_index=False)["seq_ms"].mean()

plt.figure()

plt.plot(
    sequential["size"],
    sequential["seq_ms"],
    marker="o",
    label="Sequential"
)

for threads in sorted(data["threads"].unique()):
    parallel = (
        data[data["threads"] == threads]
        .groupby("size", as_index=False)["par_ms"]
        .mean()
    )
    thread_label = "thread" if threads == 1 else "threads"
    plt.plot(
        parallel["size"],
        parallel["par_ms"],
        marker="o",
        label=f"Parallel ({threads} {thread_label})"
    )

plt.xlabel("Sequence Size")
plt.ylabel("Execution Time (ms)")
plt.title("Sequence Size vs Execution Time")
plt.legend()
plt.grid(True)

plt.savefig(
    "results/graphs/execution_time.png",
    dpi=300,
    bbox_inches="tight"
)

plt.close()


# -------------------------------------------------
# 2. Threads vs Speedup
# -------------------------------------------------

plt.figure()

for size in sorted(data["size"].unique()):

    subset = data[data["size"] == size]

    plt.plot(
        subset["threads"],
        subset["speedup"],
        marker="o",
        label=str(size)
    )

plt.xlabel("Number of Threads")
plt.ylabel("Speedup")
plt.title("Threads vs Speedup")
plt.legend(title="Sequence Size")
plt.grid(True)

plt.savefig(
    "results/graphs/speedup.png",
    dpi=300,
    bbox_inches="tight"
)

plt.close()


# -------------------------------------------------
# 3. Threads vs Efficiency
# -------------------------------------------------

plt.figure()

for size in sorted(data["size"].unique()):

    subset = data[data["size"] == size]

    plt.plot(
        subset["threads"],
        subset["efficiency"],
        marker="o",
        label=str(size)
    )

plt.xlabel("Number of Threads")
plt.ylabel("Efficiency")
plt.title("Threads vs Efficiency")
plt.legend(title="Sequence Size")
plt.grid(True)

plt.savefig(
    "results/graphs/efficiency.png",
    dpi=300,
    bbox_inches="tight"
)

plt.close()


print("All graphs generated successfully!")

print("\nGenerated files:")
print("results/graphs/execution_time.png")
print("results/graphs/speedup.png")
print("results/graphs/efficiency.png")