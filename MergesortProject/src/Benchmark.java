import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Thí nghiệm tìm giá trị CUTOFF tốt nhất cho Mergesort lai Insertion sort.
 *
 * Thiết kế thí nghiệm:
 *  - 4 kích thước (1.000 ... 1.000.000) x 4 loại dữ liệu (random/ascending/descending/duplicates).
 *  - Mỗi cấu hình thử nhiều giá trị CUTOFF (không chỉ 7 và 10).
 *  - Khởi động JIT (warm-up) trước khi đo.
 *  - Mỗi lượt đo (rep) chạy TẤT CẢ các CUTOFF theo THỨ TỰ NGẪU NHIÊN trên cùng một dữ liệu,
 *    để loại bỏ thiên lệch do thứ tự chạy và cho phép so sánh theo cặp (paired).
 *  - Chỉ đo thời gian sort (copy mảng nằm ngoài vùng đo).
 *  - Xuất: results/raw.csv (từng lần đo), results/summary.csv (thống kê), results/paired_7_vs_10.csv,
 *          results/best_cutoff.csv, results/overall_ranking.csv, results/machine.txt
 */
public class Benchmark {
    static final String DATA_DIR = "datasets";
    static final String OUT_DIR = "results";
    static final int[] SIZES = {1000, 10000, 100000, 1000000};
    static final String[] TYPES = {"random", "ascending", "descending", "duplicates"};
    static final int[] CUTOFFS = {1, 2, 4, 5, 6, 7, 8, 10, 12, 15, 20, 25, 30, 40, 50, 75, 100};

    static int repsFor(int size) {
        if (size <= 1000) return 300;
        if (size <= 10000) return 150;
        if (size <= 100000) return 40;
        return 15;
    }

    static int warmupRoundsFor(int size) {
        return Math.max(3, 300000 / size);
    }

    public static int[] loadFromFile(String fileName) throws IOException {
        try (Stream<String> lines = Files.lines(Path.of(fileName))) {
            return lines.map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }
    }

    static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    // ---------- thống kê ----------
    static double mean(double[] x) {
        double s = 0;
        for (double v : x) s += v;
        return s / x.length;
    }

    static double std(double[] x) {
        if (x.length < 2) return 0;
        double m = mean(x), s = 0;
        for (double v : x) s += (v - m) * (v - m);
        return Math.sqrt(s / (x.length - 1));
    }

    static double median(double[] x) {
        double[] c = x.clone();
        Arrays.sort(c);
        int n = c.length;
        return n % 2 == 1 ? c[n / 2] : (c[n / 2 - 1] + c[n / 2]) / 2.0;
    }

    /** Giá trị tới hạn t (hai phía, 95%) theo bậc tự do. */
    static double tCrit95(int df) {
        double[] t = {12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
                2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
                2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042};
        if (df < 1) return Double.NaN;
        if (df <= 30) return t[df - 1];
        if (df <= 40) return 2.021;
        if (df <= 60) return 2.000;
        if (df <= 120) return 1.980;
        return 1.960;
    }

