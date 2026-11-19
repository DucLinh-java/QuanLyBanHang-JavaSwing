package qlbh;

import javax.swing.JComboBox;

public record Option(String code, String text) {
    @Override public String toString() { return text; }
    public static String value(JComboBox<Option> combo) {
        Option o = (Option) combo.getSelectedItem();
        return o == null ? "" : o.code();
    }
    public static void select(JComboBox<Option> combo, String code) {
        String target = code == null ? "" : code.trim();
        for (int i = 0; i < combo.getItemCount(); i++) {
            Option o = combo.getItemAt(i);
            if (target.equals(o.code())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
    }
}
