# Thí nghiệm CUTOFF cho Mergesort (Insertion sort cho mảng con nhỏ)

## Cấu trúc
- `src/MergeSort.java`     : cài đặt Mergesort + Insertion sort (tham số `cutoff`); `main` chạy cả quy trình
- `src/DataGenerator.java` : sinh 16 file dữ liệu vào `datasets/` (4 kích thước x 4 loại)
- `src/Benchmark.java`     : chạy thí nghiệm (17 giá trị CUTOFF), ghi `results/*.csv`
- `src/ChartPlotter.java`  : vẽ đồ thị bằng Java2D, ghi `results/charts/*.png`
- `datasets/`, `results/`  : dữ liệu thí nghiệm và kết quả
- `BaoCao_ThiNghiem_CUTOFF_Mergesort.docx` : báo cáo
- `AI_chat_log.md`         : tóm tắt log trao đổi với AI

## Chạy lại (cần JDK 17+; chạy ở thư mục gốc project)
    javac -encoding UTF-8 -d out src/*.java
    java -cp out MergeSort
Mất vài phút. Kết quả ghi đè vào `results/`.
