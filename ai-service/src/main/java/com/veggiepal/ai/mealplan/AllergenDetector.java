package com.veggiepal.ai.mealplan;

import java.text.Normalizer;
import java.util.*;

public final class AllergenDetector {

    // Mapping from Allergen Code to list of keywords in normalized form (without diacritics)
    private static final Map<String, List<String>> ALLERGEN_KEYWORDS = new LinkedHashMap<>();

    static {
        ALLERGEN_KEYWORDS.put("GLUTEN", List.of(
                "gluten", "lua mi", "lua mach", "bot mi", "banh mi", "mi soi", "seitan", "can mi",
                "mi an lien", "hoanh thanh"
        ));
        ALLERGEN_KEYWORDS.put("BUCKWHEAT", List.of(
                "kieu mach", "soba"
        ));
        ALLERGEN_KEYWORDS.put("CORN", List.of(
                "bap", "ngo", "bap ngot", "ngo ngot", "tinh bot bap", "bot bap", "bot ngo"
        ));
        ALLERGEN_KEYWORDS.put("PEANUT", List.of(
                "dau phong", "lac", "bo dau phong", "bo lac", "dau dau phong"
        ));
        ALLERGEN_KEYWORDS.put("SOY", List.of(
                "dau nanh", "dau phu", "dau hu", "chao", "tempeh", "natto", "tuong ban",
                "nuoc tuong", "xi dau", "sua dau nanh", "tau hu ki", "vang dau", "tau hu"
        ));
        ALLERGEN_KEYWORDS.put("MUNG_BEAN", List.of(
                "dau xanh", "gia do", "bot dau xanh"
        ));
        ALLERGEN_KEYWORDS.put("CASHEW", List.of(
                "hat dieu", "sua hat dieu", "bo hat dieu"
        ));
        ALLERGEN_KEYWORDS.put("TREE_NUT", List.of(
                "hanh nhan", "oc cho", "mac ca", "hat de", "hat phi", "ho dao", "hat thong", "hat pecan"
        ));
        ALLERGEN_KEYWORDS.put("SESAME", List.of(
                "me", "vung", "dau me", "dau vung", "tahini", "sot me"
        ));
        ALLERGEN_KEYWORDS.put("COCONUT", List.of(
                "nuoc cot dua", "dau dua", "com dua", "nuoc dua", "dua nao"
        ));
        ALLERGEN_KEYWORDS.put("TOMATO", List.of(
                "ca chua", "sot ca chua", "xot ca chua", "ca chua bi"
        ));
        ALLERGEN_KEYWORDS.put("EGGPLANT", List.of(
                "ca tim", "ca phao"
        ));
        ALLERGEN_KEYWORDS.put("TARO", List.of(
                "khoai mon", "khoai so"
        ));
        ALLERGEN_KEYWORDS.put("CASSAVA", List.of(
                "khoai mi", "san", "bot nang", "bot san day"
        ));
        ALLERGEN_KEYWORDS.put("BAMBOO_SHOOT", List.of(
                "mang", "mang tuoi", "mang kho", "mang chua"
        ));
        ALLERGEN_KEYWORDS.put("CELERY", List.of(
                "can tay"
        ));
        ALLERGEN_KEYWORDS.put("CORIANDER", List.of(
                "rau mui", "ngo ri", "rau ram"
        ));
        ALLERGEN_KEYWORDS.put("GARLIC", List.of(
                "toi", "bot toi", "toi phi"
        ));
        ALLERGEN_KEYWORDS.put("ONION", List.of(
                "hanh tay", "hanh la", "hanh baro", "hanh tim", "hanh phi", "he"
        ));
        ALLERGEN_KEYWORDS.put("MANGO", List.of(
                "xoai"
        ));
        ALLERGEN_KEYWORDS.put("PINEAPPLE", List.of(
                "thom", "khom", "trai dua"
        ));
        ALLERGEN_KEYWORDS.put("PAPAYA", List.of(
                "du du"
        ));
        ALLERGEN_KEYWORDS.put("AVOCADO", List.of(
                "trai bo", "qua bo"
        ));
        ALLERGEN_KEYWORDS.put("DURIAN", List.of(
                "sau rieng"
        ));
        ALLERGEN_KEYWORDS.put("JACKFRUIT", List.of(
                "mit"
        ));
        ALLERGEN_KEYWORDS.put("LYCHEE", List.of(
                "vai"
        ));
        ALLERGEN_KEYWORDS.put("STRAWBERRY", List.of(
                "dau tay"
        ));
        ALLERGEN_KEYWORDS.put("MUSHROOM", List.of(
                "nam", "nam rom", "nam huong", "nam dui ga", "nam bao ngu", "nam kim cham",
                "moc nhi", "nam tai meo", "nam dong co"
        ));
        ALLERGEN_KEYWORDS.put("CHILI", List.of(
                "ot", "ot chuong", "ot bot", "tuong ot", "sa te"
        ));
        ALLERGEN_KEYWORDS.put("MUSTARD", List.of(
                "mu tat", "cai be xanh", "cai be", "hat cai"
        ));
        ALLERGEN_KEYWORDS.put("SULPHITE", List.of(
                "sulfite", "sulphite"
        ));
        ALLERGEN_KEYWORDS.put("MSG", List.of(
                "bot ngot", "mi chinh", "msg"
        ));
    }

    private AllergenDetector() {}

    /**
     * Identifies all allergen codes for a given ingredient name.
     */
    public static Set<String> detectAllergenCodes(String ingredientName, String normalizedName) {
        String text = normalizeText(normalizedName != null && !normalizedName.isBlank() ? normalizedName : ingredientName);
        if (text.isBlank()) return Set.of();

        Set<String> detected = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : ALLERGEN_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (matchesWord(text, keyword)) {
                    detected.add(entry.getKey());
                    break;
                }
            }
        }
        return detected;
    }

    public static boolean matchesWord(String text, String keyword) {
        if (text == null || keyword == null) return false;
        String paddedText = " " + text.trim() + " ";
        String paddedKeyword = " " + keyword.trim() + " ";
        return paddedText.contains(paddedKeyword);
    }

    public static String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
