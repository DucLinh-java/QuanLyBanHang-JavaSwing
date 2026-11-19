package qlbh;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private Database() {}

    public static Connection connect() throws SQLException {
        Properties p = new Properties();
        Path path = Path.of("config.properties");
        try (InputStream in = Files.exists(path)
                ? Files.newInputStream(path)
                : Database.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) throw new IOException("Không tìm thấy config.properties trong thư mục chạy ứng dụng.");
            p.load(in);
        } catch (IOException ex) {
            throw new SQLException("Không đọc được cấu hình: " + ex.getMessage(), ex);
        }

        String host = p.getProperty("db.host", "localhost").trim();
        String port = p.getProperty("db.port", "1433").trim();
        String name = p.getProperty("db.name", "QuanLyBanHang").trim();
        String user = p.getProperty("db.username", "sa").trim();
        String password = p.getProperty("db.password", "");
        String encrypt = p.getProperty("db.encrypt", "true").trim();
        String trust = p.getProperty("db.trustServerCertificate", "true").trim();
        if (!port.matches("\\d{1,5}") || Integer.parseInt(port) > 65535)
            throw new SQLException("db.port phải là cổng TCP hợp lệ (1–65535).");
        if (host.isBlank() || name.isBlank() || user.isBlank())
            throw new SQLException("Thiếu db.host, db.name hoặc db.username trong config.properties.");
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Thiếu Microsoft JDBC Driver. Hãy mở dự án bằng Maven để tải thư viện.", ex);
        }
        String url = "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + name
                + ";encrypt=" + encrypt
                + ";trustServerCertificate=" + trust
                + ";loginTimeout=8;";
        return DriverManager.getConnection(url, user, password);
    }

    @FunctionalInterface
    public interface TxAction {
        void run(Connection connection) throws SQLException;
    }

    public static void inTransaction(TxAction action) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                action.run(c);
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }
}
