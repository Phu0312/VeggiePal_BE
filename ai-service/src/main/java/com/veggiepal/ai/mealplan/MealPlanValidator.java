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
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MealPlanValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MealPlanValidator.class);
    private static final Set<String> REQUIRED_MEALS = Set.of("BREAKFAST", "LUNCH", "DINNER");
    public static final List<String> STANDARD_DAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );
    public static final List<String> STANDARD_MEALS = List.of(
            "BREAKFAST", "LUNCH", "DINNER"
    );
    // Share of the day's calorie target each meal aims for (spec: breakfast 25%, lunch 40%, dinner 35%).
    private static final Map<String, BigDecimal> MEAL_SHARES = Map.of(
            "BREAKFAST", new BigDecimal("0.25"),
            "LUNCH", new BigDecimal("0.40"),
            "DINNER", new BigDecimal("0.35"));

    private final ObjectMapper mapper;
    private final double minPortionScale;
    private final double maxPortionScale;

    public MealPlanValidator(ObjectMapper mapper) {
        this(mapper, 0.75, 2.00);
    }

    @Autowired
    public MealPlanValidator(
            ObjectMapper mapper,
            @Value("${ai.meal-plan.min-portion-scale:0.75}") double minPortionScale,
            @Value("${ai.meal-plan.max-portion-scale:2.00}") double maxPortionScale) {
        this.mapper = mapper;
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
            MealPlanDtos.NutritionTarget target, boolean repick) {
        if (recipes == null || !recipes.isArray()) throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);

        Map<Long, JsonNode> byId = new LinkedHashMap<>();
        Map<Long, Macro> baseMacros = new HashMap<>();
        List<JsonNode> candidateList = new ArrayList<>();
        for (JsonNode recipe : recipes) {
            if (!isVegan(recipe) || !isAllergySafe(recipe, allergies)) continue;
            Optional<Macro> macro = tryCalculateMacro(recipe);
            if (macro.isEmpty()) continue;
            long id = recipe.path("id").asLong();
            byId.put(id, recipe);
            baseMacros.put(id, macro.get());
            candidateList.add(recipe);
        }
        if (byId.isEmpty()) throw new AppException(ErrorCode.NO_VALID_RECIPES);

        BigDecimal mid = midCalories(target);

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
            Macro dayTotal = new Macro();

            for (int i = 0; i < STANDARD_MEALS.size(); i++) {
                String mealType = STANDARD_MEALS.get(i);
                ObjectNode mealNode = existingMeals.get(mealType);
                if (mealNode == null) {
                    mealNode = mapper.createObjectNode();
                    mealNode.put("note", "Bữa ăn cân bằng dinh dưỡng");
                }
                mealNode.put("mealType", mealType);

                // Budgets are taken one meal at a time, so a meal that falls short leaves more for the next.
                BigDecimal budget = mid == null ? null : mealBudget(i, mid, dayTotal.calories);
                long currentRecipeId = mealNode.path("recipeId").asLong(-1);
                boolean keep = byId.containsKey(currentRecipeId)
                        && (byId.size() < 3 || !sameDayRecipeIds.contains(currentRecipeId))
                        && (!repick || reachable(baseMacros.get(currentRecipeId).calories, budget));
                long selectedRecipeId = keep ? currentRecipeId
                        : selectBestCandidate(candidateList, baseMacros, sameDayRecipeIds, weeklyUsage, byId.size(), budget);
                // The existing note describes the recipe it was written for, not the one swapped in.
                if (selectedRecipeId != currentRecipeId && existingMeals.containsKey(mealType)) {
                    mealNode.put("note", "Bữa ăn cân bằng dinh dưỡng");
                }
                mealNode.put("recipeId", selectedRecipeId);
                mealNode.put("recipeTitle", byId.get(selectedRecipeId).path("title").asText());

                sameDayRecipeIds.add(selectedRecipeId);
                weeklyUsage.merge(selectedRecipeId, 1, Integer::sum);

                Macro base = baseMacros.get(selectedRecipeId);
                BigDecimal scale = portionScale(base.calories, budget);
                Macro scaled = (scale.compareTo(BigDecimal.ONE) == 0) ? base : base.multiply(scale);
                mealNode.set("nutrition", scaled.toJson(mapper));
                if (scale.compareTo(BigDecimal.ONE) != 0) {
                    mealNode.put("servingMultiplier", scale);
                } else {
                    mealNode.remove("servingMultiplier");
                }
                dayTotal.add(scaled);
                finalMealsArray.add(mealNode);
            }

            dayNode.set("meals", finalMealsArray);
            dayNode.set("nutritionTotals", dayTotal.toJson(mapper));
            markTarget(dayNode, dayTotal, target);

            finalDaysArray.add(dayNode);
        }

        plan.set("days", finalDaysArray);
    }

    public Map<String, BigDecimal> mealBudgets(MealPlanDtos.NutritionTarget target) {
        Map<String, BigDecimal> budgets = new LinkedHashMap<>();
        BigDecimal mid = midCalories(target);
        if (mid == null) return budgets;
        for (String meal : STANDARD_MEALS) {
            budgets.put(meal, mid.multiply(MEAL_SHARES.get(meal)).setScale(2, RoundingMode.HALF_UP));
        }
        return budgets;
    }

    public String mealBudgetHint(MealPlanDtos.NutritionTarget target) {
        Map<String, BigDecimal> budgets = mealBudgets(target);
        if (budgets.isEmpty()) return "";
        StringJoiner parts = new StringJoiner(", ");
        budgets.forEach((meal, kcal) -> parts.add(meal + " khoảng " + kcal.setScale(0, RoundingMode.HALF_UP).toPlainString() + " kcal"));
        return " Ngân sách calo từng bữa: " + parts + ". Khẩu phần sẽ được nhân từ " + scaleText(minPortionScale)
                + " đến " + scaleText(maxPortionScale)
                + " lần cho khớp, nên chọn món có nutritionPerServing.calories gần ngân sách của bữa.";
    }

    public boolean reachesBudget(JsonNode recipe, BigDecimal budget) {
        return budget == null || tryCalculateMacro(recipe).map(m -> reachable(m.calories, budget)).orElse(false);
    }

    public long pickReplacement(JsonNode candidates, Set<Long> excluded, BigDecimal budget) {
        List<JsonNode> remaining = new ArrayList<>();
        Map<Long, Macro> macros = new HashMap<>();
        for (JsonNode c : candidates) {
            long id = c.path("id").asLong();
            if (excluded.contains(id)) continue;
            Optional<Macro> macro = tryCalculateMacro(c);
            if (macro.isEmpty()) continue;
            remaining.add(c);
            macros.put(id, macro.get());
        }
        if (remaining.isEmpty()) throw new AppException(ErrorCode.NO_VALID_RECIPES);
        return selectBestCandidate(remaining, macros, Set.of(), Map.of(), remaining.size(), budget);
    }

    private String scaleText(double scale) {
        return BigDecimal.valueOf(scale).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    // Prefers recipes that can reach the budget within the portion range (least used this week first);
    // when none can, the one whose clamped portion lands closest. Ties keep candidate order, which
    // already favours recipes using the user's available ingredients. No budget = least used.
    private long selectBestCandidate(List<JsonNode> candidates, Map<Long, Macro> baseMacros, Set<Long> excluded,
            Map<Long, Integer> weeklyUsage, int totalDistinctCandidates, BigDecimal budget) {
        List<JsonNode> eligible = new ArrayList<>();
        for (JsonNode c : candidates) {
            if (totalDistinctCandidates < 3 || !excluded.contains(c.path("id").asLong())) {
                eligible.add(c);
            }
        }
        if (eligible.isEmpty()) {
            eligible = candidates;
        }

        JsonNode best = null;
        boolean bestReachable = false;
        BigDecimal bestError = null;
        int bestUsage = 0;
        for (JsonNode c : eligible) {
            long id = c.path("id").asLong();
            BigDecimal kcal = baseMacros.get(id).calories;
            boolean canReach = reachable(kcal, budget);
            BigDecimal error = canReach ? BigDecimal.ZERO
                    : portionScale(kcal, budget).multiply(kcal).subtract(budget).abs();
            int usage = weeklyUsage.getOrDefault(id, 0);
            boolean better = best == null
                    || (canReach && !bestReachable)
                    || (canReach && bestReachable && usage < bestUsage)
                    || (!canReach && !bestReachable && error.compareTo(bestError) < 0);
            if (better) {
                best = c;
                bestReachable = canReach;
                bestError = error;
                bestUsage = usage;
            }
        }
        return best.path("id").asLong();
    }

    // The day's aim: the midpoint of the range; a single-ended target aims 5% inside its bound, so the
    // 2-decimal rounding of the portion multipliers cannot land the day just outside it.
    private BigDecimal midCalories(MealPlanDtos.NutritionTarget target) {
        if (target == null) return null;
        BigDecimal min = target.minCalories();
        BigDecimal max = target.maxCalories();
        if (min != null && max != null) return min.add(max).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        if (min != null) return min.multiply(new BigDecimal("1.05")).setScale(4, RoundingMode.HALF_UP);
        return max == null ? null : max.multiply(new BigDecimal("0.95")).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal mealBudget(int index, BigDecimal mid, BigDecimal consumed) {
        BigDecimal remainingShare = BigDecimal.ZERO;
        for (int j = index; j < STANDARD_MEALS.size(); j++) {
            remainingShare = remainingShare.add(MEAL_SHARES.get(STANDARD_MEALS.get(j)));
        }
        BigDecimal remaining = mid.subtract(consumed).max(BigDecimal.ZERO);
        return remaining.multiply(MEAL_SHARES.get(STANDARD_MEALS.get(index)))
                .divide(remainingShare, 4, RoundingMode.HALF_UP);
    }

    private boolean reachable(BigDecimal kcal, BigDecimal budget) {
        if (budget == null) return true;
        if (kcal.signum() <= 0) return false;
        BigDecimal ratio = budget.divide(kcal, 4, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.valueOf(minPortionScale)) >= 0
                && ratio.compareTo(BigDecimal.valueOf(maxPortionScale)) <= 0;
    }

    // Rounded to 2 decimals before it multiplies anything, so servingMultiplier x base = shown nutrition.
    private BigDecimal portionScale(BigDecimal kcal, BigDecimal budget) {
        if (budget == null || kcal.signum() <= 0) return BigDecimal.ONE;
        return budget.divide(kcal, 4, RoundingMode.HALF_UP)
                .max(BigDecimal.valueOf(minPortionScale))
                .min(BigDecimal.valueOf(maxPortionScale))
                .setScale(2, RoundingMode.HALF_UP);
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

    public Macro macroFor(JsonNode recipe) {
        return tryCalculateMacro(recipe)
                .orElseThrow(() -> new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE));
    }

    private BigDecimal safeDecimal(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        return node.decimalValue();
    }

    public boolean isVegan(JsonNode recipe) {
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

    // A day that misses the target is flagged, not rejected: one off metric on one day used to fail
    // the whole 7-day plan. Only calories decide targetMet (task sheet US10: within ±10% of the target);
    // protein, carbohydrate and fat are still reported. Stale flags are cleared first because replace()
    // re-runs this on a stored plan.
    public void markTarget(ObjectNode dayNode, Macro value, MealPlanDtos.NutritionTarget target) {
        dayNode.remove(List.of("targetMet", "targetDeviations"));
        if (target == null) return;
        ObjectNode deviations = mapper.createObjectNode();
        addDeviation(deviations, "calories", value.calories, target.minCalories(), target.maxCalories());
        addDeviation(deviations, "protein", value.protein, target.minProtein(), target.maxProtein());
        addDeviation(deviations, "carbohydrate", value.carbohydrate, target.minCarbohydrate(), target.maxCarbohydrate());
        addDeviation(deviations, "fat", value.fat, target.minFat(), target.maxFat());
        dayNode.put("targetMet", !deviations.has("calories"));
        if (!deviations.isEmpty()) dayNode.set("targetDeviations", deviations);
    }

    private void addDeviation(ObjectNode deviations, String metric, BigDecimal value, BigDecimal min, BigDecimal max) {
        if (inside(value, min, max)) return;
        ObjectNode deviation = deviations.putObject(metric);
        deviation.put("actual", value.setScale(2, RoundingMode.HALF_UP));
        if (min != null) deviation.put("min", min);
        if (max != null) deviation.put("max", max);
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
