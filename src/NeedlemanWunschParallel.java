import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Parallel Needleman-Wunsch (score only), wavefront processing in Java.
 *
 * Dependency: D[i][j] needs D[i-1][j-1], D[i-1][j], D[i][j-1].
 * Cells on one anti-diagonal (i+j = const) never depend on each other, so they can run together.
 *
 * Two parallel versions are provided:
 *   TILED (default)  - the matrix is cut into BxB blocks; the wavefront runs over BLOCK diagonals.
 *                      One barrier per block-diagonal (~2n/B barriers) instead of one per cell-diagonal (~2n).
 *   DIAG (-Dmode=diag) - cell-level wavefront, one barrier per cell-diagonal (kept for comparison).
 * Threads are created once (ExecutorService); a CyclicBarrier separates diagonals. No locks, no atomics:
 * threads write disjoint memory, and the barrier guarantees earlier diagonals are complete.
 *
 * Usage:
 *   java -cp src NeedlemanWunschParallel -d datasets/data_1000.txt [1,2,4,8]   (two sequences, one per line)
 *   java -cp src NeedlemanWunschParallel -f seqA.txt seqB.txt [1,2,4,8]
 *   java -cp src NeedlemanWunschParallel 5000 [1,2,4,8]                        (random DNA of that length)
 * Options: -Dmode=diag   use the cell-level wavefront
 *          -Dcsv=true    print only CSV rows (used by the benchmark scripts)
 */
public class NeedlemanWunschParallel {

    // MUST be identical to the scoring used in NeedlemanWunschSequential.java
    static final int MATCH = 1, MISMATCH = -1, GAP = -2;

    static final int MIN_PARALLEL_LEN = 512;   // DIAG mode: diagonals shorter than this run on thread 0 only
    static final int REPS = 5;

