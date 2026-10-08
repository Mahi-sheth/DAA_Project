import java.util.Random;

/**
 * Sequential Needleman-Wunsch global sequence alignment.
 *
 * Time : O(n * m)
 * Space : O(n * m) (full DP matrix kept for traceback)
 *
 * Usage:
 * java NeedlemanWunschSequential
 *     -> runs small DNA tests
 *
 * java NeedlemanWunschSequential <len1> <len2>
 *     -> random DNA benchmark (no alignment print)
 */
public class NeedlemanWunschSequential {

    // ---- Scoring scheme ----
    static final int MATCH = 1;
    static final int MISMATCH = -1;
    static final int GAP = -2;

    static class Result {
        int score;
        String aligned1;
        String aligned2;
    }

    static int score(char a, char b) {
        return (a == b) ? MATCH : MISMATCH;
    }

    /**
     * Fills the DP matrix.
     * D[i][j] = best score aligning s1[0..i) with s2[0..j).
     */
    static int[][] buildMatrix(String s1, String s2) {

        int n = s1.length();
        int m = s2.length();

        int[][] D = new int[n + 1][m + 1];

        // Initialization
        // Aligning a prefix against an empty string = all gaps
        for (int i = 0; i <= n; i++) {
            D[i][0] = i * GAP;
        }

        for (int j = 0; j <= m; j++) {
            D[0][j] = j * GAP;
        }

        // Recurrence
        for (int i = 1; i <= n; i++) {

            for (int j = 1; j <= m; j++) {

                int diag =
                        D[i - 1][j - 1]
                        + score(
                            s1.charAt(i - 1),
                            s2.charAt(j - 1)
                        );

                int up =
                        D[i - 1][j] + GAP;

                int left =
                        D[i][j - 1] + GAP;

                D[i][j] =
                        Math.max(
                            diag,
                            Math.max(up, left)
                        );
            }
        }

        return D;
    }

    /**
     * Traceback from D[n][m] to D[0][0].
     * Tie-break priority: diagonal, up, left.
     */
    static Result traceback(
            String s1,
            String s2,
            int[][] D) {

        StringBuilder a1 = new StringBuilder();
        StringBuilder a2 = new StringBuilder();

        int i = s1.length();
        int j = s2.length();

        while (i > 0 || j > 0) {

            if (i > 0
                    && j > 0
                    && D[i][j]
                    == D[i - 1][j - 1]
                    + score(
                        s1.charAt(i - 1),
                        s2.charAt(j - 1)
                    )) {

                a1.append(s1.charAt(i - 1));
                a2.append(s2.charAt(j - 1));

                i--;
                j--;

            } else if (
                    i > 0
                    && D[i][j]
                    == D[i - 1][j] + GAP) {

                a1.append(s1.charAt(i - 1));
                a2.append('-');

                i--;

            } else {

                a1.append('-');
                a2.append(s2.charAt(j - 1));

                j--;
            }
        }

        Result r = new Result();

        r.score =
                D[s1.length()][s2.length()];

        r.aligned1 =
                a1.reverse().toString();

        r.aligned2 =
                a2.reverse().toString();

        return r;
    }

    static Result align(
            String s1,
            String s2) {

        return traceback(
                s1,
                s2,
                buildMatrix(s1, s2)
        );
    }

    static void printMatrix(
            String s1,
            String s2,
            int[][] D) {

        System.out.print("          -");

        for (char c : s2.toCharArray()) {
            System.out.printf("%4c", c);
        }

        System.out.println();

        for (int i = 0;
             i <= s1.length();
             i++) {

            System.out.print(
                i == 0
                ? " -"
                : " " + s1.charAt(i - 1)
            );

            for (int j = 0;
                 j <= s2.length();
                 j++) {

                System.out.printf(
                    "%4d",
                    D[i][j]
                );
            }

            System.out.println();
        }
    }

    static String matchLine(
            String a,
            String b) {

        StringBuilder sb =
                new StringBuilder();

        for (int k = 0;
             k < a.length();
             k++) {

            char x = a.charAt(k);
            char y = b.charAt(k);

            sb.append(
                x == '-' || y == '-'
                ? ' '
                : (x == y ? '|' : '.')
            );
        }

        return sb.toString();
    }

    static void runTest(
            String name,
            String s1,
            String s2,
            boolean showMatrix) {

        System.out.println(
            "=== " + name + " ==="
        );

        System.out.println(
            "Seq1: " + s1
        );

        System.out.println(
            "Seq2: " + s2
        );

        int[][] D =
                buildMatrix(s1, s2);

        if (showMatrix) {
            printMatrix(s1, s2, D);
        }

        Result r =
                traceback(s1, s2, D);

        System.out.println(
            "Alignment score: " + r.score
        );

        System.out.println(
            r.aligned1
        );

        System.out.println(
            matchLine(
                r.aligned1,
                r.aligned2
            )
        );

        System.out.println(
            r.aligned2
        );

        System.out.println();
    }

    static String randomDNA(
            int len,
            Random rnd) {

        char[] bases =
                {'A', 'C', 'G', 'T'};

        StringBuilder sb =
                new StringBuilder(len);

        for (int i = 0;
             i < len;
             i++) {

            sb.append(
                bases[rnd.nextInt(4)]
            );
        }

        return sb.toString();
    }

    public static void main(
            String[] args) {

        System.out.println(
            "Scoring: match="
            + MATCH
            + ", mismatch="
            + MISMATCH
            + ", gap="
            + GAP
            + "\n"
        );

        if (args.length == 2) {

            int n =
                    Integer.parseInt(args[0]);

            int m =
                    Integer.parseInt(args[1]);

            Random rnd =
                    new Random(42);

            String s1 =
                    randomDNA(n, rnd);

            String s2 =
                    randomDNA(m, rnd);

            long start =
                    System.nanoTime();

            int[][] D =
                    buildMatrix(s1, s2);

            long fillTime =
                    System.nanoTime() - start;

            Result r =
                    traceback(s1, s2, D);

            long total =
                    System.nanoTime() - start;

            System.out.println(
                "Lengths: "
                + n
                + " x "
                + m
            );

            System.out.println(
                "Alignment score: "
                + r.score
            );

            System.out.printf(
                "Matrix fill time: %.3f ms%n",
                fillTime / 1e6
            );

            System.out.printf(
                "Total time (fill + traceback): %.3f ms%n",
                total / 1e6
            );

            return;
        }

        // Small DNA tests

        runTest(
            "Test 1: textbook example",
            "GATTACA",
            "GCATGCU".replace('U', 'T'),
            true
        );

        runTest(
            "Test 2: identical",
            "ACGTACGT",
            "ACGTACGT",
            false
        );

        runTest(
            "Test 3: one mismatch",
            "ACGTACGT",
            "ACGAACGT",
            false
        );

        runTest(
            "Test 4: insertion/deletion",
            "AGCTAGCT",
            "AGCTTAGCT",
            false
        );

        runTest(
            "Test 5: completely different",
            "AAAA",
            "TTTT",
            false
        );

        runTest(
            "Test 6: empty vs non-empty",
            "",
            "ACGT",
            false
        );
    }
}