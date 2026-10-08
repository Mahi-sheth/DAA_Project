import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RealDatasetProcessor {

    static final int[] SIZES = {100, 500, 1000, 2000, 5000, 10000};

    static String readFASTA(String fileName) throws IOException {

        StringBuilder sequence = new StringBuilder();

        List<String> lines = Files.readAllLines(Paths.get(fileName));

        for (String line : lines) {

            line = line.trim();

            // Ignore FASTA header
            if (line.startsWith(">")) {
                continue;
            }

            // Keep only DNA bases
            for (int i = 0; i < line.length(); i++) {

                char c = Character.toUpperCase(line.charAt(i));

                if (c == 'A' || c == 'T' || c == 'C' || c == 'G') {
                    sequence.append(c);
                }
            }
        }

        return sequence.toString();
    }

    static void saveSequence(String fileName, String sequence)
            throws IOException {

        Files.write(
            Paths.get(fileName),
            sequence.getBytes()
        );
    }

    static void createDataset(
            String sequence,
            int size,
            String outputFolder) throws IOException {

        if (sequence.length() < size + 500) {
            throw new IOException(
                "Not enough DNA sequence for size " + size
            );
        }

        /*
         * Sequence A:
         * starts from position 0
         *
         * Sequence B:
         * starts from position 500
         */
        String sequenceA =
                sequence.substring(0, size);

        String sequenceB =
                sequence.substring(500, 500 + size);

        String fileA =
                outputFolder + "/data_" + size + "_A.txt";

        String fileB =
                outputFolder + "/data_" + size + "_B.txt";

        saveSequence(fileA, sequenceA);
        saveSequence(fileB, sequenceB);

        System.out.println(
            "Created size " + size +
            " -> A: " + fileA +
            ", B: " + fileB
        );
    }

    public static void main(String[] args) {

        String fastaFile = "ecoli.fasta";
        String outputFolder = "datasets";

        try {

            Files.createDirectories(
                Paths.get(outputFolder)
            );

            System.out.println(
                "Reading real biological DNA dataset..."
            );

            String sequence = readFASTA(fastaFile);

            System.out.println(
                "DNA bases extracted: " + sequence.length()
            );

            if (sequence.length() < 10500) {
                System.out.println(
                    "Error: FASTA sequence is too short."
                );
                return;
            }

            for (int size : SIZES) {

                createDataset(
                    sequence,
                    size,
                    outputFolder
                );
            }

            System.out.println();
            System.out.println(
                "All datasets generated successfully."
            );

        } catch (IOException e) {

            System.out.println(
                "Error: " + e.getMessage()
            );
        }
    }
}