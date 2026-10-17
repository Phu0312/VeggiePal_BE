package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IngredientUnitConverterTest {

    @Test
    void convertsMassUnitsAccurately() {
        Optional<BigDecimal> grams = IngredientUnitConverter.toGrams(new BigDecimal("250"), "GRAM", "Đậu hũ", "dau hu");
        assertTrue(grams.isPresent());
        assertEquals(new BigDecimal("250.00"), grams.get());

        Optional<BigDecimal> kg = IngredientUnitConverter.toGrams(new BigDecimal("1.5"), "KG", "Gạo lứt", "gao lut");
        assertTrue(kg.isPresent());
        assertEquals(new BigDecimal("1500.00"), kg.get());
    }

    @Test
    void convertsLiquidUnitsWithGroundedDensity() {
        // Oil density ~0.92
        Optional<BigDecimal> oil = IngredientUnitConverter.toGrams(new BigDecimal("100"), "ml", "Dầu ô liu", "dau oliu");
        assertTrue(oil.isPresent());
        assertEquals(new BigDecimal("92.00"), oil.get());

        // Soy sauce density ~1.15
        Optional<BigDecimal> soySauce = IngredientUnitConverter.toGrams(new BigDecimal("20"), "ml", "Nước tương", "nuoc tuong");
        assertTrue(soySauce.isPresent());
        assertEquals(new BigDecimal("23.00"), soySauce.get());

        // Water/broth density 1.00
        Optional<BigDecimal> water = IngredientUnitConverter.toGrams(new BigDecimal("500"), "ml", "Nước dùng rau củ", "nuoc dung rau cu");
        assertTrue(water.isPresent());
        assertEquals(new BigDecimal("500.00"), water.get());

        // 1 Liter of water
        Optional<BigDecimal> liter = IngredientUnitConverter.toGrams(new BigDecimal("1"), "l", "Nước lọc", "nuoc loc");
        assertTrue(liter.isPresent());
        assertEquals(new BigDecimal("1000.00"), liter.get());

        // Solid ingredient with ML should be rejected as invalid
        Optional<BigDecimal> invalidSolid = IngredientUnitConverter.toGrams(new BigDecimal("100"), "ml", "Khoai tây", "khoai tay");
        assertTrue(invalidSolid.isEmpty());
    }

    @Test
    void convertsSpoonUnitsAccurately() {
        // 1 tbsp oil = 13.8g
        Optional<BigDecimal> tbspOil = IngredientUnitConverter.toGrams(new BigDecimal("2"), "muong canh", "Dầu mè", "dau me");
        assertTrue(tbspOil.isPresent());
        assertEquals(new BigDecimal("27.60"), tbspOil.get());

        // 1 tbsp salt = 18g
        Optional<BigDecimal> tbspSalt = IngredientUnitConverter.toGrams(new BigDecimal("1"), "thìa canh", "Muối tinh", "muoi tinh");
        assertTrue(tbspSalt.isPresent());
        assertEquals(new BigDecimal("18.00"), tbspSalt.get());

        // 1 tsp sugar = 4.2g
        Optional<BigDecimal> tspSugar = IngredientUnitConverter.toGrams(new BigDecimal("2"), "tsp", "Đường mía", "duong mia");
        assertTrue(tspSugar.isPresent());
        assertEquals(new BigDecimal("8.40"), tspSugar.get());

        // 1 tsp pepper = 2.5g
        Optional<BigDecimal> tspPepper = IngredientUnitConverter.toGrams(new BigDecimal("1"), "muỗng cà phê", "Tiêu đen", "tieu den");
        assertTrue(tspPepper.isPresent());
        assertEquals(new BigDecimal("2.50"), tspPepper.get());
    }

    @Test
    void convertsTheRecipeFormPieceUnit() {
        // The FE recipe form offers GRAM, KG, ML, TBSP, TSP and PIECE.
        Optional<BigDecimal> tomatoes = IngredientUnitConverter.toGrams(new BigDecimal("2"), "PIECE", "Cà chua", "ca chua");
        assertEquals(Optional.of(new BigDecimal("240.00")), tomatoes);
        assertEquals(Optional.of(new BigDecimal("150.00")),
                IngredientUnitConverter.toGrams(BigDecimal.ONE, "pcs", "Đậu hũ", "dau hu"));
    }

    @Test
    void convertsPieceUnitsForGroundedIngredients() {
        // 2 tomatoes (120g each) = 240g
        Optional<BigDecimal> tomatoes = IngredientUnitConverter.toGrams(new BigDecimal("2"), "quả", "Cà chua", "ca chua");
        assertTrue(tomatoes.isPresent());
        assertEquals(new BigDecimal("240.00"), tomatoes.get());

        // 1 carrot (100g)
        Optional<BigDecimal> carrot = IngredientUnitConverter.toGrams(new BigDecimal("1"), "củ", "Cà rốt", "ca rot");
        assertTrue(carrot.isPresent());
        assertEquals(new BigDecimal("100.00"), carrot.get());

        // Garlic clove (4g each) vs garlic bulb (35g each)
        Optional<BigDecimal> garlicClove = IngredientUnitConverter.toGrams(new BigDecimal("3"), "tép", "Tỏi", "toi");
        assertTrue(garlicClove.isPresent());
        assertEquals(new BigDecimal("12.00"), garlicClove.get());

        Optional<BigDecimal> garlicBulb = IngredientUnitConverter.toGrams(new BigDecimal("1"), "củ", "Tỏi", "toi");
        assertTrue(garlicBulb.isPresent());
        assertEquals(new BigDecimal("35.00"), garlicBulb.get());

        // Tofu piece (150g)
        Optional<BigDecimal> tofuPiece = IngredientUnitConverter.toGrams(new BigDecimal("2"), "miếng", "Đậu hũ", "dau hu");
        assertTrue(tofuPiece.isPresent());
        assertEquals(new BigDecimal("300.00"), tofuPiece.get());
    }

    @Test
    void rejectsUngroundedPieceUnitsAndInvalidInputs() {
        // Unknown fruit/piece should NOT guess a generic weight
        Optional<BigDecimal> unknownPiece = IngredientUnitConverter.toGrams(new BigDecimal("1"), "quả", "Trái thần bí", "trai than bi");
        assertTrue(unknownPiece.isEmpty());

        // Zero or negative quantity
        assertTrue(IngredientUnitConverter.toGrams(BigDecimal.ZERO, "g", "Đậu hũ", "dau hu").isEmpty());
        assertTrue(IngredientUnitConverter.toGrams(new BigDecimal("-5"), "g", "Đậu hũ", "dau hu").isEmpty());

        // Blank unit
        assertTrue(IngredientUnitConverter.toGrams(new BigDecimal("100"), "", "Đậu hũ", "dau hu").isEmpty());
        assertTrue(IngredientUnitConverter.toGrams(new BigDecimal("100"), "   ", "Đậu hũ", "dau hu").isEmpty());
    }
}
