import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class DataGenerator {
    private static final Random RANDOM = new Random(42);

    public static int[] generateRandom(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = RANDOM.nextInt(size * 10);
        }
        return arr;
    }

    public static int[] generateAscending(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }
        return arr;
    }

    public static int[] generateDescending(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = size - 1 - i;
        }
        return arr;
    }

    public static int[] generateDuplicates(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = RANDOM.nextInt(10);
        }
        return arr;
    }

    public static void saveToFile(int[] data, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (int num : data) {
                writer.write(Integer.toString(num));
                writer.newLine();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int[] sizes = {1000, 10000, 100000, 1000000};
        String folder = "datasets";
        Files.createDirectories(Path.of(folder)); // Tự động tạo thư mục datasets

        for (int size : sizes) {
            String[] fileNames = {
                    folder + "/random_" + size + ".csv",
                    folder + "/ascending_" + size + ".csv",
                    folder + "/descending_" + size + ".csv",
                    folder + "/duplicates_" + size + ".csv"
            };

            // Nếu cả 4 file đã tồn tại thì bỏ qua kích thước này
            boolean allExist = true;

            for (String fileName : fileNames) {
                if (!Files.exists(Path.of(fileName))) {
                    allExist = false;
                    break;
                }
            }

            if (allExist) {
                System.out.println("Đã tồn tại dữ liệu size = " + size
                        + ", bỏ qua tạo file.");
                continue;
            }

            // Chỉ tạo những file còn thiếu
            if (!Files.exists(Path.of(fileNames[0]))) {
                saveToFile(generateRandom(size), fileNames[0]);
            }

            if (!Files.exists(Path.of(fileNames[1]))) {
                saveToFile(generateAscending(size), fileNames[1]);
            }

            if (!Files.exists(Path.of(fileNames[2]))) {
                saveToFile(generateDescending(size), fileNames[2]);
            }

            if (!Files.exists(Path.of(fileNames[3]))) {
                saveToFile(generateDuplicates(size), fileNames[3]);
            }

            System.out.println("Đã kiểm tra và tạo dữ liệu size = " + size);
        }
    }
}