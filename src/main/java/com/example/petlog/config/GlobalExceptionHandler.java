package com.example.petlog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 전역 예외 처리기 - 모든 예외를 잡아서 로그와 상세 응답 반환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception e) {
        log.error("=== 전역 예외 발생 ===");
        log.error("예외 타입: {}", e.getClass().getName());
        log.error("예외 메시지: {}", e.getMessage());
        log.error("스택 트레이스:", e);

        return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "type", e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "No message",
                "timestamp", System.currentTimeMillis()));
    }
}
