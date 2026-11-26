# Log trao đổi với AI (tóm tắt)

1. **Đọc code MergeSort + DataGenerator**: AI giải thích thuật toán, nhận xét (merge cấp phát mảng mới mỗi lần, seed dùng chung, v.v.).
2. **Lỗi NoSuchFileException**: do DataGenerator ghi vào `datasets/` nhưng Benchmark đọc ở thư mục hiện tại; sửa đường dẫn. AI cũng nêu thiếu warm-up JIT và thứ tự đo cố định.
3. **Hoàn thành project**: AI viết lại Benchmark (17 CUTOFF, warm-up, thứ tự ngẫu nhiên, thống kê, kiểm định cặp), ChartPlotter (Java2D), chạy thí nghiệm và soạn báo cáo. Cài đặt Mergesort/Insertion sort/DataGenerator là code của sinh viên, giữ nguyên.
