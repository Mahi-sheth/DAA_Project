import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CorrectnessTester {

    static final int[] SIZES = {
        100,
        500,
        1000,
        2000,
        5000,
        10000
    };

    static String readSequence(String fileName)
            throws IOException {

        return new String(
            Files.readAllBytes(Paths.get(fileName))
        ).replaceAll("\\s+", "")
         .toUpperCase();
    }

    static int getSequentialScore(
            String sequenceA,
            String sequenceB) {

        int[][] matrix =
            NeedlemanWunschSequential
            .buildMatrix(sequenceA, sequenceB);

        return matrix[
            sequenceA.length()
        ][
            sequenceB.length()
        ];
    }

    static int getParallelScore(
            String sequenceA,
            String sequenceB,
            int threads)
            throws Exception {

        byte[] a =
            sequenceA.getBytes();

        byte[] b =
            sequenceB.getBytes();

        return NeedlemanWunschParallel
                .parallelScore(
                    a,
                    b,
                    threads
                );
    }

    public static void main(String[] args) {

        try {

            FileWriter writer =
                new FileWriter(
                    "results/correctness.csv"
                );

            writer.write(
                "Size,Sequential Score," +
                "Parallel Score,Threads,Match\n"
            );

            System.out.println(
                "=========================================="
            );

            System.out.println(
                "      NEEDLEMAN-WUNSCH CORRECTNESS"
            );

            System.out.println(
                "=========================================="
            );

            for (int size : SIZES) {

                String fileA =
                    "datasets/data_" +
                    size + "_A.txt";

                String fileB =
                    "datasets/data_" +
                    size + "_B.txt";

                String sequenceA =
                    readSequence(fileA);

                String sequenceB =
                    readSequence(fileB);

                System.out.println();
                System.out.println(
                    "Dataset size: " + size
                );

                // Sequential score
                int sequentialScore =
                    getSequentialScore(
                        sequenceA,
                        sequenceB
                    );

                // Test with 1, 2, 4 and 8 threads
                int[] threadsList = {
                    1, 2, 4, 8
                };

                for (int threads : threadsList) {

                    int parallelScore =
                        getParallelScore(
                            sequenceA,
                            sequenceB,
                            threads
                        );

                    boolean match =
                        sequentialScore
                        == parallelScore;

                    System.out.println(
                        "Threads: " +
                        threads
                    );

                    System.out.println(
                        "Sequential Score: " +
                        sequentialScore
                    );

                    System.out.println(
                        "Parallel Score:   " +
                        parallelScore
                    );

                    System.out.println(
                        "Result: " +
                        (match ? "PASS" : "FAIL")
                    );

                    writer.write(
                        size + "," +
                        sequentialScore + "," +
                        parallelScore + "," +
                        threads + "," +
                        match + "\n"
                    );
                }
            }

            writer.close();

            System.out.println();
            System.out.println(
                "=========================================="
            );

            System.out.println(
                "Correctness testing completed."
            );

            System.out.println(
                "Results saved to results/correctness.csv"
            );

            System.out.println(
                "=========================================="
            );

        } catch (Exception e) {

            System.out.println(
                "Error: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}