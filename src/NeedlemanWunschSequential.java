import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NeedlemanWunschSequential {

    public static final int MATCH_SCORE = 2;
    public static final int MISMATCH_SCORE = -1;
    public static final int GAP_SCORE = -1;

    public static String[] readDataset(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<String> sequences = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    sequences.add(trimmed);
                }
            }

            if (sequences.size() < 2) {
                throw new IOException("Dataset file must contain at least two sequences: " + filePath);
            }

            return new String[] { sequences.get(0), sequences.get(1) };
        }
    }

    public static int align(String sequence1, String sequence2) {
        if (sequence1 == null || sequence2 == null) {
            throw new IllegalArgumentException("Sequence arguments cannot be null.");
        }

        int rows = sequence1.length() + 1;
        int cols = sequence2.length() + 1;
        int[][] dp = new int[rows][cols];

        for (int i = 1; i < rows; i++) {
            dp[i][0] = i * GAP_SCORE;
        }

        for (int j = 1; j < cols; j++) {
            dp[0][j] = j * GAP_SCORE;
        }

        for (int i = 1; i < rows; i++) {
            for (int j = 1; j < cols; j++) {
                int match = sequence1.charAt(i - 1) == sequence2.charAt(j - 1)
                        ? MATCH_SCORE
                        : MISMATCH_SCORE;

                int diagonal = dp[i - 1][j - 1] + match;
                int up = dp[i - 1][j] + GAP_SCORE;
                int left = dp[i][j - 1] + GAP_SCORE;

                dp[i][j] = Math.max(diagonal, Math.max(up, left));
            }
        }

        return dp[rows - 1][cols - 1];
    }
}
