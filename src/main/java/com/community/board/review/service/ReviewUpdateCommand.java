package com.community.board.review.service;

import java.math.BigDecimal;

public record ReviewUpdateCommand(
        boolean titlePresent,
        String title,
        boolean contentPresent,
        String content,
        boolean ratingPresent,
        BigDecimal rating
) {

    public boolean isEmpty() {
        return !titlePresent && !contentPresent && !ratingPresent;
    }
}
