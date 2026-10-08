import java.io.*;
import java.util.*;

public class PerformanceAnalysis {

    static class Result {
        int size;
        int threads;
        double seqTime;
        double parTime;
        double speedup;
        double efficiency;

        Result(int size, int threads, double seqTime,
               double parTime, double speedup, double efficiency) {
            this.size = size;
            this.threads = threads;
            this.seqTime = seqTime;
            this.parTime = parTime;
            this.speedup = speedup;
            this.efficiency = efficiency;
        }
    }

    static List<Result> readCSV(String file) throws Exception {

        List<Result> results = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;
        br.readLine(); // skip header

        while ((line = br.readLine()) != null) {

            String[] p = line.split(",");

            int size = Integer.parseInt(p[0]);
            int threads = Integer.parseInt(p[1]);

            double seqTime = Double.parseDouble(p[2]);
            double parTime = Double.parseDouble(p[3]);
            double speedup = Double.parseDouble(p[4]);
            double efficiency = Double.parseDouble(p[5]);

            results.add(new Result(
                    size,
                    threads,
                    seqTime,
                    parTime,
                    speedup,
                    efficiency
            ));
        }

        br.close();

        return results;
    }

    static void printAnalysis(List<Result> results) {

        System.out.println("===== PERFORMANCE ANALYSIS =====");

        for (Result r : results) {

            System.out.printf(
                    "Size: %d | Threads: %d | " +
                    "Sequential: %.3f ms | " +
                    "Parallel: %.3f ms | " +
                    "Speedup: %.3f | " +
                    "Efficiency: %.3f%n",
                    r.size,
                    r.threads,
                    r.seqTime,
                    r.parTime,
                    r.speedup,
                    r.efficiency
            );
        }
    }

    static void findBestSpeedup(List<Result> results) {

        Result best = results.get(0);

        for (Result r : results) {

            if (r.speedup > best.speedup) {
                best = r;
            }
        }

        System.out.println();
        System.out.println("===== BEST SPEEDUP =====");

        System.out.printf(
                "Size: %d | Threads: %d | Speedup: %.3f%n",
                best.size,
                best.threads,
                best.speedup
        );
    }

    static void saveSummary(List<Result> results,
                            String file) throws Exception {

        PrintWriter out = new PrintWriter(new FileWriter(file));

        out.println(
                "size,threads,seq_ms,par_ms,speedup,efficiency"
        );

        for (Result r : results) {

            out.printf(
                    "%d,%d,%.3f,%.3f,%.3f,%.3f%n",
                    r.size,
                    r.threads,
                    r.seqTime,
                    r.parTime,
                    r.speedup,
                    r.efficiency
            );
        }

        out.close();
    }

    public static void main(String[] args) throws Exception {

        String input = "results/performance.csv";

        List<Result> results = readCSV(input);

        printAnalysis(results);

        findBestSpeedup(results);

        saveSummary(
                results,
                "results/performance_summary.csv"
        );

        System.out.println();
        System.out.println(
                "Performance analysis completed."
        );
    }
}