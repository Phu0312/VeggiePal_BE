# Chọn món theo ngân sách calo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mỗi ngày của thực đơn AI đạt mục tiêu calo (±10%): mỗi bữa có ngân sách calo riêng, món được chọn theo ngân sách đó và khẩu phần co giãn riêng từng bữa (0,75–2,0). `targetMet` chỉ xét calo.

**Architecture:** Toàn bộ logic nằm ở `MealPlanValidator` (ai-service):
- `validateAndEnrich` tính ngân sách lần lượt từng bữa, chọn món chạm được ngân sách, rồi co giãn khẩu phần riêng từng bữa.
- `markTarget` bỏ dung sai, chỉ lấy calo làm tiêu chí đạt.
- Thêm vài helper public (`mealBudgets`, `mealBudgetHint`, `reachesBudget`, `pickReplacement`) để `MealPlanService` đưa ngân sách vào prompt và chọn món thay theo ngân sách.

Test chấp nhận chạy thuật toán trên ảnh chụp 115 món seed thật.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Jackson 2 `JsonNode`, JUnit 5 (+ `junit-jupiter-params`), Mockito.

**Spec:** [`docs/superpowers/specs/2026-10-21-meal-plan-calorie-fit-design.md`](../specs/2026-10-21-meal-plan-calorie-fit-design.md)

## Global Constraints

**Git và môi trường**
- **Không commit, không push, không chạy lệnh git đổi trạng thái** (`add`, `rm`, `restore`, `reset`, `stash`…). Long tự review diff và tự commit. Step cuối mỗi task chỉ dừng lại và bàn giao.
- **Chạy Maven bằng Git Bash** (`./mvnw`) từ trong `ai-service/`. PowerShell làm hỏng tham số `-Dtest=...`. Unit test của meal plan không cần database.
- Lệnh test nhanh dùng chung:
  `cd ai-service && ./mvnw test -Dtest='MealPlanValidatorTest,MealPlanServiceTest,MealPlanUpgradeServiceTest,MealPlanControllerTest,MealPlanUpgradeControllerTest,IngredientUnitConverterTest,AllergenDetectorTest'`

**Tham số cố định** (lấy từ spec, phải giống nhau ở mọi chỗ)
- Tỉ lệ ngân sách bữa (hằng số trong code, không đưa ra cấu hình): `BREAKFAST` 0,25, `LUNCH` 0,40, `DINNER` 0,35.
- Calo mục tiêu giữa: `mid = (minCalories + maxCalories) / 2`; chỉ có một đầu thì dùng đầu đó; không có cả hai thì không có ngân sách.
- Ngân sách bữa `i` tính lần lượt: `(mid − calo đã xếp ở các bữa trước) × tỉ lệ bữa i ÷ tổng tỉ lệ các bữa còn lại`, không âm.
- Món "chạm được" ngân sách `B` khi `B / kcal_khẩu_phần` nằm trong `[minScale, maxScale]`.
- `minScale = 0,75`; `maxScale` mặc định **2,00** (`ai.meal-plan.max-portion-scale`, env `AI_MEAL_PLAN_MAX_PORTION_SCALE`).
- Hệ số khẩu phần làm tròn 2 chữ số **trước khi** nhân với dinh dưỡng gốc.

**Hành vi**
- `targetMet = minCalories ≤ calo ngày ≤ maxCalories`, không dung sai. `targetDeviations` liệt kê mọi chỉ số (calories, protein, carbohydrate, fat) nằm ngoài `[min, max]`.
- Xóa property `ai.meal-plan.target-tolerance-ratio` (env `AI_MEAL_PLAN_TOLERANCE_RATIO`) ở cả `src/main` và `src/test`.
- `validateAndEnrich(plan, recipes, allergies, target, boolean repick)`: `generate` và `updateFromHealth` truyền `true`, `replace` truyền `false`. Khi `repick = false`, món đang có được giữ nếu vẫn an toàn và không trùng trong ngày.

**Quyết định của plan** (làm rõ spec §3.6)
- Món thay trong `replace()` phải khác `currentRecipeId` **và khác các món còn lại của ngày đó**. Nếu không, `validateAndEnrich` sẽ phải đổi thêm một bữa khác vì trùng món, trái với "chỉ bữa đang thay được đổi món".
- Test `replace` đặt trong `MealPlanServiceTest`, nơi đã có các test `replace` khác, thay vì `MealPlanUpgradeServiceTest` như spec ghi.

**Lưu ý merge:** nhánh `dev` đã thêm 13 dòng vào `ai-service/src/main/resources/application.properties`; khi merge, giữ cả hai bên.

---

## File Structure

