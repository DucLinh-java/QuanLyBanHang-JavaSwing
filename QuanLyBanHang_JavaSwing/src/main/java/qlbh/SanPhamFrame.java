package qlbh;

public final class SanPhamFrame extends SimpleCrudFrame {
    public SanPhamFrame() {
        super("Sản phẩm", "Quản lý sản phẩm, nước sản xuất, đơn vị tính và giá bán",
                "SANPHAM", "MASP", "TENSP",
                col("MASP", "Mã sản phẩm", Type.TEXT, 4, true),
                col("TENSP", "Tên sản phẩm", Type.TEXT, 40, true),
                col("DVT", "Đơn vị tính", Type.TEXT, 20, false),
                col("NUOCSX", "Nước sản xuất", Type.TEXT, 40, false),
                col("GIA", "Đơn giá", Type.MONEY, 20, true));
    }
}
