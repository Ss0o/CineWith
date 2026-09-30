package com.community.board.recommendation.service;

public class DuplicateRecommendationException extends RuntimeException {
    public DuplicateRecommendationException() { super("이미 추천한 콘텐츠입니다."); }
    public DuplicateRecommendationException(Throwable cause) { super("이미 추천한 콘텐츠입니다.", cause); }
}
