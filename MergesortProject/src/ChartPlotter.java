import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Vẽ đồ thị bằng Java2D (không cần thư viện ngoài) từ results/summary.csv, results/paired_7_vs_10.csv
 * và results/overall_ranking.csv. Ảnh PNG được ghi vào results/charts/.
 */
public class ChartPlotter {
    static final String OUT = Benchmark.OUT_DIR + "/charts";
    static final Color[] PALETTE = {
            new Color(0x1f77b4), new Color(0xd62728), new Color(0x2ca02c), new Color(0xff7f0e), new Color(0x9467bd)};

    // ---------------- đọc dữ liệu ----------------
    record Row(int size, String type, int cutoff, double median, double rel) {}

    static List<Row> readSummary() throws IOException {
        List<Row> rows = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(Benchmark.OUT_DIR, "summary.csv"));
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split(",");
            rows.add(new Row(Integer.parseInt(p[0]), p[1], Integer.parseInt(p[2]),
                    Double.parseDouble(p[5]), Double.parseDouble(p[10])));
        }
        return rows;
    }

    static double rel(List<Row> rows, int size, String type, int cutoff) {
        for (Row r : rows) if (r.size == size && r.type.equals(type) && r.cutoff == cutoff) return r.rel;
        throw new IllegalArgumentException();
    }

    static double med(List<Row> rows, int size, String type, int cutoff) {
        for (Row r : rows) if (r.size == size && r.type.equals(type) && r.cutoff == cutoff) return r.median;
        throw new IllegalArgumentException();
    }

    // ---------------- vẽ ----------------
    static Graphics2D canvas(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        return g;
    }

    static void centered(Graphics2D g, String s, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, y);
    }

    static double[] niceRange(double lo, double hi) {
        double span = hi - lo;
        if (span < 1e-9) span = 0.1;
        double step = Math.pow(10, Math.floor(Math.log10(span / 5)));
        double[] mult = {1, 2, 2.5, 5, 10};
        for (double m : mult) if (span / (step * m) <= 8) { step *= m; break; }
        double a = Math.floor(lo / step) * step, b = Math.ceil(hi / step) * step;
        return new double[]{a, b, step};
    }

    /** Đồ thị đường: trục x là nhãn (cutoff) cách đều nhau. */
    static void lineChart(String file, String title, String xLabel, String yLabel,
                          String[] xTicks, String[] names, double[][] ys, int[] markX) throws IOException {
        int W = 1000, H = 640, L = 85, R = 40, T = 70, B = 140;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(img);
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (double[] s : ys) for (double v : s) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
        double[] nr = niceRange(Math.min(lo, 1.0) - 0.02 * (hi - lo), hi + 0.03 * (hi - lo));
        double y0 = nr[0], y1 = nr[1], step = nr[2];
        int pw = W - L - R, ph = H - T - B, n = xTicks.length;

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.BLACK);
        centered(g, title, W / 2, 38);

        // lưới + trục y
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        for (double v = y0; v <= y1 + step * 1e-6; v += step) {
            int y = T + (int) Math.round((y1 - v) / (y1 - y0) * ph);
            g.setColor(new Color(225, 225, 225));
            g.drawLine(L, y, L + pw, y);
            g.setColor(Color.DARK_GRAY);
            String s = String.format("%.2f", v);
            g.drawString(s, L - 8 - g.getFontMetrics().stringWidth(s), y + 5);
        }
        // đường đánh dấu cutoff 7 và 10
        for (int i = 0; i < n; i++) {
            int x = L + (int) Math.round((i + 0.5) * pw / n);
            boolean mark = false;
            for (int m : markX) if (xTicks[i].equals(String.valueOf(m))) mark = true;
            if (mark) {
                g.setColor(new Color(255, 235, 150));
                g.fillRect(x - 6, T, 12, ph);
            }
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(L, T, L, T + ph);
        g.drawLine(L, T + ph, L + pw, T + ph);
        for (int i = 0; i < n; i++) {
            int x = L + (int) Math.round((i + 0.5) * pw / n);
            g.drawLine(x, T + ph, x, T + ph + 5);
            centered(g, xTicks[i], x, T + ph + 22);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        centered(g, xLabel, L + pw / 2, T + ph + 48);
        g.rotate(-Math.PI / 2);
        centered(g, yLabel, -(T + ph / 2), 24);
        g.rotate(Math.PI / 2);

        // đường dữ liệu
        for (int s = 0; s < ys.length; s++) {
            g.setColor(PALETTE[s % PALETTE.length]);
            g.setStroke(new BasicStroke(2.4f));
            int px = 0, py = 0;
            int minI = 0;
            for (int i = 0; i < n; i++) {
                int x = L + (int) Math.round((i + 0.5) * pw / n);
                int y = T + (int) Math.round((y1 - ys[s][i]) / (y1 - y0) * ph);
                if (i > 0) g.drawLine(px, py, x, y);
                px = x; py = y;
                if (ys[s][i] < ys[s][minI]) minI = i;
            }
            for (int i = 0; i < n; i++) {
                int x = L + (int) Math.round((i + 0.5) * pw / n);
                int y = T + (int) Math.round((y1 - ys[s][i]) / (y1 - y0) * ph);
                int r = (i == minI) ? 7 : 3;
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
            }
        }
        // chú giải
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        int lx = L, ly = H - 52;
        for (int s = 0; s < names.length; s++) {
            g.setColor(PALETTE[s % PALETTE.length]);
            g.fillRect(lx, ly - 10, 22, 6);
            g.setColor(Color.BLACK);
            g.drawString(names[s], lx + 28, ly);
            lx += 40 + g.getFontMetrics().stringWidth(names[s]) + 20;
        }
        g.setColor(Color.GRAY);
        g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g.drawString("Vòng tròn lớn = CUTOFF tốt nhất của đường đó; dải vàng = CUTOFF 7 và 10.", L, H - 22);
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }

    /** Đồ thị cột lệch quanh đường cơ sở 1.0 : tỉ lệ thời gian CUTOFF 10 / CUTOFF 7. */
    static void barChart(String file, String title, String[] cats, double[] vals, int[] colorIdx, String[] legend)
            throws IOException {
        int W = 1100, H = 640, L = 85, R = 40, T = 70, B = 150;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(img);
        double lo = 1, hi = 1;
        for (double v : vals) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
        double[] nr = niceRange(lo - 0.02, hi + 0.02);
        double y0 = nr[0], y1 = nr[1], step = nr[2];
        int pw = W - L - R, ph = H - T - B, n = cats.length;

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.BLACK);
        centered(g, title, W / 2, 38);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        for (double v = y0; v <= y1 + step * 1e-6; v += step) {
            int y = T + (int) Math.round((y1 - v) / (y1 - y0) * ph);
            g.setColor(new Color(225, 225, 225));
            g.drawLine(L, y, L + pw, y);
            g.setColor(Color.DARK_GRAY);
            String s = String.format("%.2f", v);
            g.drawString(s, L - 8 - g.getFontMetrics().stringWidth(s), y + 5);
        }
        int yBase = T + (int) Math.round((y1 - 1.0) / (y1 - y0) * ph);
        int slot = pw / n, bw = (int) (slot * 0.62);
        for (int i = 0; i < n; i++) {
            int cx = L + i * slot + slot / 2;
            int yv = T + (int) Math.round((y1 - vals[i]) / (y1 - y0) * ph);
            g.setColor(PALETTE[colorIdx[i]]);
            g.fillRect(cx - bw / 2, Math.min(yv, yBase), bw, Math.max(1, Math.abs(yv - yBase)));
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            centered(g, String.format("%.2f", vals[i]), cx, vals[i] >= 1 ? yv - 5 : yv + 15);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.rotate(-Math.PI / 4, cx, T + ph + 18);
            g.drawString(cats[i], cx - 56, T + ph + 22);
            g.rotate(Math.PI / 4, cx, T + ph + 18);
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(L, yBase, L + pw, yBase);
        g.drawLine(L, T, L, T + ph);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.rotate(-Math.PI / 2);
        centered(g, "Thời gian CUTOFF 10 / thời gian CUTOFF 7", -(T + ph / 2), 24);
        g.rotate(Math.PI / 2);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        int lx = L, ly = H - 44;
        for (int s = 0; s < legend.length; s++) {
            g.setColor(PALETTE[s]);
            g.fillRect(lx, ly - 11, 16, 12);
            g.setColor(Color.BLACK);
            g.drawString(legend[s], lx + 22, ly);
            lx += 50 + g.getFontMetrics().stringWidth(legend[s]);
        }
        g.setColor(Color.GRAY);
        g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g.drawString("Giá trị < 1: CUTOFF 10 nhanh hơn; > 1: CUTOFF 7 nhanh hơn (so sánh theo trung vị).", L, H - 14);
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }

    static String sizeLabel(int s) {
        return switch (s) {
            case 1000 -> "1.000";
            case 10000 -> "10.000";
            case 100000 -> "100.000";
            default -> "1.000.000";
        };
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Files.createDirectories(Path.of(OUT));
        List<Row> rows = readSummary();
        int[] cut = Benchmark.CUTOFFS;
        String[] xt = new String[cut.length];
        for (int i = 0; i < cut.length; i++) xt[i] = String.valueOf(cut[i]);
        int[] mark = {7, 10};

        // 1. mỗi kích thước một đồ thị: tỉ lệ thời gian (so với CUTOFF tốt nhất) theo cutoff, 4 loại dữ liệu
        for (int size : Benchmark.SIZES) {
            double[][] ys = new double[Benchmark.TYPES.length][cut.length];
            for (int t = 0; t < Benchmark.TYPES.length; t++)
                for (int c = 0; c < cut.length; c++) ys[t][c] = rel(rows, size, Benchmark.TYPES[t], cut[c]);
            lineChart(OUT + "/relative_" + size + ".png",
                    "Thời gian tương đối theo CUTOFF - n = " + sizeLabel(size),
                    "Giá trị CUTOFF", "Thời gian trung vị / thời gian tốt nhất (1.0 = tốt nhất)",
                    xt, Benchmark.TYPES, ys, mark);
        }

        // 2. xếp hạng tổng thể
        List<String> rk = Files.readAllLines(Path.of(Benchmark.OUT_DIR, "overall_ranking.csv"));
        double[][] all = new double[3][cut.length];
        for (int i = 1; i < rk.size(); i++) {
            String[] p = rk.get(i).split(",");
            for (int s = 0; s < 3; s++) all[s][i - 1] = Double.parseDouble(p[1 + s]);
        }
        lineChart(OUT + "/overall.png", "Xếp hạng tổng thể các giá trị CUTOFF (trung bình nhân)",
                "Giá trị CUTOFF", "Trung bình nhân của (thời gian / thời gian tốt nhất)",
                xt, new String[]{"Tất cả 16 cấu hình", "Chỉ dữ liệu random", "Chỉ n >= 100.000"}, all, mark);

        // 3. CUTOFF 10 so với CUTOFF 7
        int nCfg = Benchmark.SIZES.length * Benchmark.TYPES.length;
        String[] cats = new String[nCfg];
        double[] vals = new double[nCfg];
        int[] col = new int[nCfg];
        int k = 0;
        for (int size : Benchmark.SIZES) {
            for (int t = 0; t < Benchmark.TYPES.length; t++) {
                String type = Benchmark.TYPES[t];
                cats[k] = type + " " + sizeLabel(size);
                vals[k] = med(rows, size, type, 10) / med(rows, size, type, 7);
                col[k] = t;
                k++;
            }
        }
        barChart(OUT + "/compare_7_vs_10.png", "CUTOFF 10 so với CUTOFF 7 trên 16 cấu hình", cats, vals, col, Benchmark.TYPES);
        System.out.println("Da ve do thi vao thu muc '" + OUT + "'.");
    }
}
