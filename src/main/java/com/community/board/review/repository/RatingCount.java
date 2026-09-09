package com.community.board.review.repository;

import java.math.BigDecimal;

public interface RatingCount {

    BigDecimal getRating();

    long getReviewCount();
}
