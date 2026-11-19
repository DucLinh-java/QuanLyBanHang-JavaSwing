package qlbh;

public final class NhanVienFrame extends SimpleCrudFrame {
    public NhanVienFrame() {
        super("Nhân viên", "Quản lý thông tin nhân viên và ngày vào làm",
                "NHANVIEN", "MANV", "HOTEN",
                col("MANV", "Mã nhân viên", Type.TEXT, 4, true),
                col("HOTEN", "Họ tên", Type.TEXT, 40, true),
                col("SODT", "Số điện thoại", Type.TEXT, 20, false),
                col("NGVL", "Ngày vào làm", Type.DATE, 10, false));
    }
}
