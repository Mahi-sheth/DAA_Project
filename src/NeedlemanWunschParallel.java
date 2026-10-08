import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Parallel Needleman-Wunsch (score only)
 * using wavefront / anti-diagonal processing.
 *
 * Dependency:
 * D[i][j] needs D[i-1][j-1], D[i-1][j], D[i][j-1].
 *
 * All three lie on diagonals d-2 and d-1
 * (d = i+j), so every cell on diagonal d
 * is independent of the others on diagonal d.
 *
 * A barrier between diagonals enforces the
 * dependency order.
 *
 * Memory:
 * only 3 diagonal buffers (O(n)) are kept.
 *
 * Usage:
 *
 * java -cp src NeedlemanWunschParallel
 * <size> [t1,t2,...]
 *
 * Example:
 * java -cp src NeedlemanWunschParallel
 * 5000 1,2,4,8
 *
 * OR
 *
 * java -cp src NeedlemanWunschParallel
 * -f seqA.txt seqB.txt [t1,t2,...]
 */
public class NeedlemanWunschParallel {

    // MUST be identical to
    // NeedlemanWunschSequential.java
    static final int MATCH = 1;
    static final int MISMATCH = -1;
    static final int GAP = -2;

    static final int MIN_PARALLEL_LEN = 512;

    static final int REPS = 5;

    // ---------- Reference sequential ----------
    // Row-by-row, full matrix-free
    static int sequentialScore(
            byte[] a,
            byte[] b) {

        int n = a.length;
        int m = b.length;

        int[] prev =
                new int[m + 1];

        int[] cur =
                new int[m + 1];

        for (int j = 0;
             j <= m;
             j++) {

            prev[j] =
                    j * GAP;
        }

        for (int i = 1;
             i <= n;
             i++) {

            cur[0] =
                    i * GAP;

            for (int j = 1;
                 j <= m;
                 j++) {

                int s =
                    prev[j - 1]
                    + (
                        a[i - 1] == b[j - 1]
                        ? MATCH
                        : MISMATCH
                    );

                cur[j] =
                    Math.max(
                        s,
                        Math.max(
                            prev[j] + GAP,
                            cur[j - 1] + GAP
                        )
                    );
            }

            int[] t = prev;
            prev = cur;
            cur = t;
        }

        return prev[m];
    }

    // ---------- Parallel wavefront ----------

    static int parallelScore(
            byte[] a,
            byte[] b,
            int threads)
            throws Exception {

        final int n = a.length;
        final int m = b.length;

        if (threads == 1) {
            return wavefrontSingle(a, b);
        }

        final int[][] bufs = {
            new int[n + 2],
            new int[n + 2],
            new int[n + 2]
        };

        // diagonal 0: D[0][0]
        bufs[0][0] = 0;

        final CyclicBarrier barrier =
                new CyclicBarrier(threads);

        final int lastD =
                n + m;

        ExecutorService pool =
                Executors.newFixedThreadPool(
                    threads
                );

        List<Future<?>> fs =
                new ArrayList<>();

        for (int t = 0;
             t < threads;
             t++) {

            final int tid = t;

            fs.add(
                pool.submit(() -> {

                    // Each thread rotates
                    // its own references identically
                    int[] pp = bufs[0];
                    int[] p = bufs[1];
                    int[] c = bufs[2];

                    try {

                        // Diagonal 1 boundaries
                        if (tid == 0) {

                            if (m >= 1) {
                                p[0] = GAP;
                            }

                            if (n >= 1) {
                                p[1] = GAP;
                            }
                        }

                        barrier.await();

                        for (int d = 2;
                             d <= lastD;
                             d++) {

                            int lo =
                                Math.max(
                                    1,
                                    d - m
                                );

                            int hi =
                                Math.min(
                                    n,
                                    d - 1
                                );

                            // Boundary cells
                            if (tid == 0) {

                                if (d <= m) {
                                    c[0] =
                                        d * GAP;
                                }

                                if (d <= n) {
                                    c[d] =
                                        d * GAP;
                                }
                            }

                            int len =
                                hi - lo + 1;

                            if (len > 0) {

                                int from;
                                int to;

                                if (
                                    len
                                    < MIN_PARALLEL_LEN
                                ) {

                                    if (tid == 0) {

                                        from = lo;
                                        to = hi;

                                    } else {

                                        from = 1;
                                        to = 0;
                                    }

                                } else {

                                    from =
                                        lo
                                        + (int)
                                        (
                                            (long)
                                            len
                                            * tid
                                            / threads
                                        );

                                    to =
                                        lo
                                        + (int)
                                        (
                                            (long)
                                            len
                                            * (tid + 1)
                                            / threads
                                        )
                                        - 1;
                                }

                                for (
                                    int i = from;
                                    i <= to;
                                    i++
                                ) {

                                    int j =
                                        d - i;

                                    int s =
                                        pp[i - 1]
                                        + (
                                            a[i - 1]
                                            == b[j - 1]
                                            ? MATCH
                                            : MISMATCH
                                        );

                                    c[i] =
                                        Math.max(
                                            s,
                                            Math.max(
                                                p[i - 1]
                                                    + GAP,
                                                p[i]
                                                    + GAP
                                            )
                                        );
                                }
                            }

                            // All cells of diagonal d
                            // must finish first
                            barrier.await();

                            int[] tmp = pp;
                            pp = p;
                            p = c;
                            c = tmp;
                        }

                    } catch (
                        InterruptedException
                        | BrokenBarrierException e
                    ) {

                        throw new RuntimeException(e);
                    }

                    return null;
                })
            );
        }

        for (Future<?> f : fs) {
            f.get();
        }

        pool.shutdown();

        // Diagonal d is stored in
        // bufs[d % 3]
        return bufs[lastD % 3][n];
    }