| File | Trách nhiệm |
|---|---|
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java` | Ngân sách bữa, chọn món, co giãn khẩu phần, `markTarget`, helper public cho service |
| `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java` | Truyền `repick`; đưa ngân sách vào prompt `generate`/`replace`; chọn món thay theo ngân sách |
| `ai-service/src/main/resources/application.properties`, `ai-service/src/test/resources/application.properties` | Bỏ dung sai, đổi max scale 2,00 |
| `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanValidatorTest.java` | Unit test cho mọi quy tắc trên |
| `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanServiceTest.java` | Prompt có ngân sách; `replace` theo ngân sách, không đụng bữa khác |
| `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanSeedAcceptanceTest.java` (mới) | 7/7 ngày đạt mục tiêu trên `mealplan/recipes-seed.json` |
| `docs/AI_SERVICE_DOCUMENTATION.md` | Mô tả hành vi mới |

---

### Task 1: `targetMet` chỉ xét calo, bỏ dung sai

**Files:**
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java:36-55` (field + constructor), `:465-499` (`markTarget`, `addDeviation`, `insideWithTolerance`)
- Modify: `ai-service/src/main/resources/application.properties:58`, `ai-service/src/test/resources/application.properties:36`
- Test: `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanValidatorTest.java`

**Interfaces:**
- Produces: constructor `MealPlanValidator(ObjectMapper mapper, double minPortionScale, double maxPortionScale)` (đánh dấu `@Autowired`) và `MealPlanValidator(ObjectMapper mapper)` (gọi `this(mapper, 0.75, 1.50)` ở task này; Task 2 đổi thành `2.00`). `markTarget(ObjectNode, Macro, NutritionTarget)` giữ chữ ký.

- [ ] **Step 1: Viết test fail**

Thêm vào `MealPlanValidatorTest`:

```java
    @Test
    void targetMetLooksOnlyAtCaloriesButStillReportsProtein() {
        ObjectNode day = mapper.createObjectNode();
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                new BigDecimal("2522"), new BigDecimal("3082"),
                new BigDecimal("96"), new BigDecimal("144"),
                null, null, null, null);

        validator.markTarget(day, new MealPlanValidator.Macro(
                new BigDecimal("2800"), new BigDecimal("50"), new BigDecimal("300"), new BigDecimal("80")), target);

        assertEquals(true, day.path("targetMet").asBoolean(false));
        assertEquals(false, day.path("targetDeviations").has("calories"));
        assertEquals(0, new BigDecimal("50.00").compareTo(
                day.path("targetDeviations").path("protein").path("actual").decimalValue()));
    }

    @Test
    void caloriesOutsideTheRangeMissTheTargetEvenWithinTheOldTolerance() {
        ObjectNode day = mapper.createObjectNode();
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                new BigDecimal("2522"), new BigDecimal("3082"), null, null, null, null, null, null);

        // 2400 used to pass: the old rule accepted down to 2522 x 0.85 = 2143.7
        validator.markTarget(day, new MealPlanValidator.Macro(
                new BigDecimal("2400"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), target);

        assertEquals(false, day.path("targetMet").asBoolean(true));
        assertEquals(0, new BigDecimal("2400.00").compareTo(
                day.path("targetDeviations").path("calories").path("actual").decimalValue()));
    }
```

- [ ] **Step 2: Chạy, xác nhận fail**

```bash
cd ai-service && ./mvnw test -Dtest=MealPlanValidatorTest
```
Kỳ vọng: 2 test mới FAIL.
- `targetMetLooksOnlyAtCaloriesButStillReportsProtein`: `targetMet` là false, vì code cũ coi protein lệch là không đạt.
- `caloriesOutsideTheRangeMissTheTargetEvenWithinTheOldTolerance`: `targetMet` là true, vì dung sai 15% còn đó.

- [ ] **Step 3: Sửa constructor và `markTarget`**

Xóa field `toleranceRatio`. Constructor thành:

```java
    public MealPlanValidator(ObjectMapper mapper) {
        this(mapper, 0.75, 1.50);
    }

    @Autowired
    public MealPlanValidator(
            ObjectMapper mapper,
            @Value("${ai.meal-plan.min-portion-scale:0.75}") double minPortionScale,
            @Value("${ai.meal-plan.max-portion-scale:1.50}") double maxPortionScale) {
        this.mapper = mapper;
        this.minPortionScale = minPortionScale;
        this.maxPortionScale = maxPortionScale;
    }
```

Thay `markTarget`, `addDeviation` và xóa `insideWithTolerance`:

```java
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
```

Xóa dòng `ai.meal-plan.target-tolerance-ratio=…` ở `ai-service/src/main/resources/application.properties` và `ai-service/src/test/resources/application.properties`.

- [ ] **Step 4: Chạy lại toàn bộ test nhanh**

Chạy lệnh test nhanh ở Global Constraints. Kỳ vọng: tất cả PASS, kể cả `flagsDaysThatMissTheTargetInsteadOfRejectingThePlan` (342,00 vẫn đúng ở task này) và `marksDaysThatMeetTheTarget`.

- [ ] **Step 5: Dừng, bàn giao cho Long review**

---

### Task 2: Ngân sách từng bữa, chọn món và co giãn khẩu phần riêng từng bữa

**Files:**
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java` (`validateAndEnrich`, `selectBestCandidate`, xóa `computePortionScale`, thêm hằng số và helper private; constructor một tham số dùng `2.00`; `@Value` max mặc định `2.00`)
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java:121`, `:195`, `:289` (truyền `repick`)
- Modify: `ai-service/src/main/resources/application.properties:60`, `ai-service/src/test/resources/application.properties:38` (`1.50` → `2.00`)
- Test: `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanValidatorTest.java`

