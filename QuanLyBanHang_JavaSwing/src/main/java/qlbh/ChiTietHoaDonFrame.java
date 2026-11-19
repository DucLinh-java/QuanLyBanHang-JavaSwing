package qlbh;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public final class ChiTietHoaDonFrame extends JFrame {
    private final JComboBox<Option> sohd = new JComboBox<>();
    private final JComboBox<Option> masp = new JComboBox<>();
    private final JTextField sl = Ui.field();
    private final JTextField search = Ui.field();
    private final JLabel status = Ui.label("Sẵn sàng");
    private final DefaultTableModel model = Ui.model("Số HĐ", "Mã SP", "Tên sản phẩm", "Số lượng", "Giá bán (VNĐ)", "Thành tiền (VNĐ)");
    private final JTable table = Ui.table(model);
    private Integer selectedInvoice;
    private String selectedProduct;

    public ChiTietHoaDonFrame(Integer preselectedInvoice) {
        Ui.frame(this, "Chi tiết hóa đơn");
        setLayout(new BorderLayout());
        add(Ui.header("CHI TIẾT HÓA ĐƠN", "Quản lý từng mặt hàng trong hóa đơn • Cập nhật trị giá và doanh số tự động"), BorderLayout.NORTH);
        JPanel left = Ui.formPanel();
        left.add(Ui.formRow("Số hóa đơn *", sohd)); left.add(Box.createVerticalStrut(10));
        left.add(Ui.formRow("Sản phẩm *", masp)); left.add(Box.createVerticalStrut(10));
        left.add(Ui.formRow("Số lượng * (số nguyên > 0)", sl)); left.add(Box.createVerticalStrut(13));
        JLabel tip = Ui.label("Mỗi sản phẩm chỉ có một dòng trong cùng hóa đơn.");
        tip.setForeground(Ui.BLUE);
        left.add(tip);
        JScrollPane leftScroll = new JScrollPane(left);
        leftScroll.setPreferredSize(new Dimension(345, 1));
        leftScroll.setBorder(null);
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(leftScroll, BorderLayout.WEST);

        JPanel right = Ui.panel(new BorderLayout(0, 10));
        right.setBorder(new EmptyBorder(0, 10, 12, 16));
        JPanel searchBar = Ui.panel(new BorderLayout(8, 0));
        searchBar.add(search, BorderLayout.CENTER);
        JPanel searchButtons = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton find = Ui.button("Tìm kiếm");
        JButton refresh = Ui.button("Tải lại");
        searchButtons.add(find); searchButtons.add(refresh);
        searchBar.add(searchButtons, BorderLayout.EAST);
        right.add(searchBar, BorderLayout.NORTH);
        right.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = Ui.panel(new BorderLayout());
        JPanel buttons = Ui.panel(new FlowLayout(FlowLayout.LEFT, 7, 5));
        JButton add = Ui.button("Thêm");
        JButton edit = Ui.button("Sửa số lượng");
        JButton delete = Ui.button("Xóa");
        JButton clear = Ui.button("Làm mới form");
        buttons.add(add); buttons.add(edit); buttons.add(delete); buttons.add(clear);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.SOUTH);
        right.add(bottom, BorderLayout.SOUTH);
        add(right, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selectRow(); });
        find.addActionListener(e -> load());
        search.addActionListener(e -> load());
        refresh.addActionListener(e -> { search.setText(""); loadOptions(); load(); });
        add.addActionListener(e -> insert());
        edit.addActionListener(e -> update());
        delete.addActionListener(e -> delete());
        clear.addActionListener(e -> { loadOptions(); clear(); });
        loadOptions(); load();
        if (preselectedInvoice != null) {
            Option.select(sohd, preselectedInvoice.toString());
            search.setText(preselectedInvoice.toString());
            load();
        }
    }
    private void loadOptions() {
        String invoice = Option.value(sohd);
        String product = Option.value(masp);
        try (Connection c = Database.connect(); Statement s = c.createStatement()) {
            sohd.removeAllItems();
            try (ResultSet rs = s.executeQuery("SELECT SOHD FROM dbo.HOADON ORDER BY SOHD DESC")) {
                while (rs.next()) {
                    String id = String.valueOf(rs.getInt(1));
                    sohd.addItem(new Option(id, "Hóa đơn #" + id));
                }
            }
            masp.removeAllItems();
            try (ResultSet rs = s.executeQuery("SELECT MASP,TENSP FROM dbo.SANPHAM ORDER BY MASP")) {
                while (rs.next()) {
                    String code = rs.getString(1).trim();
                    masp.addItem(new Option(code, code + " - " + rs.getString(2)));
                }
            }
            Option.select(sohd, invoice); Option.select(masp, product);
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void load() {
        selectedInvoice = null; selectedProduct = null;
        table.clearSelection();
        sohd.setEnabled(true); masp.setEnabled(true);
        String sql = """
            SELECT d.SOHD, d.MASP, p.TENSP, d.SL, p.GIA,
                   CAST(d.SL AS DECIMAL(19,2))*CAST(p.GIA AS DECIMAL(19,2)) AS THANHTIEN
            FROM dbo.CTHD d JOIN dbo.SANPHAM p ON d.MASP=p.MASP
            WHERE CAST(d.SOHD AS VARCHAR(20)) LIKE ? OR d.MASP LIKE ? OR p.TENSP LIKE ?
            ORDER BY d.SOHD DESC, d.MASP
            """;
        String filter = "%" + search.getText().trim() + "%";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, filter); ps.setString(2, filter); ps.setString(3, filter);
            try (ResultSet rs = ps.executeQuery()) {
                model.setRowCount(0);
                while (rs.next()) {
                    BigDecimal gia = rs.getBigDecimal("GIA");
                    BigDecimal total = rs.getBigDecimal("THANHTIEN");
                    model.addRow(new Object[]{rs.getInt("SOHD"), rs.getString("MASP").trim(),
                            rs.getString("TENSP"), rs.getInt("SL"),
                            gia.stripTrailingZeros().toPlainString(),
                            total.stripTrailingZeros().toPlainString()});
                }
                status.setText("Tìm thấy " + model.getRowCount() + " chi tiết hóa đơn.");
            }
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void selectRow() {
        int index = table.getSelectedRow();
        if (index < 0) return;
        int row = table.convertRowIndexToModel(index);
        selectedInvoice = Integer.parseInt(model.getValueAt(row, 0).toString());
        selectedProduct = model.getValueAt(row, 1).toString().trim();
        Option.select(sohd, selectedInvoice.toString());
        Option.select(masp, selectedProduct);
        sl.setText(Objects.toString(model.getValueAt(row, 3), ""));
        sohd.setEnabled(false); masp.setEnabled(false);
    }
    private void clear() {
        selectedInvoice = null; selectedProduct = null;
        table.clearSelection(); sohd.setEnabled(true); masp.setEnabled(true);
        sl.setText("");
    }
    private int invoiceId() { return Ui.positiveInt(Option.value(sohd), "Số hóa đơn"); }
    private String productCode() { return Ui.required(Option.value(masp), "Sản phẩm"); }
    private int qty() { return Ui.positiveInt(sl.getText(), "Số lượng"); }
    private void insert() {
        try {
            int invoice = invoiceId(); String product = productCode(); int quantity = qty();
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO dbo.CTHD(SOHD,MASP,SL) VALUES (?,?,?)")) {
                    ps.setInt(1, invoice); ps.setString(2, product); ps.setInt(3, quantity);
                    ps.executeUpdate();
                }
                DataService.synchronizeTotals(c);
            });
            clear(); load(); Ui.success(this, "Đã thêm sản phẩm vào hóa đơn. Trị giá đã được tính lại.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void update() {
        if (selectedInvoice == null) { Ui.error(this, new IllegalArgumentException("Chọn chi tiết hóa đơn để sửa.")); return; }
        try {
            int quantity = qty(); int invoice = selectedInvoice; String product = selectedProduct;
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("UPDATE dbo.CTHD SET SL=? WHERE SOHD=? AND MASP=?")) {
                    ps.setInt(1, quantity); ps.setInt(2, invoice); ps.setString(3, product);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tìm thấy chi tiết cần sửa.");
                }
                DataService.synchronizeTotals(c);
            });
            clear(); load(); Ui.success(this, "Đã sửa số lượng và cập nhật trị giá hóa đơn.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void delete() {
        if (selectedInvoice == null) { Ui.error(this, new IllegalArgumentException("Chọn chi tiết hóa đơn để xóa.")); return; }
        if (JOptionPane.showConfirmDialog(this, "Xóa sản phẩm " + selectedProduct + " khỏi hóa đơn " + selectedInvoice + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            int invoice = selectedInvoice; String product = selectedProduct;
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM dbo.CTHD WHERE SOHD=? AND MASP=?")) {
                    ps.setInt(1, invoice); ps.setString(2, product);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tìm thấy chi tiết cần xóa.");
                }
                DataService.synchronizeTotals(c);
            });
            clear(); load(); Ui.success(this, "Đã xóa chi tiết và tính lại trị giá hóa đơn.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
}
