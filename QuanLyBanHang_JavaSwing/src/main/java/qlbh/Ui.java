package qlbh;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public final class Ui {
    public static final Color BLUE = new Color(28, 91, 158);
    public static final Color BG = new Color(246, 249, 253);
    public static final Color DARK = new Color(34, 52, 70);
    private Ui() {}

    public static JButton button(String name) {
        JButton b = new JButton(name);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setMargin(new Insets(9, 12, 9, 12));
        return b;
    }
    public static JTextField field() {
        JTextField t = new JTextField(18);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setPreferredSize(new Dimension(230, 33));
        return t;
    }
    public static JLabel label(String content) {
        JLabel l = new JLabel(content);
        l.setForeground(DARK);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }
    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(Color.WHITE);
        return p;
    }
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }
    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(29);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(231, 240, 250));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        return table;
    }
    public static JPanel formRow(String title, JComponent component) {
        JPanel row = panel(new BorderLayout(0, 5));
        row.add(label(title), BorderLayout.NORTH);
        row.add(component, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(310, 67));
        return row;
    }
    public static JPanel formPanel() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(15, 18, 15, 18));
        return p;
    }
    public static JPanel header(String title, String subtitle) {
        JPanel p = panel(new GridLayout(2, 1));
        p.setBorder(new EmptyBorder(15, 20, 12, 20));
        JLabel main = new JLabel(title);
        main.setForeground(BLUE);
        main.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel sub = label(subtitle);
        sub.setForeground(new Color(92, 108, 126));
        p.add(main);
        p.add(sub);
        return p;
    }
    public static void frame(JFrame frame, String title) {
        frame.setTitle(title + " | QUẢN LÝ BÁN HÀNG");
        frame.setSize(1120, 700);
        frame.setMinimumSize(new Dimension(960, 590));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getContentPane().setBackground(BG);
    }
    public static void success(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
    public static void error(Component parent, Exception ex) {
        String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        if (ex instanceof SQLException sql) {
            if (sql.getErrorCode() == 2627 || sql.getErrorCode() == 2601)
                message = "Mã đã tồn tại hoặc dữ liệu bị trùng khóa chính.";
            else if (sql.getErrorCode() == 547)
                message = "Dữ liệu đang được sử dụng hoặc vi phạm khóa ngoại/ràng buộc. "
                        + "Hãy kiểm tra hóa đơn, nhân viên, khách hàng và sản phẩm liên quan.";
            else if (sql.getErrorCode() == 18456)
                message = "Sai tài khoản/mật khẩu SQL Server hoặc tài khoản chưa được cấp quyền.";
            else if (sql.getErrorCode() == 208)
                message = "Không tìm thấy bảng. Hãy chạy script sql/01_tao_csdl.sql trước.";
        }
        JOptionPane.showMessageDialog(parent, message, "Có lỗi", JOptionPane.ERROR_MESSAGE);
    }
    public static String required(String value, String label) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(label + " không được để trống.");
        return trimmed;
    }
    public static String date(String v, boolean required, String label) {
        String s = v.trim();
        if (s.isEmpty() && !required) return "";
        if (s.isEmpty()) throw new IllegalArgumentException(label + " không được để trống.");
        try { return LocalDate.parse(s).toString(); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(label + ": dùng định dạng yyyy-MM-dd (ví dụ 2026-11-20)."); }
    }
    public static BigDecimal money(String v, String label) {
        try {
            BigDecimal value = new BigDecimal(required(v, label));
            if (value.signum() < 0) throw new NumberFormatException();
            if (value.scale() > 4) throw new IllegalArgumentException(label + ": tối đa 4 số thập phân.");
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " phải là số không âm (không dùng dấu phân cách hàng nghìn).");
        }
    }
    public static int positiveInt(String v, String label) {
        try {
            int value = Integer.parseInt(required(v, label));
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " phải là số nguyên dương.");
        }
    }
    public static String formatMoney(BigDecimal value) {
        if (value == null) return "0";
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        fmt.setMaximumFractionDigits(2);
        fmt.setMinimumFractionDigits(0);
        return fmt.format(value) + " đ";
    }
}
