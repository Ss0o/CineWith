package com.community.board.review.controller;

import com.community.board.review.service.RatingStatisticsService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/movies")
public class RatingStatisticsController {

    private final RatingStatisticsService ratingStatisticsService;

    public RatingStatisticsController(RatingStatisticsService ratingStatisticsService) {
        this.ratingStatisticsService = ratingStatisticsService;
    }

    @GetMapping("/{tmdbId}/rating-statistics")
    public RatingStatisticsResponse getByMovie(@PathVariable @Positive Long tmdbId) {
        return RatingStatisticsResponse.from(ratingStatisticsService.getByMovie(tmdbId));
    }
}
