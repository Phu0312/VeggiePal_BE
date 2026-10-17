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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MealPlanValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MealPlanValidator.class);
    private static final Set<String> REQUIRED_MEALS = Set.of("BREAKFAST", "LUNCH", "DINNER");
    private static final List<String> STANDARD_DAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );
    private static final List<String> STANDARD_MEALS = List.of(
            "BREAKFAST", "LUNCH", "DINNER"
    );

    private final ObjectMapper mapper;
    private final double toleranceRatio;
    private final double minPortionScale;
    private final double maxPortionScale;

    public MealPlanValidator(ObjectMapper mapper) {
        this(mapper, 0.15, 0.75, 1.50);
    }

    @Autowired
    public MealPlanValidator(
            ObjectMapper mapper,
            @Value("${ai.meal-plan.target-tolerance-ratio:0.15}") double toleranceRatio,
            @Value("${ai.meal-plan.min-portion-scale:0.75}") double minPortionScale,
            @Value("${ai.meal-plan.max-portion-scale:1.50}") double maxPortionScale) {
        this.mapper = mapper;
        this.toleranceRatio = toleranceRatio;
        this.minPortionScale = minPortionScale;
        this.maxPortionScale = maxPortionScale;
    }

    public ArrayNode safeCandidates(JsonNode recipes, JsonNode allergies, List<String> availableIngredients) {
        if (!recipes.isArray()) throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
        List<JsonNode> safe = new ArrayList<>();
        for (JsonNode recipe : recipes) {
            if (isVegan(recipe) && isAllergySafe(recipe, allergies) && canCalculateMacro(recipe)) {
                safe.add(recipe);
            }
        }
        if (safe.isEmpty()) throw new AppException(ErrorCode.NO_VALID_RECIPES);

        Set<String> available = normalized(availableIngredients);
        if (!available.isEmpty()) {
            safe.sort((r1, r2) -> {
                int c1 = countMatchingAvailableIngredients(r1, available);
                int c2 = countMatchingAvailableIngredients(r2, available);
                return Integer.compare(c2, c1);
            });
        }

        ArrayNode result = mapper.createArrayNode();
        safe.forEach(result::add);
        return result;
    }

    public int countMatchingAvailableIngredients(JsonNode recipe, Set<String> available) {
        if (available == null || available.isEmpty()) return 0;
        int matches = 0;
        for (String wanted : available) {
            boolean matched = false;
            for (JsonNode ingredient : recipe.path("ingredients")) {
                String value = normalize(ingredient.path("ingredientName").asText());
                String normalizedName = normalize(ingredient.path("normalizedName").asText());
                if ((!value.isBlank() && (value.contains(wanted) || wanted.contains(value)))
                        || (!normalizedName.isBlank() && (normalizedName.contains(wanted) || wanted.contains(normalizedName)))) {
                    matched = true;
                    break;
                }
            }
            if (matched) matches++;
        }
        return matches;
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
        if (recipes == null || !recipes.isArray()) throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);

        Map<Long, JsonNode> byId = new LinkedHashMap<>();
        List<JsonNode> candidateList = new ArrayList<>();
        for (JsonNode recipe : recipes) {
            if (isVegan(recipe) && isAllergySafe(recipe, allergies) && canCalculateMacro(recipe)) {
                long id = recipe.path("id").asLong();
                byId.put(id, recipe);
                candidateList.add(recipe);
            }
        }
        if (byId.isEmpty()) throw new AppException(ErrorCode.NO_VALID_RECIPES);

        Map<String, ObjectNode> existingDays = new HashMap<>();
        JsonNode daysNode = plan.path("days");
        if (daysNode.isArray()) {
            for (JsonNode d : daysNode) {
                if (d instanceof ObjectNode on) {
                    String dName = normalizeDay(on.path("day").asText());
                    if (!dName.isBlank() && !existingDays.containsKey(dName)) {
                        existingDays.put(dName, on);
                    }
                }
            }
        }

        ArrayNode finalDaysArray = mapper.createArrayNode();
        Map<Long, Integer> weeklyUsage = new HashMap<>();

        for (String standardDay : STANDARD_DAYS) {
            ObjectNode dayNode = existingDays.get(standardDay);
            if (dayNode == null) {
                dayNode = mapper.createObjectNode();
            }
            dayNode.put("day", standardDay);

            Map<String, ObjectNode> existingMeals = new HashMap<>();
            JsonNode mealsNode = dayNode.path("meals");
            if (mealsNode.isArray()) {
                for (JsonNode m : mealsNode) {
                    if (m instanceof ObjectNode on) {
                        String mType = on.path("mealType").asText().toUpperCase(Locale.ROOT).trim();
                        if (REQUIRED_MEALS.contains(mType) && !existingMeals.containsKey(mType)) {
                            existingMeals.put(mType, on);
                        }
                    }
                }
            }

            ArrayNode finalMealsArray = mapper.createArrayNode();
            Set<Long> sameDayRecipeIds = new HashSet<>();
            List<ObjectNode> processedMeals = new ArrayList<>();
            List<Macro> mealBaseMacros = new ArrayList<>();

            for (String mealType : STANDARD_MEALS) {
                ObjectNode mealNode = existingMeals.get(mealType);
                if (mealNode == null) {
                    mealNode = mapper.createObjectNode();
                    mealNode.put("note", "Bữa ăn cân bằng dinh dưỡng");
                }
                mealNode.put("mealType", mealType);

                long currentRecipeId = mealNode.path("recipeId").asLong(-1);
                boolean isValid = byId.containsKey(currentRecipeId)
                        && (byId.size() < 3 || !sameDayRecipeIds.contains(currentRecipeId));

                long selectedRecipeId;
                if (isValid) {
                    selectedRecipeId = currentRecipeId;
                } else {
                    selectedRecipeId = selectBestCandidate(candidateList, sameDayRecipeIds, weeklyUsage, byId.size());
                    mealNode.put("recipeId", selectedRecipeId);
                }

                sameDayRecipeIds.add(selectedRecipeId);
                weeklyUsage.merge(selectedRecipeId, 1, Integer::sum);

                JsonNode recipe = byId.get(selectedRecipeId);
                Macro baseMacro = macroFor(recipe);
                processedMeals.add(mealNode);
                mealBaseMacros.add(baseMacro);
            }

            Macro dayBaseTotal = new Macro();
            for (Macro m : mealBaseMacros) dayBaseTotal.add(m);

            BigDecimal scaleFactor = computePortionScale(dayBaseTotal, target);

            Macro dayScaledTotal = new Macro();
            for (int i = 0; i < 3; i++) {
                ObjectNode mealNode = processedMeals.get(i);
                Macro base = mealBaseMacros.get(i);
                Macro scaled = (scaleFactor.compareTo(BigDecimal.ONE) == 0) ? base : base.multiply(scaleFactor);

                mealNode.set("nutrition", scaled.toJson(mapper));
                if (scaleFactor.compareTo(BigDecimal.ONE) != 0) {
                    mealNode.put("servingMultiplier", scaleFactor.setScale(2, RoundingMode.HALF_UP));
                } else {
                    mealNode.remove("servingMultiplier");
                }
                dayScaledTotal.add(scaled);
                finalMealsArray.add(mealNode);
            }

            dayNode.set("meals", finalMealsArray);
            dayNode.set("nutritionTotals", dayScaledTotal.toJson(mapper));

            validateDailyTargetWithTolerance(dayScaledTotal, target);

            finalDaysArray.add(dayNode);
        }

        plan.set("days", finalDaysArray);
    }

    private long selectBestCandidate(List<JsonNode> candidates, Set<Long> sameDayUsed,
            Map<Long, Integer> weeklyUsage, int totalDistinctCandidates) {
        List<JsonNode> eligible = new ArrayList<>();
        for (JsonNode c : candidates) {
            long id = c.path("id").asLong();
            if (totalDistinctCandidates < 3 || !sameDayUsed.contains(id)) {
                eligible.add(c);
            }
        }
        if (eligible.isEmpty()) {
            eligible = candidates;
        }

        JsonNode best = eligible.get(0);
        int minUsage = weeklyUsage.getOrDefault(best.path("id").asLong(), 0);
        for (int i = 1; i < eligible.size(); i++) {
            JsonNode cur = eligible.get(i);
            int curUsage = weeklyUsage.getOrDefault(cur.path("id").asLong(), 0);
            if (curUsage < minUsage) {
                best = cur;
                minUsage = curUsage;
            }
        }
        return best.path("id").asLong();
    }

    private BigDecimal computePortionScale(Macro dayBaseTotal, MealPlanDtos.NutritionTarget target) {
        if (target == null) return BigDecimal.ONE;

        BigDecimal min = target.minCalories();
        BigDecimal max = target.maxCalories();

        if (inside(dayBaseTotal.calories, min, max)) {
            return BigDecimal.ONE;
        }

        BigDecimal targetCal;
        if (min != null && max != null) {
            targetCal = min.add(max).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        } else if (min != null) {
            targetCal = min;
        } else if (max != null) {
            targetCal = max;
        } else {
            return BigDecimal.ONE;
        }

        if (dayBaseTotal.calories.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }

        BigDecimal desired = targetCal.divide(dayBaseTotal.calories, 4, RoundingMode.HALF_UP);
        BigDecimal minScale = BigDecimal.valueOf(minPortionScale);
        BigDecimal maxScale = BigDecimal.valueOf(maxPortionScale);

        return desired.max(minScale).min(maxScale);
    }

    private String normalizeDay(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (STANDARD_DAYS.contains(s)) return s;
        if (s.startsWith("DAY_") || s.startsWith("DAY ")) {
            String num = s.substring(4).trim();
            try {
                int idx = Integer.parseInt(num) - 1;
                if (idx >= 0 && idx < STANDARD_DAYS.size()) return STANDARD_DAYS.get(idx);
            } catch (NumberFormatException ignored) {}
        }
        return "";
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

    public boolean containsAvailableIngredient(JsonNode recipe, Set<String> available) {
        for (JsonNode ingredient : recipe.path("ingredients")) {
            String value = normalize(ingredient.path("ingredientName").asText());
            String normalizedName = normalize(ingredient.path("normalizedName").asText());
            for (String wanted : available) if ((!value.isBlank() && (value.contains(wanted) || wanted.contains(value)))
                    || (!normalizedName.isBlank() && (normalizedName.contains(wanted) || wanted.contains(normalizedName)))) return true;
        }
        return false;
    }

    public Set<String> normalized(List<String> values) {
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

    private void checkRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    public void validateDailyTarget(Macro value, MealPlanDtos.NutritionTarget target) {
        validateDailyTargetWithTolerance(value, target);
    }

    private void validateDailyTargetWithTolerance(Macro value, MealPlanDtos.NutritionTarget target) {
        if (target == null) return;
        BigDecimal tol = BigDecimal.valueOf(toleranceRatio);
        if (!insideWithTolerance(value.calories, target.minCalories(), target.maxCalories(), tol)
                || !insideWithTolerance(value.protein, target.minProtein(), target.maxProtein(), tol)
                || !insideWithTolerance(value.carbohydrate, target.minCarbohydrate(), target.maxCarbohydrate(), tol)
                || !insideWithTolerance(value.fat, target.minFat(), target.maxFat(), tol)) {
            throw new AppException(ErrorCode.NUTRITION_TARGET_UNATTAINABLE);
        }
    }

    private boolean insideWithTolerance(BigDecimal value, BigDecimal min, BigDecimal max, BigDecimal tol) {
        if (min != null) {
            BigDecimal effectiveMin = min.multiply(BigDecimal.ONE.subtract(tol));
            if (value.compareTo(effectiveMin) < 0) return false;
        }
        if (max != null) {
            BigDecimal effectiveMax = max.multiply(BigDecimal.ONE.add(tol));
            if (value.compareTo(effectiveMax) > 0) return false;
        }
        return true;
    }

    private boolean inside(BigDecimal value, BigDecimal min, BigDecimal max) {
        return (min == null || value.compareTo(min) >= 0) && (max == null || value.compareTo(max) <= 0);
    }

    private AppException invalidAi() { return new AppException(ErrorCode.INVALID_AI_RESPONSE); }

    public static final class Macro {
        private BigDecimal calories = BigDecimal.ZERO;
        private BigDecimal protein = BigDecimal.ZERO;
        private BigDecimal carbohydrate = BigDecimal.ZERO;
        private BigDecimal fat = BigDecimal.ZERO;

        public Macro() {}

        public Macro(BigDecimal calories, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {
            this.calories = calories != null ? calories : BigDecimal.ZERO;
            this.protein = protein != null ? protein : BigDecimal.ZERO;
            this.carbohydrate = carbohydrate != null ? carbohydrate : BigDecimal.ZERO;
            this.fat = fat != null ? fat : BigDecimal.ZERO;
        }

        public void add(Macro other) {
            calories = calories.add(other.calories);
            protein = protein.add(other.protein);
            carbohydrate = carbohydrate.add(other.carbohydrate);
            fat = fat.add(other.fat);
        }

        public Macro multiply(BigDecimal factor) {
            return new Macro(
                    calories.multiply(factor),
                    protein.multiply(factor),
                    carbohydrate.multiply(factor),
                    fat.multiply(factor)
            );
        }

        public ObjectNode toJson(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("calories", rounded(calories));
            node.put("protein", rounded(protein));
            node.put("carbohydrate", rounded(carbohydrate));
            node.put("fat", rounded(fat));
            return node;
        }

        private BigDecimal rounded(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

        public BigDecimal getCalories() { return calories; }
        public BigDecimal getProtein() { return protein; }
        public BigDecimal getCarbohydrate() { return carbohydrate; }
        public BigDecimal getFat() { return fat; }
    }
}