    // 1-thread version of the same
    // diagonal algorithm
    static int wavefrontSingle(
            byte[] a,
            byte[] b) {

        int n = a.length;
        int m = b.length;

        int[][] bufs = {
            new int[n + 2],
            new int[n + 2],
            new int[n + 2]
        };

        bufs[0][0] = 0;

        if (m >= 1) {
            bufs[1][0] = GAP;
        }

        if (n >= 1) {
            bufs[1][1] = GAP;
        }

        int[] pp = bufs[0];
        int[] p = bufs[1];
        int[] c = bufs[2];

        for (int d = 2;
             d <= n + m;
             d++) {

            if (d <= m) {
                c[0] = d * GAP;
            }

            if (d <= n) {
                c[d] = d * GAP;
            }

            int lo =
                Math.max(
                    1,
                    d - m
                );

            int hi =
                Math.min(
                    n,
                    d - 1
                );

            for (int i = lo;
                 i <= hi;
                 i++) {

                int j =
                    d - i;

                int s =
                    pp[i - 1]
                    + (
                        a[i - 1] == b[j - 1]
                        ? MATCH
                        : MISMATCH
                    );

                c[i] =
                    Math.max(
                        s,
                        Math.max(
                            p[i - 1] + GAP,
                            p[i] + GAP
                        )
                    );
            }

            int[] tmp = pp;
            pp = p;
            p = c;
            c = tmp;
        }

        return bufs[(n + m) % 3][n];
    }

    // ---------- Helpers ----------

    static byte[] randomDNA(
            int len,
            long seed) {

        Random r =
                new Random(seed);

        byte[] s =
                new byte[len];

        byte[] alpha =
                {'A', 'T', 'C', 'G'};

        for (int i = 0;
             i < len;
             i++) {

            s[i] =
                alpha[r.nextInt(4)];
        }

        return s;
    }

    static byte[] readSeq(
            String path)
            throws IOException {

        String s =
            new String(
                Files.readAllBytes(
                    Paths.get(path)
                )
            ).replaceAll(
                "\\s+",
                ""
            );

        if (s.startsWith(">")) {

            throw new IOException(
                "FASTA headers not supported: "
                + path
            );
        }

        return s
            .toUpperCase()
            .getBytes();
    }

    static double medianMs(
            long[] t) {

        Arrays.sort(t);

        return t[t.length / 2]
                / 1e6;
    }

    public static void main(
            String[] args)
            throws Exception {

        byte[] a;
        byte[] b;
        int idx;

        if (
            args.length >= 3
            && args[0].equals("-f")
        ) {

            a =
                readSeq(args[1]);

            b =
                readSeq(args[2]);

            idx = 3;

        } else {

            int size =
                args.length > 0
                ? Integer.parseInt(args[0])
                : 1000;

            a =
                randomDNA(
                    size,
                    42
                );

            b =
                randomDNA(
                    size,
                    4242
                );

            idx = 1;
        }

        int[] threadList =
                {1, 2, 4, 8};

        if (args.length > idx) {

            String[] p =
                args[idx].split(",");

            threadList =
                new int[p.length];

            for (int i = 0;
                 i < p.length;
                 i++) {

                threadList[i] =
                    Integer.parseInt(
                        p[i].trim()
                    );
            }
        }

        // Sequential baseline
        // Warm-up + timed

        int seqScore =
                sequentialScore(a, b);

        long[] ts =
                new long[REPS];

        for (int r = 0;
             r < REPS;
             r++) {

            long s =
                    System.nanoTime();

            sequentialScore(a, b);

            ts[r] =
                System.nanoTime() - s;
        }

        double seqMs =
                medianMs(ts);

        System.out.printf(
            "Sequence lengths: %d x %d, cores available: %d%n",
            a.length,
            b.length,
            Runtime.getRuntime()
                .availableProcessors()
        );

        System.out.printf(
            "Sequential score = %d, time = %.3f ms%n",
            seqScore,
            seqMs
        );

        System.out.println(
            "size,threads,seq_ms,par_ms,"
            + "speedup,efficiency,"
            + "seq_score,par_score,match"
        );

        for (int th : threadList) {

            // Warm-up
            int score =
                    parallelScore(
                        a,
                        b,
                        th
                    );

            long[] tp =
                    new long[REPS];

            for (int r = 0;
                 r < REPS;
                 r++) {

                long s =
                        System.nanoTime();

                score =
                    parallelScore(
                        a,
                        b,
                        th
                    );

                tp[r] =
                    System.nanoTime() - s;
            }

            double parMs =
                    medianMs(tp);

            double speedup =
                    seqMs / parMs;

            System.out.printf(
                "%d,%d,%.3f,%.3f,%.3f,%.3f,%d,%d,%s%n",
                a.length,
                th,
                seqMs,
                parMs,
                speedup,
                speedup / th,
                seqScore,
                score,
                seqScore == score
                    ? "PASS"
                    : "FAIL"
            );
        }
    }
}