package com.veggiepal.ai.mealplan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class IngredientUnitConverter {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal DENSITY_WATER = new BigDecimal("1.00");
    private static final BigDecimal DENSITY_OIL = new BigDecimal("0.92");
    private static final BigDecimal DENSITY_SOY_SAUCE = new BigDecimal("1.15");
    private static final BigDecimal DENSITY_PLANT_MILK = new BigDecimal("1.03");
    private static final BigDecimal DENSITY_SYRUP = new BigDecimal("1.33");

    // Standard piece weights in grams for well-known ingredients (edible portion).
    // Ordered with longer phrases first to ensure precise multi-word matching.
    private static final java.util.List<Map.Entry<String, BigDecimal>> PIECE_WEIGHTS = java.util.List.of(
            Map.entry("la rong bien", new BigDecimal("2.5")),
            Map.entry("rong bien", new BigDecimal("2.5")),
            Map.entry("nam dui ga", new BigDecimal("80")),
            Map.entry("banh mi", new BigDecimal("80")),
            Map.entry("khoai tay", new BigDecimal("150")),
            Map.entry("khoai lang", new BigDecimal("130")),
            Map.entry("ca chua", new BigDecimal("120")),
            Map.entry("ca rot", new BigDecimal("100")),
            Map.entry("hanh tay", new BigDecimal("110")),
            Map.entry("dua chuot", new BigDecimal("150")),
            Map.entry("dua leo", new BigDecimal("150")),
            Map.entry("dau phu", new BigDecimal("150")),
            Map.entry("dau hu", new BigDecimal("150")),
            Map.entry("chuoi", new BigDecimal("118")),
            Map.entry("chanh", new BigDecimal("50")),
            Map.entry("bap", new BigDecimal("150")),
            Map.entry("ngo", new BigDecimal("150")),
            Map.entry("tao", new BigDecimal("150")),
            Map.entry("bo", new BigDecimal("150")),
            Map.entry("ot", new BigDecimal("5"))
    );

    private IngredientUnitConverter() {}

    /**
     * Converts an ingredient quantity and unit into grams.
     * Returns Optional.empty() if conversion cannot be reliably grounded.
     */
    public static Optional<BigDecimal> toGrams(BigDecimal quantity, String rawUnit, String ingredientName, String normalizedName) {
        if (quantity == null || quantity.signum() <= 0 || rawUnit == null || rawUnit.isBlank()) {
            return Optional.empty();
        }

        String unit = normalizeText(rawUnit);
        String name = normalizeText(normalizedName != null && !normalizedName.isBlank() ? normalizedName : ingredientName);

        // 1. Grams / Kilograms
        switch (unit) {
            case "g", "gram", "grams", "gr", "gam" -> {
                return Optional.of(quantity.setScale(2, RoundingMode.HALF_UP));
            }
            case "kg", "kilogram", "kilograms", "kilo" -> {
                return Optional.of(quantity.multiply(THOUSAND).setScale(2, RoundingMode.HALF_UP));
            }
            case "mg", "milligram", "milligrams" -> {
                return Optional.of(quantity.divide(THOUSAND, 4, RoundingMode.HALF_UP));
            }
            default -> {}
        }

        // 2. Milliliters / Liters
        if (unit.equals("ml") || unit.equals("milliliter") || unit.equals("milliliters") || unit.equals("mililit")) {
            BigDecimal density = liquidDensity(name);
            if (density == null) return Optional.empty();
            return Optional.of(quantity.multiply(density).setScale(2, RoundingMode.HALF_UP));
        }
        if (unit.equals("l") || unit.equals("liter") || unit.equals("liters") || unit.equals("lit") || unit.equals("lít")) {
            BigDecimal density = liquidDensity(name);
            if (density == null) return Optional.empty();
            return Optional.of(quantity.multiply(THOUSAND).multiply(density).setScale(2, RoundingMode.HALF_UP));
        }

        // 3. Tablespoon (muỗng canh, thìa canh, tbsp) - 15 ml
        if (unit.equals("muong canh") || unit.equals("thia canh") || unit.equals("tbsp") || unit.equals("tablespoon")) {
            BigDecimal gramsPerTbsp = tablespoonGrams(name);
            return Optional.of(quantity.multiply(gramsPerTbsp).setScale(2, RoundingMode.HALF_UP));
        }

        // 4. Teaspoon (muỗng cà phê, thìa cà phê, tsp) - 5 ml
        if (unit.equals("muong ca phe") || unit.equals("thia ca phe") || unit.equals("tsp") || unit.equals("teaspoon")) {
            BigDecimal gramsPerTsp = teaspoonGrams(name);
            return Optional.of(quantity.multiply(gramsPerTsp).setScale(2, RoundingMode.HALF_UP));
        }

        // 5. Piece / count units: quả, trái, củ, bắp, tép, cây, miếng, bìa, lá
        if (isPieceUnit(unit)) {
            // Special handling for garlic: clove (tép) vs bulb (củ)
            if (matchesWord(name, "toi")) {
                if (unit.equals("tep")) {
                    return Optional.of(quantity.multiply(new BigDecimal("4")).setScale(2, RoundingMode.HALF_UP));
                }
                if (unit.equals("cu")) {
                    return Optional.of(quantity.multiply(new BigDecimal("35")).setScale(2, RoundingMode.HALF_UP));
                }
            }
            for (Map.Entry<String, BigDecimal> entry : PIECE_WEIGHTS) {
                if (matchesWord(name, entry.getKey())) {
                    return Optional.of(quantity.multiply(entry.getValue()).setScale(2, RoundingMode.HALF_UP));
                }
            }
            // If the ingredient is not in the trusted piece table, do not guess
            return Optional.empty();
        }

        // Unsupported or unrecognized unit
        return Optional.empty();
    }

    private static boolean isPieceUnit(String unit) {
        return unit.equals("qua") || unit.equals("trai") || unit.equals("cu")
                || unit.equals("bap") || unit.equals("tep") || unit.equals("cay")
                || unit.equals("mieng") || unit.equals("bia") || unit.equals("la")
                // PIECE is what the FE recipe form sends.
                || unit.equals("piece") || unit.equals("pieces") || unit.equals("pcs");
    }

    private static BigDecimal liquidDensity(String name) {
        if (name.contains("dau") && (name.contains("an") || name.contains("oliu") || name.contains("me") || name.contains("dua"))) {
            return DENSITY_OIL;
        }
        if (name.contains("nuoc tuong") || name.contains("xi dau") || name.contains("maggi") || name.contains("tam thai tu")) {
            return DENSITY_SOY_SAUCE;
        }
        if (name.contains("sua dau nanh") || name.contains("sua hat") || name.contains("sua yen mach")) {
            return DENSITY_PLANT_MILK;
        }
        if (name.contains("mat ong") || name.contains("siro") || name.contains("duong den")) {
            return DENSITY_SYRUP;
        }
        // General edible liquid check
        if (name.contains("nuoc") || name.contains("canh") || name.contains("giam") || name.contains("sot")
                || name.contains("dau") || name.contains("sua") || name.contains("tra")) {
            return DENSITY_WATER;
        }
        // Solid ingredients with volume units cannot be converted
        return null;
    }

    private static BigDecimal tablespoonGrams(String name) {
        if (name.contains("dau") && (name.contains("an") || name.contains("oliu") || name.contains("me") || name.contains("dua"))) {
            return new BigDecimal("13.8");
        }
        if (name.contains("nuoc tuong") || name.contains("xi dau")) {
            return new BigDecimal("17.25");
        }
        if (name.contains("muoi")) {
            return new BigDecimal("18.0");
        }
        if (name.contains("duong")) {
            return new BigDecimal("12.5");
        }
        if (name.contains("bot") || name.contains("tinh bot")) {
            return new BigDecimal("8.5");
        }
        if (name.contains("bo dau phong") || name.contains("bo lac")) {
            return new BigDecimal("16.0");
        }
        return new BigDecimal("15.0");
    }

    private static BigDecimal teaspoonGrams(String name) {
        if (name.contains("dau") && (name.contains("an") || name.contains("oliu") || name.contains("me") || name.contains("dua"))) {
            return new BigDecimal("4.6");
        }
        if (name.contains("nuoc tuong") || name.contains("xi dau")) {
            return new BigDecimal("5.75");
        }
        if (name.contains("muoi")) {
            return new BigDecimal("6.0");
        }
        if (name.contains("duong")) {
            return new BigDecimal("4.2");
        }
        if (name.contains("tieu") || name.contains("ot bot") || name.contains("bot que") || name.contains("bot nghe")) {
            return new BigDecimal("2.5");
        }
        return new BigDecimal("5.0");
    }

    private static String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private static boolean matchesWord(String text, String term) {
        if (text == null || term == null) return false;
        String paddedText = " " + text.trim() + " ";
        String paddedTerm = " " + term.trim() + " ";
        return paddedText.contains(paddedTerm);
    }
}
