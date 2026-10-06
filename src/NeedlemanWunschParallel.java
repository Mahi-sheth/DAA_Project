import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NeedlemanWunschParallel {

    public static final int MATCH_SCORE = 2;
    public static final int MISMATCH_SCORE = -1;
    public static final int GAP_SCORE = -1;

    public static int align(String sequence1, String sequence2) {
        return align(sequence1, sequence2, Runtime.getRuntime().availableProcessors());
    }

    public static int align(String sequence1, String sequence2, int threads) {
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

        int totalDiagonals = rows + cols - 2;
        int actualThreads = Math.max(1, Math.min(threads, Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(actualThreads);

        try {
            for (int diagonal = 1; diagonal <= totalDiagonals; diagonal++) {
                int startRow = Math.max(1, diagonal - (cols - 1));
                int endRow = Math.min(rows - 1, diagonal);

                List<Callable<Void>> tasks = new ArrayList<>();

                for (int i = startRow; i <= endRow; i++) {
                    final int row = i;
                    final int col = diagonal - row;

                    if (col < 1 || row < 1) {
                        continue;
                    }

                    tasks.add(() -> {
                        int match = sequence1.charAt(row - 1) == sequence2.charAt(col - 1)
                                ? MATCH_SCORE
                                : MISMATCH_SCORE;

                        int diagonalScore = dp[row - 1][col - 1] + match;
                        int upScore = dp[row - 1][col] + GAP_SCORE;
                        int leftScore = dp[row][col - 1] + GAP_SCORE;

                        dp[row][col] = Math.max(diagonalScore, Math.max(upScore, leftScore));
                        return null;
                    });
                }

                if (!tasks.isEmpty()) {
                    List<Future<Void>> futures = executor.invokeAll(tasks);
                    for (Future<Void> future : futures) {
                        future.get();
                    }
                }
            }

            return dp[rows - 1][cols - 1];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel alignment was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Parallel alignment failed.", e.getCause());
        } finally {
            executor.shutdown();
        }
    }

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
}
