package com.projectx.kotlinlab.presentation

import com.projectx.kotlinlab.domain.LabError
import com.projectx.kotlinlab.domain.LabException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class LabExceptionHandler {
    @ExceptionHandler(LabException::class)
    fun domain(exception: LabException): ResponseEntity<ApiError> {
        val status = when (exception.error) {
            LabError.INVALID_INPUT -> HttpStatus.BAD_REQUEST
            LabError.ITEM_NOT_FOUND -> HttpStatus.NOT_FOUND
            LabError.VERSION_CONFLICT -> HttpStatus.CONFLICT
            LabError.DEMO_ROLLBACK -> HttpStatus.UNPROCESSABLE_ENTITY
        }
        return ResponseEntity.status(status).body(ApiError(exception.error.name, exception.message ?: "요청을 처리할 수 없습니다."))
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun optimisticConflict(): ResponseEntity<ApiError> = ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError("VERSION_CONFLICT", "다른 요청이 먼저 수정했습니다. 다시 조회해 주세요."))

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun invalidInput(): ResponseEntity<ApiError> = ResponseEntity.badRequest()
        .body(ApiError("INVALID_INPUT", "요청 값의 형식 또는 필수값을 확인해 주세요."))
}