**Interfaces:**
- Consumes: constructor và `markTarget` của Task 1.
- Produces:
  - `public void validateAndEnrich(ObjectNode plan, JsonNode recipes, JsonNode allergies, MealPlanDtos.NutritionTarget target, boolean repick)`. Chữ ký 4 tham số cũ bị **xóa**.
  - Các helper private mà Task 3 dùng lại: `private static final Map<String, BigDecimal> MEAL_SHARES`, `private BigDecimal midCalories(MealPlanDtos.NutritionTarget)`, `private boolean reachable(BigDecimal kcal, BigDecimal budget)`, `private BigDecimal portionScale(BigDecimal kcal, BigDecimal budget)`, `private long selectBestCandidate(List<JsonNode> candidates, Map<Long, Macro> baseMacros, Set<Long> excluded, Map<Long, Integer> weeklyUsage, int totalDistinctCandidates, BigDecimal budget)`.

- [ ] **Step 1: Thêm helper cho test và các test fail**

Trong `MealPlanValidatorTest`, thêm helper (cạnh các helper hiện có):

```java
    private static final MealPlanDtos.NutritionTarget TARGET_2522_3082 = new MealPlanDtos.NutritionTarget(
            new BigDecimal("2522"), new BigDecimal("3082"), null, null, null, null, null, null);

    // One ingredient of 100 g per serving, so the recipe's kcal per serving equals kcal.
    private ObjectNode recipeWithKcal(long id, int kcal) {
        ObjectNode recipe = mapper.createObjectNode();
        recipe.put("id", id).put("title", "Món " + id).put("servings", 1);
        recipe.putArray("ingredients").addObject()
                .put("ingredientName", "Nguyên liệu " + id).put("normalizedName", "nguyen lieu " + id)
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", kcal).put("proteinPer100g", 10)
                .put("carbsPer100g", 20).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", false);
        return recipe;
    }

    // idKcal = {id1, kcal1, id2, kcal2, ...}
    private ArrayNode recipesWithKcal(int... idKcal) {
        ArrayNode recipes = mapper.createArrayNode();
        for (int i = 0; i < idKcal.length; i += 2) recipes.add(recipeWithKcal(idKcal[i], idKcal[i + 1]));
        return recipes;
    }

    // null leaves that meal out of the plan
    private ObjectNode planOf(Long breakfast, Long lunch, Long dinner) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode days = root.putArray("days");
        for (String dayName : MealPlanValidator.STANDARD_DAYS) {
            ArrayNode meals = days.addObject().put("day", dayName).putArray("meals");
            if (breakfast != null) meals.addObject().put("mealType", "BREAKFAST").put("recipeId", breakfast);
            if (lunch != null) meals.addObject().put("mealType", "LUNCH").put("recipeId", lunch);
            if (dinner != null) meals.addObject().put("mealType", "DINNER").put("recipeId", dinner);
        }
        return root;
    }

    private void assertEveryDayMeetsTarget(ObjectNode plan, MealPlanDtos.NutritionTarget target) {
        for (var day : plan.path("days")) {
            BigDecimal calories = day.path("nutritionTotals").path("calories").decimalValue();
            String where = day.path("day").asText() + " = " + calories;
            assertEquals(true, calories.compareTo(target.minCalories()) >= 0, where);
            assertEquals(true, calories.compareTo(target.maxCalories()) <= 0, where);
            assertEquals(true, day.path("targetMet").asBoolean(false), where);
        }
    }
```

Thêm các test (cần `import java.util.Map;` và `import java.util.Set;`):

