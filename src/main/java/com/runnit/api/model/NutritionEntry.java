package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_entries")
public class NutritionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @Column(name = "meal_type", nullable = false, length = 30)
    private String mealType;

    @Column(name = "food_name", nullable = false, length = 255)
    private String foodName;

    @Column(name = "serving_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal servingQuantity = BigDecimal.ONE;

    @Column(name = "serving_unit", length = 50)
    private String servingUnit;

    @Column(name = "calories_kcal")
    private Integer caloriesKcal;

    @Column(name = "protein_grams", precision = 8, scale = 2)
    private BigDecimal proteinGrams;

    @Column(name = "carbs_grams", precision = 8, scale = 2)
    private BigDecimal carbsGrams;

    @Column(name = "fat_grams", precision = 8, scale = 2)
    private BigDecimal fatGrams;

    @Column(name = "fiber_grams", precision = 8, scale = 2)
    private BigDecimal fiberGrams;

    @Column(name = "source", nullable = false, length = 30)
    private String source = "MANUAL";

    @Column(name = "external_food_id", length = 100)
    private String externalFoodId;

    @Column(name = "barcode", length = 64)
    private String barcode;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NutritionEntry() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public String getMealType() { return mealType; }
    public String getFoodName() { return foodName; }
    public BigDecimal getServingQuantity() { return servingQuantity; }
    public String getServingUnit() { return servingUnit; }
    public Integer getCaloriesKcal() { return caloriesKcal; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getCarbsGrams() { return carbsGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public BigDecimal getFiberGrams() { return fiberGrams; }
    public String getSource() { return source; }
    public String getExternalFoodId() { return externalFoodId; }
    public String getBarcode() { return barcode; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public void setServingQuantity(BigDecimal servingQuantity) { this.servingQuantity = servingQuantity; }
    public void setServingUnit(String servingUnit) { this.servingUnit = servingUnit; }
    public void setCaloriesKcal(Integer caloriesKcal) { this.caloriesKcal = caloriesKcal; }
    public void setProteinGrams(BigDecimal proteinGrams) { this.proteinGrams = proteinGrams; }
    public void setCarbsGrams(BigDecimal carbsGrams) { this.carbsGrams = carbsGrams; }
    public void setFatGrams(BigDecimal fatGrams) { this.fatGrams = fatGrams; }
    public void setFiberGrams(BigDecimal fiberGrams) { this.fiberGrams = fiberGrams; }
    public void setSource(String source) { this.source = source; }
    public void setExternalFoodId(String externalFoodId) { this.externalFoodId = externalFoodId; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public void setNotes(String notes) { this.notes = notes; }
}
