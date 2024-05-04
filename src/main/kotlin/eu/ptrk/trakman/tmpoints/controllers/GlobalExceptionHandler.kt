package eu.ptrk.trakman.tmpoints.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException::class)
    fun handle(e: ResponseStatusException): ResponseEntity<String> {
        return ResponseEntity(e.reason, e.statusCode)
    }
}