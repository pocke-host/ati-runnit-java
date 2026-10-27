package com.runnit.api.controller;

import com.runnit.api.dto.NutritionEntryRequest;
import com.runnit.api.model.NutritionEntry;
import com.runnit.api.repository.NutritionEntryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/nutrition")
@RequiredArgsConstructor
public class NutritionController {

    private final NutritionEntryRepository nutritionEntryRepository;

    @GetMapping
    public ResponseEntity<?> getEntries(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "UTC") String timezone,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            ZoneId zone = safeZone(timezone);
            LocalDate requestedDate = date == null || date.isBlank()
                    ? LocalDate.now(zone)
                    : LocalDate.parse(date);
            LocalDateTime start = requestedDate.atStartOfDay();
            LocalDateTime end = requestedDate.plusDays(1).atStartOfDay();
            List<NutritionEntry> entries = nutritionEntryRepository
                    .findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(userId, start, end);
            return ResponseEntity.ok(response(requestedDate, entries));
        } catch (DateTimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "date must be YYYY-MM-DD and timezone must be valid"));
        } catch (Exception e) {
            log.error("Nutrition read failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Couldn't load nutrition entries"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createEntry(
            @Valid @RequestBody NutritionEntryRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            NutritionEntry entry = new NutritionEntry();
            entry.setUserId(userId);
            entry.setLoggedAt(request.getLoggedAt());
            entry.setMealType(normalize(request.getMealType(), 30, "mealType"));
            entry.setFoodName(normalize(request.getFoodName(), 255, "foodName"));
            entry.setServingQuantity(request.getServingQuantity() == null ? BigDecimal.ONE : request.getServingQuantity());
            entry.setServingUnit(normalizeNullable(request.getServingUnit(), 50));
            entry.setCaloriesKcal(request.getCaloriesKcal());
            entry.setProteinGrams(request.getProteinGrams());
            entry.setCarbsGrams(request.getCarbsGrams());
            entry.setFatGrams(request.getFatGrams());
            entry.setFiberGrams(request.getFiberGrams());
            entry.setSource(normalizeSource(request.getSource()));
            entry.setExternalFoodId(normalizeNullable(request.getExternalFoodId(), 100));
            entry.setBarcode(normalizeNullable(request.getBarcode(), 64));
            entry.setNotes(normalizeNullable(request.getNotes(), 500));
            return ResponseEntity.ok(toMap(nutritionEntryRepository.save(entry)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Nutrition create failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Couldn't save this food"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return nutritionEntryRepository.findByIdAndUserId(id, userId)
                .map(entry -> {
                    nutritionEntryRepository.delete(entry);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> response(LocalDate date, List<NutritionEntry> entries) {
        int calories = entries.stream().map(NutritionEntry::getCaloriesKcal).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("caloriesKcal", calories);
        totals.put("proteinGrams", decimalTotal(entries, NutritionEntry::getProteinGrams));
        totals.put("carbsGrams", decimalTotal(entries, NutritionEntry::getCarbsGrams));
        totals.put("fatGrams", decimalTotal(entries, NutritionEntry::getFatGrams));
        totals.put("fiberGrams", decimalTotal(entries, NutritionEntry::getFiberGrams));
        return Map.of("date", date.toString(), "entries", entries.stream().map(this::toMap).toList(), "totals", totals);
    }

    private BigDecimal decimalTotal(List<NutritionEntry> entries, java.util.function.Function<NutritionEntry, BigDecimal> getter) {
        return entries.stream().map(getter).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> toMap(NutritionEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entry.getId());
        map.put("loggedAt", entry.getLoggedAt());
        map.put("mealType", entry.getMealType());
        map.put("foodName", entry.getFoodName());
        map.put("servingQuantity", entry.getServingQuantity());
        map.put("servingUnit", entry.getServingUnit());
        map.put("caloriesKcal", entry.getCaloriesKcal());
        map.put("proteinGrams", entry.getProteinGrams());
        map.put("carbsGrams", entry.getCarbsGrams());
        map.put("fatGrams", entry.getFatGrams());
        map.put("fiberGrams", entry.getFiberGrams());
        map.put("source", entry.getSource());
        map.put("externalFoodId", entry.getExternalFoodId());
        map.put("barcode", entry.getBarcode());
        map.put("notes", entry.getNotes());
        return map;
    }

    private ZoneId safeZone(String timezone) {
        try { return ZoneId.of(timezone); } catch (DateTimeException e) { return ZoneId.of("UTC"); }
    }

    private String normalize(String value, int max, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        if (normalized.length() > max) throw new IllegalArgumentException(field + " is too long");
        return normalized;
    }

    private String normalizeNullable(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    private String normalizeSource(String value) {
        String source = value == null || value.isBlank() ? "MANUAL" : value.trim().toUpperCase();
        return switch (source) {
            case "MANUAL", "USDA", "MYFITNESSPAL", "APPLE_HEALTH" -> source;
            default -> "MANUAL";
        };
    }
}
