package qlbh;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public final class HoaDonFrame extends JFrame {
    private final JTextField sohd = Ui.field();
    private final JTextField nghd = Ui.field();
    private final JTextField search = Ui.field();
    private final JComboBox<Option> makh = new JComboBox<>();
    private final JComboBox<Option> manv = new JComboBox<>();
    private final JTextField trigia = Ui.field();
    private final DefaultTableModel model = Ui.model("Số HĐ", "Ngày HĐ", "Mã KH", "Tên khách hàng", "Mã NV", "Tên nhân viên", "Trị giá (VNĐ)");
    private final JTable table = Ui.table(model);
    private final JLabel status = Ui.label("Sẵn sàng");
    private Integer selectedId;

    public HoaDonFrame() {
        Ui.frame(this, "Hóa đơn");
        setLayout(new BorderLayout(0, 2));
        add(Ui.header("QUẢN LÝ HÓA ĐƠN", "Tạo, sửa, xóa và tra cứu hóa đơn • Trị giá tự tính từ chi tiết"), BorderLayout.NORTH);
        JPanel left = Ui.formPanel();
        left.add(Ui.formRow("Số hóa đơn *", sohd)); left.add(Box.createVerticalStrut(9));
        nghd.setText(LocalDate.now().toString());
        left.add(Ui.formRow("Ngày hóa đơn * (yyyy-MM-dd)", nghd)); left.add(Box.createVerticalStrut(9));
        left.add(Ui.formRow("Khách hàng (có thể chọn Khách lẻ)", makh)); left.add(Box.createVerticalStrut(9));
        left.add(Ui.formRow("Nhân viên lập hóa đơn *", manv)); left.add(Box.createVerticalStrut(9));
        trigia.setEditable(false);
        trigia.setBackground(new Color(240, 244, 248));
        trigia.setText("0");
        left.add(Ui.formRow("Trị giá tự động (VNĐ)", trigia));
        left.add(Box.createVerticalStrut(14));
        JLabel info = Ui.label("Để thêm mặt hàng, mở form Chi tiết hóa đơn.");
        info.setForeground(Ui.BLUE);
        left.add(info);
        JScrollPane leftScroll = new JScrollPane(left);
        leftScroll.setPreferredSize(new Dimension(345, 1));
        leftScroll.setBorder(null);
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(leftScroll, BorderLayout.WEST);

        JPanel right = Ui.panel(new BorderLayout(0, 11));
        right.setBorder(new EmptyBorder(0, 10, 12, 16));
        JPanel searchBar = Ui.panel(new BorderLayout(8, 0));
        searchBar.add(search, BorderLayout.CENTER);
        JPanel searchButtons = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton searchButton = Ui.button("Tìm kiếm");
        JButton refresh = Ui.button("Tải lại");
        searchButtons.add(searchButton); searchButtons.add(refresh);
        searchBar.add(searchButtons, BorderLayout.EAST);
        right.add(searchBar, BorderLayout.NORTH);
        right.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = Ui.panel(new BorderLayout());
        JPanel buttons = Ui.panel(new FlowLayout(FlowLayout.LEFT, 7, 5));
        JButton add = Ui.button("Thêm");
        JButton edit = Ui.button("Sửa");
        JButton delete = Ui.button("Xóa");
        JButton clear = Ui.button("Làm mới form");
        JButton details = Ui.button("Xem chi tiết HĐ");
        buttons.add(add); buttons.add(edit); buttons.add(delete); buttons.add(clear); buttons.add(details);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.SOUTH);
        right.add(bottom, BorderLayout.SOUTH);
        add(right, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selectRow(); });
        searchButton.addActionListener(e -> load());
        search.addActionListener(e -> load());
        refresh.addActionListener(e -> { search.setText(""); loadOptions(); load(); });
        add.addActionListener(e -> insert());
        edit.addActionListener(e -> update());
        delete.addActionListener(e -> delete());
        clear.addActionListener(e -> { loadOptions(); clear(); });
        details.addActionListener(e -> {
            if (selectedId == null) { Ui.error(this, new IllegalArgumentException("Hãy chọn một hóa đơn trên bảng.")); return; }
            new ChiTietHoaDonFrame(selectedId).setVisible(true);
        });
        loadOptions();
        load();
    }

    private void loadOptions() {
        String oldKh = Option.value(makh);
        String oldNv = Option.value(manv);
        try (Connection c = Database.connect(); Statement s = c.createStatement()) {
            makh.removeAllItems();
            makh.addItem(new Option("", "-- Khách lẻ (không mã khách) --"));
            try (ResultSet rs = s.executeQuery("SELECT MAKH,HOTEN FROM dbo.KHACHHANG ORDER BY MAKH")) {
                while (rs.next()) makh.addItem(new Option(rs.getString(1).trim(), rs.getString(1).trim() + " - " + rs.getString(2)));
            }
            manv.removeAllItems();
            try (ResultSet rs = s.executeQuery("SELECT MANV,HOTEN FROM dbo.NHANVIEN ORDER BY MANV")) {
                while (rs.next()) manv.addItem(new Option(rs.getString(1).trim(), rs.getString(1).trim() + " - " + rs.getString(2)));
            }
            Option.select(makh, oldKh);
            Option.select(manv, oldNv);
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void load() {
        selectedId = null;
        table.clearSelection();
        sohd.setEditable(true);
        String filter = "%" + search.getText().trim() + "%";
        String sql = """
                SELECT h.SOHD, h.NGHD, h.MAKH, k.HOTEN AS TENKH,
                       h.MANV, n.HOTEN AS TENNV, h.TRIGIA
                FROM dbo.HOADON h
                LEFT JOIN dbo.KHACHHANG k ON h.MAKH=k.MAKH
                JOIN dbo.NHANVIEN n ON h.MANV=n.MANV
                WHERE CAST(h.SOHD AS VARCHAR(20)) LIKE ? OR k.HOTEN LIKE ? OR n.HOTEN LIKE ?
                ORDER BY h.SOHD DESC
                """;
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, filter); ps.setString(2, filter); ps.setString(3, filter);
            try (ResultSet rs = ps.executeQuery()) {
                model.setRowCount(0);
                while (rs.next()) {
                    Timestamp date = rs.getTimestamp("NGHD");
                    BigDecimal money = rs.getBigDecimal("TRIGIA");
                    model.addRow(new Object[]{rs.getInt("SOHD"),
                            date.toLocalDateTime().toLocalDate().toString(),
                            rs.getString("MAKH") == null ? "" : rs.getString("MAKH").trim(),
                            Objects.toString(rs.getString("TENKH"), "Khách lẻ"),
                            rs.getString("MANV").trim(),
                            rs.getString("TENNV"),
                            money.stripTrailingZeros().toPlainString()});
                }
                status.setText("Tìm thấy " + model.getRowCount() + " hóa đơn.");
            }
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void selectRow() {
        int index = table.getSelectedRow();
        if (index < 0) return;
        int row = table.convertRowIndexToModel(index);
        selectedId = Integer.parseInt(model.getValueAt(row, 0).toString());
        sohd.setText(selectedId.toString());
        sohd.setEditable(false);
        nghd.setText(model.getValueAt(row, 1).toString());
        Option.select(makh, model.getValueAt(row, 2).toString());
        Option.select(manv, model.getValueAt(row, 4).toString());
        trigia.setText(model.getValueAt(row, 6).toString());
    }
    private void clear() {
        selectedId = null;
        table.clearSelection();
        sohd.setText(""); sohd.setEditable(true);
        nghd.setText(LocalDate.now().toString());
        Option.select(makh, "");
        if (manv.getItemCount() > 0) manv.setSelectedIndex(0);
        trigia.setText("0");
    }
    private int inputId() { return Ui.positiveInt(sohd.getText(), "Số hóa đơn"); }
    private Timestamp inputDate() {
        String text = Ui.date(nghd.getText(), true, "Ngày hóa đơn");
        LocalDate date = LocalDate.parse(text);
        if (date.isBefore(LocalDate.of(1900, 1, 1)) || date.isAfter(LocalDate.of(2079, 6, 6)))
            throw new IllegalArgumentException("Ngày hóa đơn phải từ 1900-01-01 đến 2079-06-06 (SMALLDATETIME).");
        return Timestamp.valueOf(date.atStartOfDay());
    }
    private String inputEmployee() {
        return Ui.required(Option.value(manv), "Nhân viên");
    }
    private void insert() {
        try {
            int id = inputId(); Timestamp date = inputDate();
            String customer = Option.value(makh); String employee = inputEmployee();
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO dbo.HOADON(SOHD,NGHD,MAKH,MANV) VALUES (?,?,?,?)")) {
                    ps.setInt(1, id); ps.setTimestamp(2, date);
                    if (customer.isEmpty()) ps.setNull(3, Types.CHAR); else ps.setString(3, customer);
                    ps.setString(4, employee); ps.executeUpdate();
                }
                DataService.synchronizeTotals(c);
            });
            clear(); load(); Ui.success(this, "Đã tạo hóa đơn " + id + ". Tiếp theo hãy thêm chi tiết hóa đơn.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void update() {
        if (selectedId == null) { Ui.error(this, new IllegalArgumentException("Chọn hóa đơn để sửa.")); return; }
        try {
            Timestamp date = inputDate(); String customer = Option.value(makh);
            String employee = inputEmployee(); int id = selectedId;
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("UPDATE dbo.HOADON SET NGHD=?,MAKH=?,MANV=? WHERE SOHD=?")) {
                    ps.setTimestamp(1, date);
                    if (customer.isEmpty()) ps.setNull(2, Types.CHAR); else ps.setString(2, customer);
                    ps.setString(3, employee); ps.setInt(4, id);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tìm thấy hóa đơn cần sửa.");
                }
                DataService.synchronizeTotals(c);
            });
            clear(); load(); Ui.success(this, "Đã cập nhật hóa đơn.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void delete() {
        if (selectedId == null) { Ui.error(this, new IllegalArgumentException("Chọn hóa đơn để xóa.")); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa hóa đơn " + selectedId + " và TẤT CẢ chi tiết của hóa đơn này?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            int id = selectedId;
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM dbo.CTHD WHERE SOHD=?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM dbo.HOADON WHERE SOHD=?")) {
                    ps.setInt(1, id);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tìm thấy hóa đơn cần xóa.");
                }
                DataService.synchronizeTotals(c);
            });
            clear(); load(); Ui.success(this, "Đã xóa hóa đơn và các chi tiết liên quan.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
}
