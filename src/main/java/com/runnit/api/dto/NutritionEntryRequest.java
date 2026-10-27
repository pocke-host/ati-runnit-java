package com.runnit.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NutritionEntryRequest {

    @NotNull
    private LocalDateTime loggedAt;

    @NotBlank
    @Size(max = 30)
    private String mealType;

    @NotBlank
    @Size(max = 255)
    private String foodName;

    @DecimalMin(value = "0.01")
    private BigDecimal servingQuantity = BigDecimal.ONE;

    @Size(max = 50)
    private String servingUnit;

    @Max(100000)
    private Integer caloriesKcal;

    @DecimalMin(value = "0.0")
    private BigDecimal proteinGrams;

    @DecimalMin(value = "0.0")
    private BigDecimal carbsGrams;

    @DecimalMin(value = "0.0")
    private BigDecimal fatGrams;

    @DecimalMin(value = "0.0")
    private BigDecimal fiberGrams;

    @Size(max = 30)
    private String source;

    @Size(max = 100)
    private String externalFoodId;

    @Size(max = 64)
    private String barcode;

    @Size(max = 500)
    private String notes;

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
