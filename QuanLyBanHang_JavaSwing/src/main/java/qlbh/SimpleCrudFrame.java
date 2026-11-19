package qlbh;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Khung CRUD dùng chung; các lớp con là 3 cửa sổ quản lý riêng. */
public abstract class SimpleCrudFrame extends JFrame {
    protected enum Type { TEXT, DATE, MONEY, READONLY_MONEY }
    protected record Column(String code, String name, Type type, int maxLength, boolean required) { }
    protected static Column col(String code, String label, Type type, int max, boolean required) {
        return new Column(code, label, type, max, required);
    }

    private final String tableName;
    private final String pkColumn;
    private final String nameColumn;
    private final List<Column> columns;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField search = Ui.field();
    private final JLabel status = Ui.label("Sẵn sàng");
    private String selectedKey;

    protected SimpleCrudFrame(String title, String description,
                              String tableName, String pkColumn, String nameColumn, Column... columns) {
        this.tableName = tableName;
        this.pkColumn = pkColumn;
        this.nameColumn = nameColumn;
        this.columns = List.of(columns);
        Ui.frame(this, title);
        setLayout(new BorderLayout(0, 3));
        add(Ui.header(title.toUpperCase(Locale.ROOT), description), BorderLayout.NORTH);

        JPanel form = Ui.formPanel();
        for (Column column : columns) {
            JTextField field = Ui.field();
            if (column.type == Type.READONLY_MONEY) {
                field.setEditable(false);
                field.setText("0");
                field.setBackground(new Color(240, 244, 248));
            }
            fields.put(column.code, field);
            form.add(Ui.formRow(column.name + (column.required ? " *" : ""), field));
            form.add(Box.createVerticalStrut(8));
        }
        JLabel note = Ui.label("Ngày: yyyy-MM-dd   |   Tiền: nhập số");
        note.setForeground(new Color(105, 116, 128));
        form.add(Box.createVerticalStrut(7));
        form.add(note);
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setPreferredSize(new Dimension(345, 1));
        formScroll.setBorder(null);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(formScroll, BorderLayout.WEST);

        String[] headers = this.columns.stream().map(Column::name).toArray(String[]::new);
        model = Ui.model(headers);
        table = Ui.table(model);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selectedRow();
        });

        JPanel right = Ui.panel(new BorderLayout(0, 11));
        right.setBorder(new EmptyBorder(0, 10, 12, 16));
        JPanel searchBar = Ui.panel(new BorderLayout(8, 0));
        searchBar.add(search, BorderLayout.CENTER);
        JButton searchButton = Ui.button("Tìm kiếm");
        JButton all = Ui.button("Tải lại");
        JPanel searchButtons = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchButtons.add(searchButton);
        searchButtons.add(all);
        searchBar.add(searchButtons, BorderLayout.EAST);
        right.add(searchBar, BorderLayout.NORTH);
        right.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel footer = Ui.panel(new BorderLayout());
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.LEFT, 7, 6));
        JButton add = Ui.button("Thêm");
        JButton edit = Ui.button("Sửa");
        JButton delete = Ui.button("Xóa");
        JButton clear = Ui.button("Làm mới form");
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(clear);
        footer.add(actions, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        right.add(footer, BorderLayout.SOUTH);
        add(right, BorderLayout.CENTER);

        searchButton.addActionListener(e -> load());
        search.addActionListener(e -> load());
        all.addActionListener(e -> { search.setText(""); load(); });
        add.addActionListener(e -> insert());
        edit.addActionListener(e -> update());
        delete.addActionListener(e -> delete());
        clear.addActionListener(e -> reset());
        load();
    }

    private void load() {
        selectedKey = null;
        table.clearSelection();
        fields.get(pkColumn).setEditable(true);
        String sql = "SELECT " + String.join(",", columns.stream().map(Column::code).toList())
                + " FROM dbo." + tableName + " WHERE " + pkColumn + " LIKE ? OR " + nameColumn
                + " LIKE ? ORDER BY " + pkColumn;
        String filter = "%" + search.getText().trim() + "%";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, filter);
            ps.setString(2, filter);
            try (ResultSet rs = ps.executeQuery()) {
                model.setRowCount(0);
                while (rs.next()) {
                    Object[] row = new Object[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        Column col = columns.get(i);
                        if (col.type == Type.DATE) {
                            Timestamp time = rs.getTimestamp(col.code);
                            row[i] = time == null ? "" : time.toLocalDateTime().toLocalDate().toString();
                        } else if (col.type == Type.MONEY || col.type == Type.READONLY_MONEY) {
                            BigDecimal n = rs.getBigDecimal(col.code);
                            row[i] = n == null ? "" : n.stripTrailingZeros().toPlainString();
                        } else {
                            String text = rs.getString(col.code);
                            row[i] = text == null ? "" : text.trim();
                        }
                    }
                    model.addRow(row);
                }
                status.setText("Tìm thấy " + model.getRowCount() + " bản ghi.");
            }
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void selectedRow() {
        int view = table.getSelectedRow();
        if (view < 0) return;
        int index = table.convertRowIndexToModel(view);
        for (int i = 0; i < columns.size(); i++) {
            String value = Objects.toString(model.getValueAt(index, i), "");
            fields.get(columns.get(i).code).setText(value);
        }
        selectedKey = fields.get(pkColumn).getText().trim();
        fields.get(pkColumn).setEditable(false);
    }

    private void reset() {
        selectedKey = null;
        table.clearSelection();
        for (Column c : columns) {
            JTextField t = fields.get(c.code);
            t.setText(c.type == Type.READONLY_MONEY ? "0" : "");
        }
        fields.get(pkColumn).setEditable(true);
        fields.get(pkColumn).requestFocusInWindow();
    }

    private List<Column> writable() {
        return columns.stream().filter(c -> c.type != Type.READONLY_MONEY).toList();
    }
    private Map<String,Object> validateForm() {
        Map<String,Object> data = new LinkedHashMap<>();
        for (Column c : writable()) {
            String value = fields.get(c.code).getText().trim();
            Object result;
            if (c.type == Type.DATE) {
                String date = Ui.date(value, c.required, c.name);
                result = date.isEmpty() ? null : Timestamp.valueOf(LocalDate.parse(date).atStartOfDay());
            } else if (c.type == Type.MONEY) {
                result = Ui.money(value, c.name);
            } else {
                if (c.required) Ui.required(value, c.name);
                if (value.length() > c.maxLength)
                    throw new IllegalArgumentException(c.name + " tối đa " + c.maxLength + " ký tự.");
                result = value.isEmpty() ? null : value;
            }
            data.put(c.code, result);
        }
        return data;
    }
    private void setValues(PreparedStatement ps, List<String> names, Map<String,Object> data) throws SQLException {
        for (int i = 0; i < names.size(); i++) ps.setObject(i + 1, data.get(names.get(i)));
    }
    private void insert() {
        try {
            Map<String,Object> values = validateForm();
            List<String> names = writable().stream().map(Column::code).toList();
            String sql = "INSERT INTO dbo." + tableName + " (" + String.join(",", names)
                    + ") VALUES (" + String.join(",", Collections.nCopies(names.size(), "?")) + ")";
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    setValues(ps, names, values);
                    ps.executeUpdate();
                }
                if ("SANPHAM".equals(tableName)) DataService.synchronizeTotals(c);
            });
            reset(); load(); Ui.success(this, "Đã thêm bản ghi thành công.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void update() {
        if (selectedKey == null) { Ui.error(this, new IllegalArgumentException("Chọn một dòng trên bảng để sửa.")); return; }
        try {
            Map<String,Object> values = validateForm();
            List<String> names = writable().stream().map(Column::code)
                    .filter(x -> !x.equals(pkColumn)).toList();
            String updates = String.join(",", names.stream().map(x -> x + "=?").toList());
            String sql = "UPDATE dbo." + tableName + " SET " + updates + " WHERE " + pkColumn + "=?";
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    setValues(ps, names, values);
                    ps.setString(names.size() + 1, selectedKey);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tìm thấy bản ghi cần sửa.");
                }
                if ("SANPHAM".equals(tableName)) DataService.synchronizeTotals(c);
            });
            reset(); load(); Ui.success(this, "Đã cập nhật thành công.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
    private void delete() {
        if (selectedKey == null) { Ui.error(this, new IllegalArgumentException("Chọn một dòng trên bảng để xóa.")); return; }
        if (JOptionPane.showConfirmDialog(this,
                "Bạn chắc chắn muốn xóa " + selectedKey + "?", "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM dbo." + tableName + " WHERE " + pkColumn + "=?")) {
                    ps.setString(1, selectedKey);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không tìm thấy bản ghi cần xóa.");
                }
            });
            reset(); load(); Ui.success(this, "Đã xóa bản ghi.");
        } catch (Exception ex) { Ui.error(this, ex); }
    }
}
