package com.community.board.review.service;

import com.community.board.review.repository.RatingCount;
import com.community.board.review.repository.RatingSummary;
import com.community.board.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RatingStatisticsService {

    private final ReviewRepository reviewRepository;

    public RatingStatisticsService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    // Both aggregate queries must observe the same committed review snapshot.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RatingStatisticsView getByMovie(Long tmdbId) {
        RatingSummary summary = reviewRepository.summarizeRatingsByTmdbId(tmdbId);
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int step = 1; step <= 10; step++) {
            distribution.put(BigDecimal.valueOf(step * 5L, 1).toPlainString(), 0L);
        }
        for (RatingCount bucket : reviewRepository.countRatingsByTmdbId(tmdbId)) {
            distribution.put(bucket.getRating().setScale(1).toPlainString(), bucket.getReviewCount());
        }

        // JPQL AVG returns Double even when the persisted field is BigDecimal.
        BigDecimal average = summary.getAverageRating() == null ? null
                : BigDecimal.valueOf(summary.getAverageRating()).setScale(2, RoundingMode.HALF_UP);
        return new RatingStatisticsView(
                average, summary.getReviewCount(), Collections.unmodifiableMap(distribution)
        );
    }
}