```java
    @Test
    void reachesTheDailyTargetWhenTheAiPicksLightRecipes() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = sevenDayPlan(1L); // the AI picked the 120 kcal recipe for every meal

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        assertEveryDayMeetsTarget(plan, TARGET_2522_3082);
        for (var day : plan.path("days")) {
            for (var meal : day.path("meals")) {
                long id = meal.path("recipeId").asLong();
                assertEquals(false, id == 1L || id == 2L, "recipes too light for any budget must not be picked");
                if (meal.has("servingMultiplier")) {
                    BigDecimal scale = meal.path("servingMultiplier").decimalValue();
                    assertEquals(true, scale.compareTo(new BigDecimal("0.75")) >= 0 && scale.compareTo(new BigDecimal("2.00")) <= 0);
                }
            }
        }
    }

    @Test
    void displayedNutritionEqualsBaseTimesDisplayedMultiplier() {
        Map<Long, BigDecimal> kcal = Map.of(1L, new BigDecimal("120"), 2L, new BigDecimal("300"),
                3L, new BigDecimal("500"), 4L, new BigDecimal("700"), 5L, new BigDecimal("650"));
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = sevenDayPlan(1L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        for (var day : plan.path("days")) {
            for (var meal : day.path("meals")) {
                BigDecimal multiplier = meal.has("servingMultiplier") ? meal.path("servingMultiplier").decimalValue() : BigDecimal.ONE;
                BigDecimal expected = kcal.get(meal.path("recipeId").asLong()).multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
                assertEquals(0, expected.compareTo(meal.path("nutrition").path("calories").decimalValue()));
            }
        }
    }

    @Test
    void keepsAiPicksThatReachTheirBudget() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 300, 3, 500, 4, 700, 5, 650);
        ObjectNode plan = planOf(3L, 4L, 5L);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);

        for (var day : plan.path("days")) {
            assertEquals(3L, day.path("meals").get(0).path("recipeId").asLong());
            assertEquals(4L, day.path("meals").get(1).path("recipeId").asLong());
            assertEquals(5L, day.path("meals").get(2).path("recipeId").asLong());
        }
        assertEveryDayMeetsTarget(plan, TARGET_2522_3082);
    }

    @Test
    void laterMealsAbsorbAShortBreakfast() {
        // Breakfast is kept at 100 kcal x 2.00 = 200 (repick = false); lunch and dinner must carry the rest.
        // With fixed shares (lunch 1120.8, dinner 980.7) the day would end near 2300, below 2522.
        ArrayNode recipes = recipesWithKcal(1, 100, 4, 700, 5, 650, 6, 800, 7, 900);
        ObjectNode plan = planOf(1L, null, null);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), TARGET_2522_3082, false);

        for (var day : plan.path("days")) {
            var breakfast = day.path("meals").get(0);
            assertEquals(1L, breakfast.path("recipeId").asLong());
            assertEquals(0, new BigDecimal("2.00").compareTo(breakfast.path("servingMultiplier").decimalValue()));
        }
        assertEveryDayMeetsTarget(plan, TARGET_2522_3082);
    }

    @Test
    void repickFalseKeepsSafeMealsThatMissTheirBudget() {
        ArrayNode recipes = recipesWithKcal(1, 120, 2, 130, 3, 140, 4, 700, 5, 650, 6, 800);

        ObjectNode kept = planOf(1L, 2L, 3L);
        validator.validateAndEnrich(kept, recipes, mapper.createArrayNode(), TARGET_2522_3082, false);
        for (var day : kept.path("days")) {
            assertEquals(Set.of(1L, 2L, 3L), Set.of(
                    day.path("meals").get(0).path("recipeId").asLong(),
                    day.path("meals").get(1).path("recipeId").asLong(),
                    day.path("meals").get(2).path("recipeId").asLong()));
        }

        ObjectNode repicked = planOf(1L, 2L, 3L);
        validator.validateAndEnrich(repicked, recipes, mapper.createArrayNode(), TARGET_2522_3082, true);
        assertEquals(false, repicked.path("days").get(0).path("meals").get(1).path("recipeId").asLong() == 2L,
                "with repick = true an unreachable lunch is replaced");
    }
```

Thêm `, true` vào **cuối mọi lời gọi `validator.validateAndEnrich(...)` có sẵn** trong `MealPlanValidatorTest` (7 chỗ, ở các dòng gần 23, 255, 281, 304, 331, 354, 375).

Trong `flagsDaysThatMissTheTargetInsteadOfRejectingThePlan`, cập nhật comment và số kỳ vọng. Mỗi bữa giờ co giãn riêng, tối đa 2,00 → 76 × 2 × 3 = 456:

```java
        // Recipe has 76 kcal per serving. Each meal is scaled on its own, at most 2.00x -> 152 kcal, 456 kcal a day.
        // Target: 1000-1200 kcal (impossible to reach).
```
```java
        assertEquals(new BigDecimal("456.00"), calories.path("actual").decimalValue());
```

- [ ] **Step 2: Chạy, xác nhận fail**

```bash
cd ai-service && ./mvnw test -Dtest=MealPlanValidatorTest
```
Kỳ vọng: **compile error**, vì `validateAndEnrich` chưa có tham số `repick`. Ghi lại lỗi này làm bằng chứng RED.

- [ ] **Step 3: Viết lại `validateAndEnrich` và phần chọn món**

Trong `MealPlanValidator`:

1. **Hằng số và constructor.** Thêm hằng số dưới `STANDARD_MEALS`:

```java
    // Share of the day's calorie target each meal aims for (spec: breakfast 25%, lunch 40%, dinner 35%).
    private static final Map<String, BigDecimal> MEAL_SHARES = Map.of(
            "BREAKFAST", new BigDecimal("0.25"),
            "LUNCH", new BigDecimal("0.40"),
            "DINNER", new BigDecimal("0.35"));
```

   Constructor một tham số đổi thành `this(mapper, 0.75, 2.00)`, và `@Value("${ai.meal-plan.max-portion-scale:2.00}")`.

2. **Thân `validateAndEnrich`.** Thay toàn bộ (chữ ký mới có `boolean repick`):

```java
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
                mealNode.put("recipeId", selectedRecipeId);

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
```

3. **Bộ chọn món.** Thay `selectBestCandidate` và xóa `computePortionScale`:

```java
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

    private BigDecimal midCalories(MealPlanDtos.NutritionTarget target) {
        if (target == null) return null;
        BigDecimal min = target.minCalories();
        BigDecimal max = target.maxCalories();
        if (min != null && max != null) return min.add(max).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        return min != null ? min : max;
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
```

4. **`MealPlanService`.** Ba lời gọi thêm tham số:

```java
        validator.validateAndEnrich(plan, candidates, allergies, request.dailyNutritionTarget(), true);   // generate (~:121)
        validator.validateAndEnrich(root, safeRecipes, allergies, target, false);                           // replace (~:195)
        validator.validateAndEnrich(root, allRecipes, allergies, recalculatedTarget, true);                 // updateFromHealth (~:289)
```

