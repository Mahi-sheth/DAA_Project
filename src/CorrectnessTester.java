import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CorrectnessTester {

    static int[] DATASET_SIZES = {
        100,
        500,
        1000,
        2000,
        5000,
        10000
    };

    public static void main(String[] args) {

        try {

            File resultsDirectory = new File("results");

            if (!resultsDirectory.exists()) {
                resultsDirectory.mkdirs();
            }

            FileWriter writer =
                    new FileWriter("results/correctness.csv");

            writer.write(
                "Dataset Size,Sequential Score,Parallel Score,Match\n"
            );

            for (int size : DATASET_SIZES) {

                String file =
                        "datasets/data_" + size + ".txt";

                System.out.println(
                    "\nTesting dataset: " + size
                );

                String[] sequences =
                        NeedlemanWunschSequential
                        .readDataset(file);

                // Sequential
                int sequentialScore =
                        NeedlemanWunschSequential
                        .align(
                            sequences[0],
                            sequences[1]
                        );

                // Parallel using 4 threads
                int parallelScore =
                        NeedlemanWunschParallel
                        .align(
                            sequences[0],
                            sequences[1],
                            4
                        );

                boolean match =
                        sequentialScore == parallelScore;

                System.out.println(
                    "Sequential Score: " +
                    sequentialScore
                );

                System.out.println(
                    "Parallel Score: " +
                    parallelScore
                );

                System.out.println(
                    "Match: " + match
                );

                writer.write(
                    size + "," +
                    sequentialScore + "," +
                    parallelScore + "," +
                    match + "\n"
                );
            }

            writer.close();

            System.out.println(
                "\nCorrectness testing completed."
            );

        } catch (Exception e) {

            System.out.println(
                "Error: " + e.getMessage()
            );
        }
    }
}