    // ---------- Reference sequential (row by row, score only) ----------
    static int sequentialScore(byte[] a, byte[] b) {
        int n = a.length, m = b.length;
        int[] prev = new int[m + 1], cur = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j * GAP;
        for (int i = 1; i <= n; i++) {
            cur[0] = i * GAP;
            for (int j = 1; j <= m; j++) {
                int s = prev[j - 1] + (a[i - 1] == b[j - 1] ? MATCH : MISMATCH);
                cur[j] = Math.max(s, Math.max(prev[j] + GAP, cur[j - 1] + GAP));
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[m];
    }

    // ---------- TILED wavefront (default) ----------
    // hRow[bi] = row D[bi*B][0..m]  (top boundary of block-row bi; hRow[bi+1] is its bottom boundary)
    // vCol[bj] = column D[0..n][bj*B] (left boundary of block-column bj; vCol[bj+1] is its right boundary)
    static int tiledScore(byte[] a, byte[] b, int threads) throws Exception {
        final int n = a.length, m = b.length;
        final int B = Math.max(16, Math.min(256, (Math.max(n, m) + 2 * threads - 1) / (2 * threads)));
        final int nb = (n + B - 1) / B, mb = (m + B - 1) / B;
        final int[][] hRow = new int[nb + 1][m + 1];
        final int[][] vCol = new int[mb + 1][n + 1];
        for (int j = 0; j <= m; j++) hRow[0][j] = j * GAP;
        for (int bi = 0; bi <= nb; bi++) hRow[bi][0] = Math.min(bi * B, n) * GAP;
        for (int i = 0; i <= n; i++) vCol[0][i] = i * GAP;

        final int lastE = nb + mb - 2;                      // last block-diagonal
        final CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Object>> fs = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            fs.add(pool.submit((Callable<Object>) () -> {
                int[] prev = new int[B + 1], cur = new int[B + 1];
                for (int e = 0; e <= lastE; e++) {
                    int biLo = Math.max(0, e - (mb - 1)), biHi = Math.min(nb - 1, e);
                    for (int bi = biLo + tid; bi <= biHi; bi += threads) {   // blocks of this diagonal, round-robin
                        int bj = e - bi;
                        int i0 = bi * B + 1, i1 = Math.min((bi + 1) * B, n);
                        int j0 = bj * B + 1, j1 = Math.min((bj + 1) * B, m);
                        int w = j1 - j0 + 1;
                        System.arraycopy(hRow[bi], j0 - 1, prev, 0, w + 1);  // D[i0-1][j0-1 .. j1]
                        int[] left = vCol[bj], right = vCol[bj + 1];
                        for (int i = i0; i <= i1; i++) {
                            cur[0] = left[i];                               // D[i][j0-1]
                            byte ai = a[i - 1];
                            for (int k = 1; k <= w; k++) {
                                int s = prev[k - 1] + (ai == b[j0 + k - 2] ? MATCH : MISMATCH);
                                cur[k] = Math.max(s, Math.max(prev[k] + GAP, cur[k - 1] + GAP));
                            }
                            right[i] = cur[w];                              // D[i][j1]
                            int[] tmp = prev; prev = cur; cur = tmp;
                        }
                        System.arraycopy(prev, 1, hRow[bi + 1], j0, w);     // D[i1][j0 .. j1]
                    }
                    barrier.await();                                        // block-diagonal e finished
                }
                return null;
            }));
        }
        for (Future<Object> f : fs) f.get();
        pool.shutdown();
        return hRow[nb][m];
    }

    // ---------- DIAG wavefront (cell-level, -Dmode=diag) ----------
    static int diagScore(byte[] a, byte[] b, int threads) throws Exception {
        final int n = a.length, m = b.length;
        if (threads == 1) return diagSingle(a, b);
        final int[][] bufs = { new int[n + 2], new int[n + 2], new int[n + 2] };
        final CyclicBarrier barrier = new CyclicBarrier(threads);
        final int lastD = n + m;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Object>> fs = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            fs.add(pool.submit((Callable<Object>) () -> {
                int[] pp = bufs[0], p = bufs[1], c = bufs[2];                // diagonals d-2, d-1, d
                if (tid == 0) {                                              // diagonal 1 boundaries
                    if (m >= 1) p[0] = GAP;
                    if (n >= 1) p[1] = GAP;
                }
                barrier.await();
                for (int d = 2; d <= lastD; d++) {
                    int lo = Math.max(1, d - m), hi = Math.min(n, d - 1);
                    if (tid == 0) {
                        if (d <= m) c[0] = d * GAP;
                        if (d <= n) c[d] = d * GAP;
                    }
                    int len = hi - lo + 1;
                    if (len > 0) {
                        int from, to;
                        if (len < MIN_PARALLEL_LEN) {
                            if (tid == 0) { from = lo; to = hi; } else { from = 1; to = 0; }
                        } else {
                            from = lo + (int) ((long) len * tid / threads);
                            to   = lo + (int) ((long) len * (tid + 1) / threads) - 1;
                        }
                        for (int i = from; i <= to; i++) {
                            int j = d - i;
                            int s = pp[i - 1] + (a[i - 1] == b[j - 1] ? MATCH : MISMATCH);
                            c[i] = Math.max(s, Math.max(p[i - 1] + GAP, p[i] + GAP));
                        }
                    }
                    barrier.await();
                    int[] tmp = pp; pp = p; p = c; c = tmp;
                }
                return null;
            }));
        }
        for (Future<Object> f : fs) f.get();
        pool.shutdown();
        return bufs[lastD % 3][n];                                           // diagonal d is in bufs[d % 3]
    }

    static int diagSingle(byte[] a, byte[] b) {
        int n = a.length, m = b.length;
        int[][] bufs = { new int[n + 2], new int[n + 2], new int[n + 2] };
        if (m >= 1) bufs[1][0] = GAP;
        if (n >= 1) bufs[1][1] = GAP;
        int[] pp = bufs[0], p = bufs[1], c = bufs[2];
        for (int d = 2; d <= n + m; d++) {
            if (d <= m) c[0] = d * GAP;
            if (d <= n) c[d] = d * GAP;
            int lo = Math.max(1, d - m), hi = Math.min(n, d - 1);
            for (int i = lo; i <= hi; i++) {
                int j = d - i;
                int s = pp[i - 1] + (a[i - 1] == b[j - 1] ? MATCH : MISMATCH);
                c[i] = Math.max(s, Math.max(p[i - 1] + GAP, p[i] + GAP));
            }
            int[] tmp = pp; pp = p; p = c; c = tmp;
        }
        return bufs[(n + m) % 3][n];
    }

