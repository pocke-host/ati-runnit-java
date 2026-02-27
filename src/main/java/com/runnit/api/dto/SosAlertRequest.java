package com.runnit.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SosAlertRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String message;
}
