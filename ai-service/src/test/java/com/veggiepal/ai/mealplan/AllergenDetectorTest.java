package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AllergenDetectorTest {

    @Test
    void detectsSoyFromTofuAndSoyProducts() {
        Set<String> codes = AllergenDetector.detectAllergenCodes("Đậu hũ", "dau hu");
        assertTrue(codes.contains("SOY"));
        assertFalse(codes.contains("PEANUT"));

        Set<String> soySauce = AllergenDetector.detectAllergenCodes("Nước tương", "nuoc tuong");
        assertTrue(soySauce.contains("SOY"));
    }

    @Test
    void detectsPeanutAccurately() {
        Set<String> codes = AllergenDetector.detectAllergenCodes("Đậu phộng rang", "dau phong rang");
        assertTrue(codes.contains("PEANUT"));
        assertFalse(codes.contains("SOY"));

        Set<String> peanutButter = AllergenDetector.detectAllergenCodes("Bơ đậu phộng", "bo dau phong");
        assertTrue(peanutButter.contains("PEANUT"));
    }

    @Test
    void detectsTomatoAccurately() {
        Set<String> codes = AllergenDetector.detectAllergenCodes("Cà chua bi", "ca chua bi");
        assertTrue(codes.contains("TOMATO"));
    }

    @Test
    void avoidsFalsePositiveSubstringMatches() {
        // "ca rot" should not match "ot" (CHILI)
        Set<String> carrot = AllergenDetector.detectAllergenCodes("Cà rốt", "ca rot");
        assertFalse(carrot.contains("CHILI"));

        // "ớt chuông" should match "CHILI"
        Set<String> chili = AllergenDetector.detectAllergenCodes("Ớt chuông", "ot chuong");
        assertTrue(chili.contains("CHILI"));
    }

    @Test
    void detectsGlutenInWheatNoodleAndWontonWrapper() {
        Set<String> instantNoodle = AllergenDetector.detectAllergenCodes("Mì ăn liền", "mi an lien");
        assertTrue(instantNoodle.contains("GLUTEN"));

        Set<String> wontonWrapper = AllergenDetector.detectAllergenCodes("Vỏ hoành thánh", "vo hoanh thanh");
        assertTrue(wontonWrapper.contains("GLUTEN"));
    }

    @Test
    void returnsEmptyForSafeIngredients() {
        Set<String> rice = AllergenDetector.detectAllergenCodes("Gạo lứt", "gao lut");
        assertTrue(rice.isEmpty());

        Set<String> broccoli = AllergenDetector.detectAllergenCodes("Bông cải xanh", "bong cai xanh");
        assertTrue(broccoli.isEmpty());
    }
}