    static void machineInfo() throws IOException {
        Runtime rt = Runtime.getRuntime();
        String s = "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")\n"
                + "Java: " + System.getProperty("java.version") + " - " + System.getProperty("java.vm.name") + "\n"
                + "CPU logical cores: " + rt.availableProcessors() + "\n"
                + "JVM max heap: " + rt.maxMemory() / (1024 * 1024) + " MB\n";
        Files.writeString(Path.of(OUT_DIR, "machine.txt"), s);
        System.out.println(s);
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of(OUT_DIR));
        machineInfo();

        MergeSort sorter = new MergeSort();
        Random rnd = new Random(2024);
        int nc = CUTOFFS.length;

        try (BufferedWriter raw = Files.newBufferedWriter(Path.of(OUT_DIR, "raw.csv"));
             BufferedWriter sum = Files.newBufferedWriter(Path.of(OUT_DIR, "summary.csv"));
             BufferedWriter pair = Files.newBufferedWriter(Path.of(OUT_DIR, "paired_7_vs_10.csv"));
             BufferedWriter best = Files.newBufferedWriter(Path.of(OUT_DIR, "best_cutoff.csv"))) {

            raw.write("size,type,cutoff,rep,nanos\n");
            sum.write("size,type,cutoff,n,mean_ms,median_ms,std_ms,ci95_ms,min_ms,max_ms,rel_best\n");
            pair.write("size,type,n,mean_t7_ms,mean_t10_ms,mean_diff_ms,ci95_ms,t_stat,faster\n");
            best.write("size,type,best_cutoff,best_median_ms,median_c7_ms,median_c10_ms\n");

            // log-sum của (median / best median) cho từng cutoff, để xếp hạng tổng thể
            int ncfg = SIZES.length * TYPES.length;
            double[][] relTable = new double[ncfg][nc];
            int cfg = 0;

            for (int size : SIZES) {
                for (String type : TYPES) {
                    String file = DATA_DIR + "/" + type + "_" + size + ".csv";
                    int[] original = loadFromFile(file);
                    int[] expected = original.clone();
                    Arrays.sort(expected);
                    int[] work = new int[original.length];
                    int reps = repsFor(size);

                    // 1. kiểm tra tính đúng đắn của mọi CUTOFF
                    for (int c : CUTOFFS) {
                        System.arraycopy(original, 0, work, 0, work.length);
                        sorter.mergeSort(work, c);
                        if (!Arrays.equals(work, expected))
                            throw new IllegalStateException("Sort SAI: " + file + " cutoff=" + c);
                    }

                    // 2. warm-up JIT
                    int wr = warmupRoundsFor(size);
                    for (int w = 0; w < wr; w++) {
                        for (int c : CUTOFFS) {
                            System.arraycopy(original, 0, work, 0, work.length);
                            sorter.mergeSort(work, c);
                        }
                    }

                    // 3. đo: mỗi rep chạy mọi cutoff theo thứ tự ngẫu nhiên
                    double[][] ms = new double[nc][reps];
                    int[] order = new int[nc];
                    for (int i = 0; i < nc; i++) order[i] = i;
                    for (int r = 0; r < reps; r++) {
                        for (int i = nc - 1; i > 0; i--) { // Fisher-Yates
                            int j = rnd.nextInt(i + 1);
                            int t = order[i]; order[i] = order[j]; order[j] = t;
                        }
                        for (int ci : order) {
                            System.arraycopy(original, 0, work, 0, work.length);
                            long t0 = System.nanoTime();
                            sorter.mergeSort(work, CUTOFFS[ci]);
                            long dt = System.nanoTime() - t0;
                            ms[ci][r] = dt / 1e6;
                            raw.write(size + "," + type + "," + CUTOFFS[ci] + "," + r + "," + dt + "\n");
                        }
                    }

                    // 4. thống kê từng cutoff
                    double[] med = new double[nc];
                    double bestMed = Double.MAX_VALUE;
                    int bestIdx = 0;
                    for (int ci = 0; ci < nc; ci++) {
                        med[ci] = median(ms[ci]);
                        if (med[ci] < bestMed) { bestMed = med[ci]; bestIdx = ci; }
                    }
                    int i7 = -1, i10 = -1;
                    for (int ci = 0; ci < nc; ci++) {
                        double[] x = ms[ci];
                        double ci95 = tCrit95(reps - 1) * std(x) / Math.sqrt(reps);
                        double mn = Arrays.stream(x).min().getAsDouble();
                        double mx = Arrays.stream(x).max().getAsDouble();
                        sum.write(String.format("%d,%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                                size, type, CUTOFFS[ci], reps, mean(x), med[ci], std(x), ci95, mn, mx, med[ci] / bestMed));
                        relTable[cfg][ci] = med[ci] / bestMed;
                        if (CUTOFFS[ci] == 7) i7 = ci;
                        if (CUTOFFS[ci] == 10) i10 = ci;
                    }
                    best.write(String.format("%d,%s,%d,%.4f,%.4f,%.4f%n",
                            size, type, CUTOFFS[bestIdx], bestMed, med[i7], med[i10]));

                    // 5. so sánh cặp CUTOFF 7 vs 10
                    double[] d = new double[reps];
                    for (int r = 0; r < reps; r++) d[r] = ms[i10][r] - ms[i7][r];
                    double md = mean(d);
                    double ci = tCrit95(reps - 1) * std(d) / Math.sqrt(reps);
                    double tstat = md / (std(d) / Math.sqrt(reps));
                    String faster = Math.abs(md) <= ci ? "khong khac biet" : (md < 0 ? "CUTOFF 10 nhanh hon" : "CUTOFF 7 nhanh hon");
                    pair.write(String.format("%d,%s,%d,%.4f,%.4f,%.4f,%.4f,%.3f,%s%n",
                            size, type, reps, mean(ms[i7]), mean(ms[i10]), md, ci, tstat, faster));

                    System.out.printf("size=%-8d %-11s best=%-3d (%.3f ms) | c7=%.3f ms | c10=%.3f ms | %s%n",
                            size, type, CUTOFFS[bestIdx], bestMed, med[i7], med[i10], faster);
                    raw.flush(); sum.flush(); pair.flush(); best.flush();
                    cfg++;
                }
            }

            // 6. xếp hạng tổng thể bằng trung bình nhân của tỉ lệ (median / best)
            try (BufferedWriter rank = Files.newBufferedWriter(Path.of(OUT_DIR, "overall_ranking.csv"))) {
                rank.write("cutoff,geomean_all,geomean_random,geomean_size_ge_100000,worst_case_ratio\n");
                for (int ci = 0; ci < nc; ci++) {
                    double sAll = 0, sRnd = 0, sBig = 0, worst = 0;
                    int nAll = 0, nRnd = 0, nBig = 0, k = 0;
                    for (int size : SIZES) {
                        for (String type : TYPES) {
                            double lr = Math.log(relTable[k][ci]);
                            sAll += lr; nAll++;
                            if (type.equals("random")) { sRnd += lr; nRnd++; }
                            if (size >= 100000) { sBig += lr; nBig++; }
                            worst = Math.max(worst, relTable[k][ci]);
                            k++;
                        }
                    }
                    rank.write(String.format("%d,%.4f,%.4f,%.4f,%.4f%n", CUTOFFS[ci],
                            Math.exp(sAll / nAll), Math.exp(sRnd / nRnd), Math.exp(sBig / nBig), worst));
                }
            }
        }
        System.out.println("Da ghi ket qua vao thu muc '" + OUT_DIR + "'.");
    }
}