5. **Cấu hình.** Đổi `ai.meal-plan.max-portion-scale` thành `${AI_MEAL_PLAN_MAX_PORTION_SCALE:2.00}` ở `src/main` và `2.00` ở `src/test`.

- [ ] **Step 4: Chạy lại toàn bộ test nhanh**

Chạy lệnh test nhanh ở Global Constraints. Kỳ vọng: tất cả PASS.
- `appliesPortionScalingToMeetCalorieTarget` và `marksDaysThatMeetTheTarget` vẫn đạt (325,28 kcal, trong khoảng 300–350).
- Nếu một test của `MealPlanServiceTest` hoặc `MealPlanUpgradeServiceTest` fail vì con số gắn với hệ số chung cả ngày cũ: tính lại con số theo quy tắc mới và ghi cách tính vào report. Không được nới lỏng assertion.

- [ ] **Step 5: Dừng, bàn giao cho Long review**

---

### Task 3: Ngân sách vào prompt và chọn món thay theo ngân sách

**Files:**
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanValidator.java` (thêm 4 method public)
- Modify: `ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java` (`generate` prompt; `replace`; xóa `isCandidateSafe`, `selectAlternativeReplacement`)
- Test: `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanValidatorTest.java`, `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanServiceTest.java`

**Interfaces:**
- Consumes: `MEAL_SHARES`, `midCalories`, `reachable`, `selectBestCandidate`, `tryCalculateMacro` (Task 2).
- Produces (public, trên `MealPlanValidator`):
  - `Map<String, BigDecimal> mealBudgets(MealPlanDtos.NutritionTarget target)`: thứ tự BREAKFAST, LUNCH, DINNER; mỗi giá trị là `mid × tỉ lệ`, scale 2; không có `mid` thì trả map rỗng.
  - `String mealBudgetHint(MealPlanDtos.NutritionTarget target)`: chuỗi bắt đầu bằng dấu cách để nối vào prompt, hoặc `""`.
  - `boolean reachesBudget(JsonNode recipe, BigDecimal budget)`: `budget == null` thì trả true.
  - `long pickReplacement(JsonNode candidates, Set<Long> excluded, BigDecimal budget)`: ném `AppException(NO_VALID_RECIPES)` khi không còn ứng viên.

- [ ] **Step 1: Viết test fail cho validator**

Thêm vào `MealPlanValidatorTest`:

```java
    @Test
    void mealBudgetsSplitTheMidpointBetweenMeals() {
        var budgets = validator.mealBudgets(new MealPlanDtos.NutritionTarget(
                new BigDecimal("2400"), new BigDecimal("2600"), null, null, null, null, null, null));
        assertEquals(List.of("BREAKFAST", "LUNCH", "DINNER"), List.copyOf(budgets.keySet()));
        assertEquals(new BigDecimal("625.00"), budgets.get("BREAKFAST"));
        assertEquals(new BigDecimal("1000.00"), budgets.get("LUNCH"));
        assertEquals(new BigDecimal("875.00"), budgets.get("DINNER"));

        var minOnly = validator.mealBudgets(new MealPlanDtos.NutritionTarget(
                new BigDecimal("2000"), null, null, null, null, null, null, null));
        assertEquals(new BigDecimal("800.00"), minOnly.get("LUNCH"));

        assertEquals(true, validator.mealBudgets(null).isEmpty());
    }

    @Test
    void mealBudgetHintNamesEveryMealAndThePortionRange() {
        String hint = validator.mealBudgetHint(new MealPlanDtos.NutritionTarget(
                new BigDecimal("2400"), new BigDecimal("2600"), null, null, null, null, null, null));
        assertEquals(true, hint.contains("BREAKFAST khoảng 625 kcal, LUNCH khoảng 1000 kcal, DINNER khoảng 875 kcal"), hint);
        assertEquals(true, hint.contains("từ 0,75 đến 2 lần"), hint);
        assertEquals("", validator.mealBudgetHint(null));
    }

    @Test
    void reachesBudgetUsesThePortionRange() {
        assertEquals(true, validator.reachesBudget(recipeWithKcal(4, 700), new BigDecimal("1120.80")));
        assertEquals(false, validator.reachesBudget(recipeWithKcal(1, 120), new BigDecimal("1120.80")));
        assertEquals(true, validator.reachesBudget(recipeWithKcal(1, 120), null));
    }

    @Test
    void pickReplacementSkipsExcludedRecipesAndPrefersReachableOnes() {
        ArrayNode candidates = recipesWithKcal(1, 120, 3, 500, 4, 700, 5, 650, 6, 800);
        assertEquals(6L, validator.pickReplacement(candidates, Set.of(3L, 4L, 5L), new BigDecimal("1120.80")));

        AppException ex = assertThrows(AppException.class,
                () -> validator.pickReplacement(candidates, Set.of(1L, 3L, 4L, 5L, 6L), new BigDecimal("1120.80")));
        assertEquals(com.veggiepal.ai.common.ErrorCode.NO_VALID_RECIPES, ex.getErrorCode());
    }
