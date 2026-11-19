package qlbh;

public final class KhachHangFrame extends SimpleCrudFrame {
    public KhachHangFrame() {
        super("Khách hàng", "Quản lý khách hàng và theo dõi doanh số mua hàng tự động",
                "KHACHHANG", "MAKH", "HOTEN",
                col("MAKH", "Mã khách hàng", Type.TEXT, 4, true),
                col("HOTEN", "Họ tên", Type.TEXT, 40, true),
                col("DCHI", "Địa chỉ", Type.TEXT, 50, false),
                col("SODT", "Số điện thoại", Type.TEXT, 20, false),
                col("NGSINH", "Ngày sinh", Type.DATE, 10, false),
                col("NGDK", "Ngày đăng ký", Type.DATE, 10, false),
                col("DOANHSO", "Doanh số (tự động)", Type.READONLY_MONEY, 20, false));
    }
}
