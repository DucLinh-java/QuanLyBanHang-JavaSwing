package qlbh;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Chỉ đồng bộ trong giao dịch sau khi sửa sản phẩm, hóa đơn hoặc chi tiết. */
public final class DataService {
    private DataService() {}

    public static void synchronizeTotals(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("""
                UPDATE h SET TRIGIA = ISNULL((
                    SELECT SUM(CAST(d.SL AS DECIMAL(19,2)) * CAST(p.GIA AS DECIMAL(19,2)))
                    FROM dbo.CTHD d JOIN dbo.SANPHAM p ON d.MASP=p.MASP
                    WHERE d.SOHD=h.SOHD
                ),0) FROM dbo.HOADON h
                """);
            s.executeUpdate("""
                UPDATE k SET DOANHSO = ISNULL((
                    SELECT SUM(h.TRIGIA) FROM dbo.HOADON h WHERE h.MAKH=k.MAKH
                ),0) FROM dbo.KHACHHANG k
                """);
        }
    }
}