    static int parallelScore(byte[] a, byte[] b, int threads) throws Exception {
        return "diag".equals(System.getProperty("mode")) ? diagScore(a, b, threads) : tiledScore(a, b, threads);
    }

    // ---------- Helpers ----------
    static byte[] randomDNA(int len, long seed) {
        Random r = new Random(seed);
        byte[] s = new byte[len];
        byte[] alpha = {'A', 'T', 'C', 'G'};
        for (int i = 0; i < len; i++) s[i] = alpha[r.nextInt(4)];
        return s;
    }

    static byte[] readSeq(String path) throws IOException {
        String s = new String(Files.readAllBytes(Paths.get(path))).replaceAll("\\s+", "");
        return s.toUpperCase().getBytes();
    }

    static double medianMs(long[] t) { Arrays.sort(t); return t[t.length / 2] / 1e6; }

    public static void main(String[] args) throws Exception {
        boolean csv = "true".equals(System.getProperty("csv"));
        java.io.PrintStream info = csv ? System.err : System.out;
        byte[] a, b; int idx;
        if (args.length >= 2 && args[0].equals("-d")) {          // dataset file: first two non-empty lines
            List<String> lines = new ArrayList<>();
            for (String ln : Files.readAllLines(Paths.get(args[1]))) if (!ln.trim().isEmpty()) lines.add(ln.trim());
            if (lines.size() < 2) throw new IOException("Dataset needs two sequences (two lines): " + args[1]);
            a = lines.get(0).toUpperCase().getBytes(); b = lines.get(1).toUpperCase().getBytes(); idx = 2;
        } else if (args.length >= 3 && args[0].equals("-f")) {
            a = readSeq(args[1]); b = readSeq(args[2]); idx = 3;
        } else {
            int size = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
            a = randomDNA(size, 42); b = randomDNA(size, 4242); idx = 1;
        }
        int[] threadList = {1, 2, 4, 8};
        if (args.length > idx) {
            String[] p = args[idx].split(",");
            threadList = new int[p.length];
            for (int i = 0; i < p.length; i++) threadList[i] = Integer.parseInt(p[i].trim());
        }

        int seqScore = sequentialScore(a, b);                     // warm-up + reference score
        long[] ts = new long[REPS];
        for (int r = 0; r < REPS; r++) { long s = System.nanoTime(); sequentialScore(a, b); ts[r] = System.nanoTime() - s; }
        double seqMs = medianMs(ts);

        int cores = Runtime.getRuntime().availableProcessors();
        info.printf("Lengths %d x %d | cores: %d | mode: %s | Java %s%n", a.length, b.length, cores,
                System.getProperty("mode", "tiled"), System.getProperty("java.version"));
        info.printf("Sequential score = %d, median time = %.3f ms%n", seqScore, seqMs);
        for (int th : threadList) if (th > cores)
            info.println("NOTE: " + th + " threads requested but only " + cores + " cores: expect no further speedup.");
        if (!csv) System.out.println("size,threads,seq_ms,par_ms,speedup,efficiency,seq_score,par_score,match");

        for (int th : threadList) {
            int score = parallelScore(a, b, th);                  // warm-up
            long[] tp = new long[REPS];
            for (int r = 0; r < REPS; r++) {
                long s = System.nanoTime(); score = parallelScore(a, b, th); tp[r] = System.nanoTime() - s;
            }
            double parMs = medianMs(tp), speedup = seqMs / parMs;
            System.out.printf("%d,%d,%.3f,%.3f,%.3f,%.3f,%d,%d,%s%n", a.length, th, seqMs, parMs,
                    speedup, speedup / th, seqScore, score, seqScore == score ? "PASS" : "FAIL");
        }
    }
}
