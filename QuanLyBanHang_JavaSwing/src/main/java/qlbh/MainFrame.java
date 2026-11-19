package qlbh;

import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class MainFrame extends JFrame {
    private final JLabel connectionStatus = new JLabel("Chưa kiểm tra kết nối SQL Server");
    private final JLabel stats = new JLabel("Số liệu sẽ hiển thị khi kết nối thành công.");

    public MainFrame() {
        Ui.frame(this, "Trang chủ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 620);
        setLayout(new BorderLayout(0, 18));
        add(Ui.header("QUẢN LÝ BÁN HÀNG", "Đồ án Java Swing • Microsoft SQL Server • 5 form quản lý độc lập"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 32, 32, 32));
        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setOpaque(false);
        addNav(grid, "01  NHÂN VIÊN", "Thêm, sửa, xóa, tìm kiếm nhân viên", () -> new NhanVienFrame().setVisible(true));
        addNav(grid, "02  SẢN PHẨM", "Quản lý danh mục và giá sản phẩm", () -> new SanPhamFrame().setVisible(true));
        addNav(grid, "03  KHÁCH HÀNG", "Thông tin và doanh số khách hàng", () -> new KhachHangFrame().setVisible(true));
        addNav(grid, "04  HÓA ĐƠN", "Lập, tìm kiếm, sửa và xóa hóa đơn", () -> new HoaDonFrame().setVisible(true));
        addNav(grid, "05  CHI TIẾT HÓA ĐƠN", "Sản phẩm đã bán và số lượng", () -> new ChiTietHoaDonFrame(null).setVisible(true));
        addNav(grid, "KIỂM TRA SQL SERVER", "Xác nhận cấu hình và xem thống kê", this::checkConnection);
        content.add(grid, BorderLayout.CENTER);

        JPanel bottom = Ui.panel(new GridLayout(2, 1, 0, 8));
        bottom.setBorder(new EmptyBorder(16, 18, 16, 18));
        connectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        stats.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bottom.add(connectionStatus);
        bottom.add(stats);
        content.add(bottom, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

    private void addNav(JPanel grid, String text, String subtitle, Runnable action) {
        JButton b = new JButton("<html><div style='padding:8px 4px'><b>" + text
                + "</b><br><span style='font-size:10px'>" + subtitle + "</span></div></html>");
        b.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBackground(Color.WHITE);
        b.setForeground(Ui.BLUE);
        b.setFocusPainted(false);
        b.addActionListener(e -> action.run());
        grid.add(b);
    }

    private void checkConnection() {
        try (Connection c = Database.connect(); Statement s = c.createStatement()) {
            int[] counts = new int[5];
            String[] tables = {"NHANVIEN", "SANPHAM", "KHACHHANG", "HOADON", "CTHD"};
            for (int i = 0; i < tables.length; i++) {
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dbo." + tables[i])) {
                    if (r.next()) counts[i] = r.getInt(1);
                }
            }
            connectionStatus.setText("●  Kết nối thành công: " + c.getCatalog());
            connectionStatus.setForeground(new Color(26, 119, 72));
            stats.setText(String.format("Nhân viên: %d    |    Sản phẩm: %d    |    Khách hàng: %d    |    Hóa đơn: %d    |    Chi tiết: %d",
                    counts[0], counts[1], counts[2], counts[3], counts[4]));
            Ui.success(this, "Kết nối SQL Server thành công!");
        } catch (Exception ex) {
            connectionStatus.setText("●  Chưa kết nối SQL Server");
            connectionStatus.setForeground(new Color(177, 53, 53));
            Ui.error(this, ex);
        }
    }
}
