import java.util.Arrays;

public class PerformanceBenchmark {

    public static void main(String[] args) {
        int[] sizes = {100, 500, 1000, 2000, 5000, 10000};

        System.out.println("Size,Sequential Time ms,Parallel Time ms");

        for (int size : sizes) {
            String file = "datasets/data_" + size + ".txt";
            try {
                String[] sequences = NeedlemanWunschSequential.readDataset(file);

                long start = System.nanoTime();
                int sequentialScore = NeedlemanWunschSequential.align(sequences[0], sequences[1]);
                long sequentialTime = (System.nanoTime() - start) / 1_000_000;

                start = System.nanoTime();
                int parallelScore = NeedlemanWunschParallel.align(sequences[0], sequences[1], 4);
                long parallelTime = (System.nanoTime() - start) / 1_000_000;

                System.out.println(size + "," + sequentialTime + "," + parallelTime + "," + sequentialScore + "," + parallelScore);
            } catch (Exception e) {
                System.err.println("Failed for size " + size + ": " + e.getMessage());
            }
        }
    }
}
