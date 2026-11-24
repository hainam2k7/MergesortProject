import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class Benchmark {
    public static int[] loadFromFile(String fileName) throws IOException {
        try (Stream<String> lines = Files.lines(Path.of(fileName))) {
            return lines.map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }
    }

    public static void main(String[] args) throws IOException {
        MergeSort sorter = new MergeSort();
        int[] sizes = {1000, 10000, 100000, 1000000};
        String[] files = {"random", "ascending", "descending", "duplicates"};

        for (int size : sizes) {
            System.out.println("=== KÍCH THƯỚC DỮ LIỆU: " + size + " ===");
            for (String file : files) {
                int[] originalData = loadFromFile("datasets/" + file + "_" + size + ".csv");

                int[] dataForCutoff7 = Arrays.copyOf(originalData, originalData.length);
                int[] dataForCutoff10 = Arrays.copyOf(originalData, originalData.length);

                // Đo thời gian chạy CUTOFF = 7
                long start7 = System.nanoTime();
                sorter.mergeSort(dataForCutoff7, MergeSort.CUTOFF_7);
                long time7 = System.nanoTime() - start7;

                // Đo thời gian chạy CUTOFF = 10
                long start10 = System.nanoTime();
                sorter.mergeSort(dataForCutoff10, MergeSort.CUTOFF_10);
                long time10 = System.nanoTime() - start10;

                System.out.printf("File %-15s | CUTOFF 7: %.3f ms | CUTOFF 10: %.3f ms%n",
                        file, time7 / 1_000_000.0, time10 / 1_000_000.0);
            }
        }

    }
}