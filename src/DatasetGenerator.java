import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DatasetGenerator {

    static final char[] DNA = {'A', 'T', 'C', 'G'};
    static final Random random = new Random();

    public static String generateSequence(int length) {
        StringBuilder sequence = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sequence.append(DNA[random.nextInt(4)]);
        }

        return sequence.toString();
    }

    public static String mutateSequence(String sequence) {
        char[] result = sequence.toCharArray();

        int mutations = Math.max(1, sequence.length() / 10);

        for (int i = 0; i < mutations; i++) {
            int position = random.nextInt(result.length);

            char oldChar = result[position];
            char newChar;

            do {
                newChar = DNA[random.nextInt(4)];
            } while (newChar == oldChar);

            result[position] = newChar;
        }

        return new String(result);
    }

    public static void generateDataset(int size) {

        String sequence1 = generateSequence(size);
        String sequence2 = mutateSequence(sequence1);

        String fileName = "datasets/data_" + size + ".txt";

        try {
            File directory = new File("datasets");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            FileWriter writer = new FileWriter(fileName);

            writer.write(sequence1);
            writer.write("\n");
            writer.write(sequence2);
            writer.write("\n");

            writer.close();

            System.out.println("Generated: " + fileName);

        } catch (IOException e) {
            System.out.println("Error creating dataset: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        int[] sizes = {
            100,
            500,
            1000,
            2000,
            5000,
            10000
        };

        for (int size : sizes) {
            generateDataset(size);
        }

        System.out.println("All datasets generated successfully.");
    }
}