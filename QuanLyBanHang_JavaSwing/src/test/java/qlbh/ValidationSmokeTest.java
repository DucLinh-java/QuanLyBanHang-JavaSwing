package qlbh;

import java.math.BigDecimal;

/** Chạy trực tiếp (không yêu cầu SQL Server) để kiểm tra dữ liệu nhập. */
public final class ValidationSmokeTest {
    public static void main(String[] args) {
        check("2026-11-20".equals(Ui.date("2026-11-20", true, "Ngày")), "Ngày hợp lệ");
        check(Ui.money("650000", "Giá").compareTo(new BigDecimal("650000")) == 0, "Tiền hợp lệ");
        check(Ui.positiveInt("2", "Số lượng") == 2, "Số lượng hợp lệ");
        expectError(() -> Ui.date("2026-02-30", true, "Ngày"));
        expectError(() -> Ui.money("-100", "Giá"));
        expectError(() -> Ui.positiveInt("0", "Số lượng"));
        expectError(() -> Ui.required(" ", "Mã"));
        System.out.println("PASS: 7 kịch bản kiểm tra dữ liệu nhập.");
    }
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
    private static void expectError(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Giá trị không hợp lệ phải bị từ chối.");
    }
}
