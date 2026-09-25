package com.learning.miniblog.exception;

// HttpStatus — enum of HTTP status codes (BAD_REQUEST = 400, NOT_FOUND = 404, etc.)
import org.springframework.http.HttpStatus;

// ResponseEntity — a wrapper that lets you control BOTH the HTTP status AND the response body.
// In a @RestController, returning a plain object gives 200 OK by default.
// ResponseEntity lets you override the status and headers.
import org.springframework.http.ResponseEntity;

// MethodArgumentNotValidException — thrown by Spring when @Valid fails on a @RequestBody parameter.
// Contains details about every field that failed validation.
import org.springframework.web.bind.MethodArgumentNotValidException;

// @ExceptionHandler — "when this exception is thrown anywhere in the app, run this method"
import org.springframework.web.bind.annotation.ExceptionHandler;

// @RestControllerAdvice — "global exception handler for all controllers"
// = @ControllerAdvice (a @Component that advises all controllers)
// + @ResponseBody (method return values become JSON, not view names)
import org.springframework.web.bind.annotation.RestControllerAdvice;

// HashMap — a Map implementation. Used to build JSON objects {key: value}.
import java.util.HashMap;

// Map — key-value collection. Here used to build JSON like {"title": "Title is required"}.
import java.util.Map;

// @RestControllerAdvice — registers this class as a global exception handler.
// Spring scans it at startup and uses it to intercept exceptions from ANY @RestController.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------------------
    // Handler 1 — Validation errors (from @Valid on @RequestBody)
    // ---------------------------------------------------------------------
    //
    // What triggers this?
    //   When a controller method has `@Valid @RequestBody PostRequest request`
    //   and one of the DTO's annotations (e.g., @NotBlank) fails,
    //   Spring throws MethodArgumentNotValidException.
    //
    // Without this handler: Spring returns its ugly default 400 error.
    // With this handler: we return a clean JSON with field-level messages.
    //
    // @ExceptionHandler(X.class) — "when X is thrown, run THIS method"

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Build a Map (will become JSON) to hold "fieldName" -> "error message".
        // Example: {"title": "Title is required", "author": "Author is required"}
        Map<String, String> errors = new HashMap<>();

        // ex.getBindingResult() — the result of running validation on the DTO.
        // .getFieldErrors()     — list of FieldError objects, one per failed field.
        //   Each FieldError has:
        //     getField()          → the field name        ("title")
        //     getDefaultMessage() → the message string    ("Title is required")
        //
        // .forEach(...) — a lambda that runs once per FieldError.
        //   `fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage())`
        //   is shorthand for:
        //     for (FieldError fieldError : fieldErrors) {
        //         errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        //     }
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );

        /*
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        
        for(FieldError fieldError : fieldErrors){
            String fieldName = fieldError.getField();
            String message = fieldError.getDefaultMessage();
            errors.put(fieldName, message);
        }
        */

        // ResponseEntity.status(...) — set the HTTP status code explicitly
        //                            .body(...) — attach a response body (the Map → JSON)
        // Result: HTTP 400 with {"title": "Title is required"}
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // ---------------------------------------------------------------------
    // Handler 2 — Resource not found (thrown by our service)
    // ---------------------------------------------------------------------
    //
    // What triggers this?
    //   When the service calls `orElseThrow(() -> new ResourceNotFoundException(...))`
    //   and the entity doesn't exist in the DB.
    //
    // @ExceptionHandler(ResourceNotFoundException.class) — catch OUR custom exception

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        // Build a Map to hold {"error": "Post not found with id: 9999"}
        Map<String, String> error = new HashMap<>();

        // ex.getMessage() — returns the message stored by super(message) in
        // ResourceNotFoundException. That's WHY we called super(message) earlier!
        error.put("error", ex.getMessage());

        // Result: HTTP 404 with {"error": "Post not found with id: 9999"}
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
