package com.community.board.review.controller;

import com.community.board.review.service.ReviewUpdateCommand;

import java.math.BigDecimal;

public final class ReviewUpdateRequest {

    private boolean titlePresent;
    private String title;
    private boolean contentPresent;
    private String content;
    private boolean ratingPresent;
    private BigDecimal rating;

    public ReviewUpdateRequest() {
    }

    public void setTitle(String title) {
        this.titlePresent = true;
        this.title = title;
    }

    public void setContent(String content) {
        this.contentPresent = true;
        this.content = content;
    }

    public void setRating(BigDecimal rating) {
        this.ratingPresent = true;
        this.rating = rating;
    }

    public ReviewUpdateCommand toCommand() {
        return new ReviewUpdateCommand(
                titlePresent,
                title,
                contentPresent,
                content,
                ratingPresent,
                rating
        );
    }
}
