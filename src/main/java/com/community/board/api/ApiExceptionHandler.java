package com.community.board.api;

import com.community.board.member.service.DuplicateNicknameException;
import com.community.board.member.service.MemberSignupConflictException;
import com.community.board.member.service.MemberNotFoundException;
import com.community.board.integration.tmdb.exception.MovieClientAuthenticationException;
import com.community.board.integration.tmdb.exception.MovieClientCommunicationException;
import com.community.board.integration.tmdb.exception.MovieClientException;
import com.community.board.integration.tmdb.exception.MovieClientUnavailableException;
import com.community.board.integration.tmdb.exception.MovieNotFoundException;
import com.community.board.review.service.DuplicateReviewException;
import com.community.board.review.domain.InvalidReviewRatingException;
import com.community.board.review.domain.InvalidReviewUpdateException;
import com.community.board.review.service.ReviewNotFoundException;
import com.community.board.review.service.ReviewOwnershipException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DuplicateNicknameException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateNickname(DuplicateNicknameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse("DUPLICATE_NICKNAME", exception.getMessage())
        );
    }

    @ExceptionHandler(MemberSignupConflictException.class)
    ResponseEntity<ApiErrorResponse> handleMemberSignupConflict(MemberSignupConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse("MEMBER_SIGNUP_CONFLICT", exception.getMessage())
        );
    }

    @ExceptionHandler(MemberNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleMemberNotFound(MemberNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiErrorResponse("MEMBER_NOT_FOUND", exception.getMessage())
        );
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleReviewNotFound(ReviewNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(MovieNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleMovieNotFound(MovieNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", "영화를 찾을 수 없습니다.");
    }

    @ExceptionHandler(DuplicateReviewException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateReview(DuplicateReviewException exception) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_REVIEW", exception.getMessage());
    }

    @ExceptionHandler(ReviewOwnershipException.class)
    ResponseEntity<ApiErrorResponse> handleReviewOwnership(ReviewOwnershipException exception) {
        return error(HttpStatus.FORBIDDEN, "REVIEW_FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(InvalidReviewRatingException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidReviewRating(InvalidReviewRatingException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REVIEW_RATING", exception.getMessage());
    }

    @ExceptionHandler(InvalidReviewUpdateException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidReviewUpdate(InvalidReviewUpdateException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REVIEW_UPDATE", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다.");
    }

    @ExceptionHandler({MovieClientUnavailableException.class, MovieClientCommunicationException.class})
    ResponseEntity<ApiErrorResponse> handleMovieProviderUnavailable(MovieClientException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "MOVIE_PROVIDER_UNAVAILABLE", "영화 정보 서비스를 사용할 수 없습니다.");
    }

    @ExceptionHandler({MovieClientAuthenticationException.class, MovieClientException.class})
    ResponseEntity<ApiErrorResponse> handleMovieProviderFailure(MovieClientException exception) {
        return error(HttpStatus.BAD_GATEWAY, "MOVIE_PROVIDER_ERROR", "영화 정보를 가져오지 못했습니다.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