```

Chạy `cd ai-service && ./mvnw test -Dtest=MealPlanValidatorTest`. Kỳ vọng: compile error (các method chưa có).

- [ ] **Step 2: Thêm 4 method public vào `MealPlanValidator`**

Cần `import java.util.StringJoiner;`.

```java
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
```

Chạy lại `./mvnw test -Dtest=MealPlanValidatorTest`. Kỳ vọng: PASS.

- [ ] **Step 3: Viết test fail cho service**

Thêm vào `MealPlanServiceTest` một helper và hai test (dùng đúng cách mock RestClient như các test có sẵn trong file):

```java
    private void addRecipe(ArrayNode content, long id, int kcal) {
        content.addObject().put("id", id).put("title", "Món " + id).put("servings", 1)
                .putArray("ingredients").addObject()
                .put("ingredientName", "Nguyên liệu " + id).put("normalizedName", "nguyen lieu " + id)
                .put("quantity", 100).put("unit", "GRAM")
                .put("caloriesPer100g", kcal).put("proteinPer100g", 10)
                .put("carbsPer100g", 20).put("fatPer100g", 5)
                .put("vegan", true).put("allergen", false);
    }

    private void stubGet(String url, ObjectNode body, boolean withJwt) {
        RestClient.RequestHeadersUriSpec spec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec resp = mock(RestClient.ResponseSpec.class);
        when(http.get().uri(url)).thenReturn(spec);
        if (withJwt) when(spec.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt")).thenReturn(spec);
        when(spec.retrieve()).thenReturn(resp);
        when(resp.body(any(Class.class))).thenReturn(body);
    }

    @Test
    void generatePromptCarriesTheMealBudgets() {
        ObjectNode recipes = mapper.createObjectNode();
        addRecipe(recipes.putObject("result").putArray("content"), 1, 600);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);
        ObjectNode health = mapper.createObjectNode();
        health.putObject("result").put("weightKg", 65).put("heightCm", 170);
        stubGet("http://nutrition-service/nutrition/me/health-records/latest", health, true);
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenThrow(new RuntimeException("Groq down"));
        when(repository.save(any(MealPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(1L, "test-jwt", new MealPlanDtos.GenerateRequest(
                "Kế hoạch", "Tăng cân", List.of(), List.of(),
                new MealPlanDtos.NutritionTarget(new BigDecimal("2400"), new BigDecimal("2600"),
                        null, null, null, null, null, null)));

        verify(groq).complete(any(), argThat((List<com.veggiepal.ai.groq.dto.GroqRequest.Message> messages) ->
                messages.getLast().content().contains("LUNCH khoảng 1000 kcal")), anyDouble(), anyInt());
    }

    @Test
    void replaceRejectsAnAiPickThatCannotReachTheMealBudgetAndLeavesOtherMealsAlone() {
        ObjectNode stored = mapper.createObjectNode();
        ArrayNode days = stored.putArray("days");
        for (String dayName : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")) {
            ArrayNode meals = days.addObject().put("day", dayName).putArray("meals");
            meals.addObject().put("mealType", "BREAKFAST").put("recipeId", 3L);
            meals.addObject().put("mealType", "LUNCH").put("recipeId", 4L);
            meals.addObject().put("mealType", "DINNER").put("recipeId", 5L);
        }
        ObjectNode constraints = stored.putObject("constraints");
        constraints.putArray("availableIngredients");
        constraints.putObject("dailyNutritionTarget").put("minCalories", 2522).put("maxCalories", 3082);
        when(repository.findByIdAndUserId(100L, 1L))
                .thenReturn(java.util.Optional.of(new MealPlan(1L, "Thực đơn", "Tăng cân", stored.toString())));

        ObjectNode recipes = mapper.createObjectNode();
        ArrayNode content = recipes.putObject("result").putArray("content");
        addRecipe(content, 1, 120);
        addRecipe(content, 3, 500);
        addRecipe(content, 4, 700);
        addRecipe(content, 5, 650);
        addRecipe(content, 6, 800);
        stubGet("http://recipe-service/recipes?size=200", recipes, false);
        ObjectNode allergies = mapper.createObjectNode();
        allergies.putArray("result");
        stubGet("http://nutrition-service/nutrition/me/allergies", allergies, true);

        // The AI suggests the 120 kcal recipe for a ~1121 kcal lunch: 120 x 2.00 can never reach it.
        when(groq.complete(any(), any(), any(Double.class), any(Integer.class)))
                .thenReturn(new com.veggiepal.ai.groq.GroqClient.GroqResult("{\"recipeId\": 1, \"note\": \"Món nhẹ\"}", 100, 50));

        MealPlanDtos.MealPlanResponse response = service.replace(1L, 100L, "test-jwt",
                new MealPlanDtos.ReplaceMealRequest("MONDAY", "LUNCH", 4L));

        verify(groq).complete(any(), argThat((List<com.veggiepal.ai.groq.dto.GroqRequest.Message> messages) ->
                messages.getLast().content().contains("khoảng 1121 kcal")), anyDouble(), anyInt());
        var planDays = mapper.valueToTree(response.plan()).path("days");
        // 6 is the only reachable recipe that is neither the current lunch (4) nor another Monday meal (3, 5).
        assertEquals(6L, planDays.get(0).path("meals").get(1).path("recipeId").asLong());
        for (int d = 0; d < 7; d++) {
            var meals = planDays.get(d).path("meals");
            assertEquals(3L, meals.get(0).path("recipeId").asLong());
            assertEquals(5L, meals.get(2).path("recipeId").asLong());
            if (d > 0) assertEquals(4L, meals.get(1).path("recipeId").asLong());
        }
    }
```

Chạy `cd ai-service && ./mvnw test -Dtest=MealPlanServiceTest`. Kỳ vọng: 2 test mới FAIL.
- Prompt chưa có ngân sách.
- `replace` nhận món 1 do LLM chọn (thứ Hai bữa trưa ra 1, không phải 6).

- [ ] **Step 4: Sửa `MealPlanService`**

**`generate`:** nối gợi ý ngân sách vào cuối prompt, ngay sau `.formatted(...)`:

```java
                """.formatted(request.healthGoal(), request.preferences(), request.availableIngredients(), health,
                allergies, request.dailyNutritionTarget(), json(promptRecipes))
                + validator.mealBudgetHint(request.dailyNutritionTarget());
```

**`replace`:** thay đoạn từ dòng dựng `prompt` tới hết khối `if (!validReplacement) {…}`:

```java
        String mealType = request.mealType().trim().toUpperCase(Locale.ROOT);
        BigDecimal budget = validator.mealBudgets(target).get(mealType);
        String prompt = "Chọn một recipe thay thế từ danh sách thật, khác ID " + request.currentRecipeId()
                + ", phù hợp mục tiêu " + entity.getGoal()
                + (budget == null ? "" : ", ngân sách calo của bữa này khoảng "
                        + budget.setScale(0, RoundingMode.HALF_UP).toPlainString() + " kcal")
                + ". Trả duy nhất JSON {\"recipeId\":1,\"note\":\"...\"}, note viết bằng tiếng Việt. Recipes: "
                + json(validator.promptCandidates(candidates));

        long replacementId = -1;
        String note = "Món ăn thay thế dinh dưỡng";
        try {
            JsonNode replacement = call(userId, prompt);
            replacementId = replacement.path("recipeId").asLong(-1);
            if (replacement.hasNonNull("note") && !replacement.path("note").asText().isBlank()) {
                note = replacement.path("note").asText();
            }
        } catch (Exception ex) {
            log.warn("AI recipe replacement failed, selecting best deterministic alternative.", ex);
        }

        // The replacement must differ from the current recipe and from the day's other meals: a same-day
        // duplicate would make validateAndEnrich change another meal, and only this one may change.
        Set<Long> excluded = otherRecipeIdsOfDay(root, request.day(), mealType);
        Long currentId = request.currentRecipeId();
        if (currentId != null) excluded.add(currentId);
        JsonNode picked = replacementId > 0 ? candidate(candidates, replacementId) : null;
        boolean validReplacement = picked != null
                && !excluded.contains(replacementId)
                && validator.isAllergySafe(picked, allergies)
                && validator.reachesBudget(picked, budget);

        if (!validReplacement) {
            replacementId = validator.pickReplacement(candidates, excluded, budget);
        }
```

Vòng lặp tìm bữa để thay ở phía dưới giữ nguyên, nhưng so sánh với `mealType` đã chuẩn hóa, thay cho `request.mealType()`. Xóa `isCandidateSafe` và `selectAlternativeReplacement`. Thêm hai helper:

```java
    private JsonNode candidate(JsonNode candidates, long id) {
        for (JsonNode c : candidates) {
            if (c.path("id").asLong() == id) return c;
        }
        return null;
    }

    private Set<Long> otherRecipeIdsOfDay(JsonNode root, String day, String mealType) {
        Set<Long> ids = new HashSet<>();
        for (JsonNode d : root.path("days")) {
            if (!d.path("day").asText().equalsIgnoreCase(day)) continue;
            for (JsonNode m : d.path("meals")) {
                if (!m.path("mealType").asText().equalsIgnoreCase(mealType)) ids.add(m.path("recipeId").asLong());
            }
        }
        return ids;
    }
```

- [ ] **Step 5: Chạy lại toàn bộ test nhanh**

Chạy lệnh test nhanh ở Global Constraints. Kỳ vọng: tất cả PASS, kể cả test cũ `replaceMealSuccessfullyUpdatesRecipeAndEnrichesNutrition`: không có mục tiêu calo nên không có ngân sách; món 2 khác món 1 nên được nhận.

- [ ] **Step 6: Dừng, bàn giao cho Long review**

---

### Task 4: Test chấp nhận trên 115 món seed và tài liệu

**Files:**
- Create: `ai-service/src/test/java/com/veggiepal/ai/mealplan/MealPlanSeedAcceptanceTest.java`
- Modify: `docs/AI_SERVICE_DOCUMENTATION.md` (~:225, ~:226, ~:691-700)

**Interfaces:**
- Consumes: `validateAndEnrich(..., boolean repick)` (Task 2). Dữ liệu: `ai-service/src/test/resources/mealplan/recipes-seed.json` (115 món, xuất từ recipe-service ở plan dữ liệu).

- [ ] **Step 1: Viết test chấp nhận**

```java
package com.veggiepal.ai.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Runs the planner on the real seed catalogue (recipes-seed.json, exported from recipe-service's
// GET /recipes?size=200). Re-export it whenever recipe-service's data.sql changes — see
// docs/superpowers/plans/2026-10-21-recipe-nutrition-data.md, Task 6 Step 9.
class MealPlanSeedAcceptanceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MealPlanValidator validator = new MealPlanValidator(mapper);

    @ParameterizedTest(name = "{0}-{1} kcal")
    @CsvSource({"1350,1650", "1800,2200", "2250,2750", "2700,3300", "2522,3082"})
    void everyDayMeetsTheCalorieTargetOnTheSeedCatalogue(int min, int max) throws Exception {
        JsonNode recipes;
        try (InputStream in = getClass().getResourceAsStream("/mealplan/recipes-seed.json")) {
            recipes = mapper.readTree(in);
        }
        ObjectNode plan = mapper.createObjectNode(); // what generate() validates when the AI call fails
        MealPlanDtos.NutritionTarget target = new MealPlanDtos.NutritionTarget(
                BigDecimal.valueOf(min), BigDecimal.valueOf(max), null, null, null, null, null, null);

        validator.validateAndEnrich(plan, recipes, mapper.createArrayNode(), target, true);

        assertEquals(7, plan.path("days").size());
        for (JsonNode day : plan.path("days")) {
            assertTrue(day.path("targetMet").asBoolean(false),
                    day.path("day").asText() + " totals " + day.path("nutritionTotals"));
        }
    }
}
```

Bốn mức ±10% quanh 1.500 / 2.000 / 2.500 / 3.000 kcal, và ca 2.522–3.082 trong tài liệu gốc.

- [ ] **Step 2: Chạy test chấp nhận**

```bash
cd ai-service && ./mvnw test -Dtest=MealPlanSeedAcceptanceTest
```

Kỳ vọng: 5/5 PASS.
- Bảng khả thi của seed có ít nhất 11 món chạm được ngân sách mỗi bữa ở mọi mức, nên ngày nào cũng có món đạt.
- **Nếu một ca fail:** đó là phát hiện thật về dữ liệu hoặc thuật toán. Dừng lại và báo, kèm `nutritionTotals` của ngày fail. Không sửa test, không sửa `recipes-seed.json`.

- [ ] **Step 3: Kiểm tra test bắt được lỗi**

Tạm đổi `"0.40"` của `LUNCH` trong `MEAL_SHARES` thành `"0.10"`, rồi chạy lại Step 2. Kỳ vọng: có ca FAIL (ngân sách bữa trưa quá nhỏ làm bữa tối phải gánh quá giới hạn 2,0). Khôi phục `"0.40"`, chạy lại, kỳ vọng PASS. Ghi cả hai kết quả vào report.

- [ ] **Step 4: Cập nhật `docs/AI_SERVICE_DOCUMENTATION.md`**

- **Dòng ~225** (`Có thể truyền dailyNutritionTarget; từng ngày sai min/max … sẽ bị từ chối.`) thay bằng:
  `- Có thể truyền dailyNutritionTarget. Mỗi bữa có ngân sách calo riêng (sáng 25%, trưa 40%, tối 35% của mục tiêu giữa, tính lần lượt để bữa sau gánh phần bữa trước thiếu); món được chọn theo ngân sách và khẩu phần nhân riêng từng bữa trong khoảng 0,75–2,0 (servingMultiplier). Ngày lệch mục tiêu chỉ bị đánh dấu, không bị từ chối.`
- **Dòng ~226** (`Thay món …`) thêm vào cuối câu:
  `Món thay còn phải khác các món còn lại trong ngày và chạm được ngân sách của bữa đó; các bữa khác giữ nguyên món.`
- **Đoạn ~691–700:**
  - Câu mở đầu thay bằng: "Khi `dailyNutritionTarget` được cung cấp, backend tính tổng dinh dưỡng của từng ngày (`nutritionTotals`) và trả về:"
  - `targetMet` mô tả là: `true` khi calo trong ngày nằm trong `[minCalories, maxCalories]`, không có dung sai; đạm, carb và béo không quyết định `targetMet`.
  - `targetDeviations` mô tả là: liệt kê mọi chỉ số nằm ngoài `[min, max]`, kể cả khi `targetMet` là `true`.
  - Xóa mọi nhắc tới `ai.meal-plan.target-tolerance-ratio`.
- **Không sửa** `docs/PHASE2_CHANGES_SUMMARY.md`: đó là tài liệu lịch sử của một đợt trước.

- [ ] **Step 5: Chạy toàn bộ test của ai-service**

```bash
cd ai-service && ./mvnw test
```

Kỳ vọng: BUILD SUCCESS. Test của ai-service dùng H2 cấu hình trong `src/test/resources`. Nếu một test lỗi vì môi trường (không phải do thay đổi này), ghi tên test và lỗi vào report thay vì sửa.

- [ ] **Step 6: Dừng, bàn giao cho Long review**

Báo cho Long: các file đã đổi, kết quả 5 ca của test chấp nhận, kết quả kiểm tra test bắt được lỗi, và số test PASS của toàn bộ ai-service.
