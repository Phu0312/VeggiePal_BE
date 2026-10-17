package com.veggiepal.ai.mealplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MealPlanValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MealPlanValidator.class);
    private static final Set<String> REQUIRED_MEALS = Set.of("BREAKFAST", "LUNCH", "DINNER");
    private final ObjectMapper mapper;

    public MealPlanValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ArrayNode safeCandidates(JsonNode recipes, JsonNode allergies, List<String> availableIngredients) {
        if (!recipes.isArray()) throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
        ArrayNode safe = mapper.createArrayNode();
        for (JsonNode recipe : recipes) {
            if (isVegan(recipe) && isAllergySafe(recipe, allergies) && canCalculateMacro(recipe)) {
                safe.add(recipe);
            }
        }
        if (safe.isEmpty()) throw new AppException(ErrorCode.NO_VALID_RECIPES);

        Set<String> available = normalized(availableIngredients);
        if (available.isEmpty()) return safe;
        ArrayNode matching = mapper.createArrayNode();
        for (JsonNode recipe : safe) if (containsAvailableIngredient(recipe, available)) matching.add(recipe);
        if (matching.isEmpty()) throw new AppException(ErrorCode.NO_VALID_RECIPES);
        return matching;
    }

    public ArrayNode promptCandidates(JsonNode recipes) {
        ArrayNode result = mapper.createArrayNode();
        for (JsonNode recipe : recipes) {
            ObjectNode item = mapper.createObjectNode();
            item.put("id", recipe.path("id").asLong());
            item.put("title", recipe.path("title").asText());
            ArrayNode names = item.putArray("ingredients");
            for (JsonNode ingredient : recipe.path("ingredients")) names.add(ingredient.path("ingredientName").asText());
            item.set("nutritionPerServing", nutritionFor(recipe));
            result.add(item);
        }
        return result;
    }

    public void validateTarget(MealPlanDtos.NutritionTarget target) {
        if (target == null) return;
        checkRange(target.minCalories(), target.maxCalories());
        checkRange(target.minProtein(), target.maxProtein());
        checkRange(target.minCarbohydrate(), target.maxCarbohydrate());
        checkRange(target.minFat(), target.maxFat());
    }

    public void validateAndEnrich(ObjectNode plan, JsonNode recipes, JsonNode allergies,
            MealPlanDtos.NutritionTarget target) {
        JsonNode days = plan.path("days");
        if (!days.isArray() || days.size() != 7) throw invalidAi();
        Map<Long, JsonNode> byId = new HashMap<>();
        recipes.forEach(recipe -> byId.put(recipe.path("id").asLong(), recipe));
        Set<String> dayNames = new HashSet<>();

        for (JsonNode dayNode : days) {
            if (!(dayNode instanceof ObjectNode day)) throw invalidAi();
            String dayName = day.path("day").asText().trim().toUpperCase(Locale.ROOT);
            if (dayName.isBlank() || !dayNames.add(dayName)) throw invalidAi();
            JsonNode meals = day.path("meals");
            if (!meals.isArray() || meals.size() != 3) throw invalidAi();
            Set<String> mealTypes = new HashSet<>();
            Macro total = new Macro();
            for (JsonNode mealNode : meals) {
                if (!(mealNode instanceof ObjectNode meal)) throw invalidAi();
                String type = meal.path("mealType").asText().toUpperCase(Locale.ROOT);
                if (!REQUIRED_MEALS.contains(type) || !mealTypes.add(type)) throw invalidAi();
                JsonNode recipe = byId.get(meal.path("recipeId").asLong(-1));
                if (recipe == null || !isVegan(recipe) || !isAllergySafe(recipe, allergies)) throw invalidAi();
                Macro macro = macroFor(recipe);
                meal.set("nutrition", macro.toJson(mapper));
                total.add(macro);
            }
            if (!mealTypes.equals(REQUIRED_MEALS)) throw invalidAi();
            day.set("nutritionTotals", total.toJson(mapper));
            validateDailyTarget(total, target);
        }
    }

    public boolean canCalculateMacro(JsonNode recipe) {
        return tryCalculateMacro(recipe).isPresent();
    }

    public Optional<Macro> tryCalculateMacro(JsonNode recipe) {
        if (recipe == null) return Optional.empty();
        BigDecimal servings = safeDecimal(recipe.path("servings"));
        if (servings == null || servings.signum() <= 0) {
            log.warn("Excluding recipe id={}, title='{}': invalid servings",
                    recipe.path("id").asLong(-1), recipe.path("title").asText());
            return Optional.empty();
        }
        JsonNode ingredients = recipe.path("ingredients");
        if (!ingredients.isArray() || ingredients.isEmpty()) {
            log.warn("Excluding recipe id={}, title='{}': empty ingredients",
                    recipe.path("id").asLong(-1), recipe.path("title").asText());
            return Optional.empty();
        }
        Macro result = new Macro();
        for (JsonNode ingredient : ingredients) {
            BigDecimal cal = safeDecimal(ingredient.path("caloriesPer100g"));
            BigDecimal pro = safeDecimal(ingredient.path("proteinPer100g"));
            BigDecimal carbs = safeDecimal(ingredient.path("carbsPer100g"));
            BigDecimal fat = safeDecimal(ingredient.path("fatPer100g"));
            if (cal == null || pro == null || carbs == null || fat == null
                    || cal.signum() < 0 || pro.signum() < 0 || carbs.signum() < 0 || fat.signum() < 0) {
                log.warn("Excluding recipe id={}, title='{}': invalid or missing nutrition data for ingredient '{}'",
                        recipe.path("id").asLong(-1), recipe.path("title").asText(), ingredient.path("ingredientName").asText());
                return Optional.empty();
            }
            BigDecimal quantity = safeDecimal(ingredient.path("quantity"));
            String unit = ingredient.path("unit").asText();
            String name = ingredient.path("ingredientName").asText();
            String norm = ingredient.path("normalizedName").asText();
            Optional<BigDecimal> gramsOpt = IngredientUnitConverter.toGrams(quantity, unit, name, norm);
            if (gramsOpt.isEmpty()) {
                log.warn("Excluding recipe id={}, title='{}': cannot convert unit '{}' for ingredient '{}'",
                        recipe.path("id").asLong(-1), recipe.path("title").asText(), unit, name);
                return Optional.empty();
            }
            BigDecimal grams = gramsOpt.get();
            BigDecimal factor = grams.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                    .divide(servings, 8, RoundingMode.HALF_UP);
            result.calories = result.calories.add(cal.multiply(factor));
            result.protein = result.protein.add(pro.multiply(factor));
            result.carbohydrate = result.carbohydrate.add(carbs.multiply(factor));
            result.fat = result.fat.add(fat.multiply(factor));
        }
        return Optional.of(result);
    }

    private ObjectNode nutritionFor(JsonNode recipe) {
        return macroFor(recipe).toJson(mapper);
    }

    private Macro macroFor(JsonNode recipe) {
        return tryCalculateMacro(recipe)
                .orElseThrow(() -> new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE));
    }

    private BigDecimal safeDecimal(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        return node.decimalValue();
    }

    private boolean isVegan(JsonNode recipe) {
        JsonNode ingredients = recipe.path("ingredients");
        if (!ingredients.isArray() || ingredients.isEmpty()) return false;
        for (JsonNode ingredient : ingredients) if (!ingredient.path("vegan").asBoolean(false)) return false;
        return true;
    }

    public boolean isAllergySafe(JsonNode recipe, JsonNode allergies) {
        if (allergies == null || !allergies.isArray() || allergies.isEmpty()) return true;

        Set<String> userAllergyCodes = new HashSet<>();
        Set<String> userAllergyTerms = new HashSet<>();
        allergies.forEach(item -> {
            String code = item.path("code").asText().trim().toUpperCase(Locale.ROOT);
            if (!code.isBlank()) userAllergyCodes.add(code);
            addTerm(userAllergyTerms, code);
            addTerm(userAllergyTerms, item.path("name").asText());
        });

        JsonNode ingredients = recipe.path("ingredients");
        if (!ingredients.isArray() || ingredients.isEmpty()) return false;

        for (JsonNode ingredient : ingredients) {
            JsonNode allergenNode = ingredient.path("allergen");
            if (allergenNode.isMissingNode() || allergenNode.isNull()) {
                log.warn("Excluding recipe id={}, title='{}': ingredient '{}' has missing allergen info",
                        recipe.path("id").asLong(-1), recipe.path("title").asText(),
                        ingredient.path("ingredientName").asText());
                return false;
            }

            String name = ingredient.path("ingredientName").asText();
            String norm = ingredient.path("normalizedName").asText();
            String normalizedIngName = normalize(name);
            String normalizedIngNorm = normalize(norm);

            for (String term : userAllergyTerms) {
                if ((!normalizedIngName.isBlank() && AllergenDetector.matchesWord(normalizedIngName, term))
                        || (!normalizedIngNorm.isBlank() && AllergenDetector.matchesWord(normalizedIngNorm, term))) {
                    return false;
                }
            }

            Set<String> detectedAllergenCodes = AllergenDetector.detectAllergenCodes(name, norm);
            for (String detectedCode : detectedAllergenCodes) {
                if (userAllergyCodes.contains(detectedCode)) {
                    return false;
                }
            }

            if (allergenNode.asBoolean(false) && detectedAllergenCodes.isEmpty()) {
                log.warn("Excluding recipe id={}, title='{}': ingredient '{}' has unverified allergen flag",
                        recipe.path("id").asLong(-1), recipe.path("title").asText(), name);
                return false;
            }
        }
        return true;
    }

    private boolean containsAvailableIngredient(JsonNode recipe, Set<String> available) {
        for (JsonNode ingredient : recipe.path("ingredients")) {
            String value = normalize(ingredient.path("ingredientName").asText());
            String normalizedName = normalize(ingredient.path("normalizedName").asText());
            for (String wanted : available) if ((!value.isBlank() && (value.contains(wanted) || wanted.contains(value)))
                    || (!normalizedName.isBlank() && (normalizedName.contains(wanted) || wanted.contains(normalizedName)))) return true;
        }
        return false;
    }

    private Set<String> normalized(List<String> values) {
        Set<String> result = new HashSet<>();
        if (values != null) values.forEach(value -> addTerm(result, value));
        return result;
    }

    private void addTerm(Set<String> terms, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) terms.add(normalized);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private BigDecimal decimal(JsonNode node) {
        if (node == null || !node.isNumber()) throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
        return node.decimalValue();
    }

    private void checkRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    private void validateDailyTarget(Macro value, MealPlanDtos.NutritionTarget target) {
        if (target == null) return;
        if (!inside(value.calories, target.minCalories(), target.maxCalories())
                || !inside(value.protein, target.minProtein(), target.maxProtein())
                || !inside(value.carbohydrate, target.minCarbohydrate(), target.maxCarbohydrate())
                || !inside(value.fat, target.minFat(), target.maxFat())) throw invalidAi();
    }

    private boolean inside(BigDecimal value, BigDecimal min, BigDecimal max) {
        return (min == null || value.compareTo(min) >= 0) && (max == null || value.compareTo(max) <= 0);
    }

    private AppException invalidAi() { return new AppException(ErrorCode.INVALID_AI_RESPONSE); }

    private static final class Macro {
        private BigDecimal calories = BigDecimal.ZERO;
        private BigDecimal protein = BigDecimal.ZERO;
        private BigDecimal carbohydrate = BigDecimal.ZERO;
        private BigDecimal fat = BigDecimal.ZERO;

        private void add(Macro other) {
            calories = calories.add(other.calories);
            protein = protein.add(other.protein);
            carbohydrate = carbohydrate.add(other.carbohydrate);
            fat = fat.add(other.fat);
        }

        private ObjectNode toJson(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("calories", rounded(calories));
            node.put("protein", rounded(protein));
            node.put("carbohydrate", rounded(carbohydrate));
            node.put("fat", rounded(fat));
            return node;
        }

        private BigDecimal rounded(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    }
}
