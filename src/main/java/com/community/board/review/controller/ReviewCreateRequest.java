package com.community.board.review.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ReviewCreateRequest(
        @NotNull @Positive Long tmdbId,
        @NotBlank String title,
        @NotBlank String content,
        @NotNull @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal rating
) {
}
