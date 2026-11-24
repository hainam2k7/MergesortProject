public class MergeSort {
    public static final int CUTOFF_7 = 7;
    public static final int CUTOFF_10 = 10;

    private static void insertionSort(int[] arr, int left, int right) {
        for (int i =left + 1; i <= right; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= left && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }
    private void merge(int arr[], int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        int[] left_array = new int[n1];
        int[] right_array = new int[n2];

        for (int i = 0; i < n1; i++) {
            left_array[i] = arr[left + i];
        }
        for (int j = 0; j < n2; j++) {
            right_array[j] = arr[mid + j + 1];
        }

        int i = 0; int j = 0; int k = left;
        while (i < n1 && j < n2) {
            if (left_array[i] <= right_array[j]) {
                arr[k] = left_array[i];
                k++; i++;
            } else {
                arr[k] = right_array[j];
                k++; j++;
            }
        }
        while (i < n1) {
            arr[k] = left_array[i];
            k++; i++;
        }
        while (j < n2) {
            arr[k] = right_array[j];
            k++; j++;
        }
    }
    public void mergeSort(int[] arr, int cutoff) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        mergeSort(arr, 0, arr.length - 1, cutoff);
    }
    private void mergeSort(int[] arr, int left, int right, int cutoff) {
        if (left < right) {
            if (right - left + 1 <= cutoff) {
                insertionSort(arr, left, right);
                return;
            }
            int mid = left + (right - left) / 2; //tránh tràn số

            mergeSort(arr, left, mid, cutoff);
            mergeSort(arr, mid + 1, right, cutoff);

            merge(arr, left, mid, right);
        }
    }
    public static void main(String[] args) throws java.io.IOException {

        System.out.println("===== BUOC 1: TAO DU LIEU =====");
        DataGenerator.main(args);

        System.out.println("\n===== BUOC 2: CHAY THI NGHIEM =====");
        Benchmark.main(args);

        System.out.println("\n===== HOAN THANH THI NGHIEM =====");
    }
}