package com.community.board.api;

import com.community.board.member.service.DuplicateNicknameException;
import com.community.board.member.service.MemberSignupConflictException;
import com.community.board.member.service.MemberNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
