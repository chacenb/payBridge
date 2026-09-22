package com.sahelys.payBridge.globals.exceptions;

import com.sahelys.payBridge.domain.dto.WsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override /* default handler for ALL EXCEPTIONS */
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.error("_____[GLOBAL EXCEPTION HANDLER : Internal Exception] >> ", ex);
        return ResponseEntity.badRequest().body(WsResponse.builder()
                                                          .timeStamp(ZonedDateTime.now())
                                                          .status(HttpStatus.valueOf(statusCode.value()))
                                                          .message(ex.getMessage())
                                                          .error(ex)
                                                          .build());
    }

    @Override /* Handles exceptions related to invalid method arguments */
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("_____[GLOBAL EXCEPTION HANDLER : Method Argument Not Valid] >>", exception);

        /* get all the errors messages for clean returning */
        List<String> errorList = exception.getBindingResult().getFieldErrors().stream().map(fieldError -> fieldError.getDefaultMessage()).toList();

        return ResponseEntity.badRequest().body(WsResponse.builder()
                                                          .timeStamp(ZonedDateTime.now())
                                                          .status(HttpStatus.valueOf(status.value()))
                                                          .message(errorList.toString())
                                                          .build());
    }


    @Override /* This handler ensures JSON parsing/deserialization issues are caught at the entry point of the APIs */
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.error("_____[GLOBAL EXCEPTION HANDLER : Http Message Not Readable] >> Invalid JSON / Cannot map to request object", ex);

        String message = "Invalid request body. Please check the JSON format, the field types and field values.";

        return ResponseEntity.badRequest().body(WsResponse.builder()
                                                          .timeStamp(java.time.ZonedDateTime.now())
                                                          .status(HttpStatus.BAD_REQUEST)
                                                          .message(message)
                                                          .build());
    }


    @ExceptionHandler({CustomException.class,}) /* handler for FTA's CustomException */
    public ResponseEntity<WsResponse<?>> customExceptionHandler(CustomException ex) {
        log.error("_____[GLOBAL EXCEPTION HANDLER : FTA Custom Exception] >> code = {}", ex.getCode(), ex);
        return ResponseEntity.badRequest().body(WsResponse.builder()
                                                          .timeStamp(ZonedDateTime.now())
                                                          .status(HttpStatus.BAD_REQUEST)
                                                          .message(ex.getMessage())
//                                                         .error(ex)
                                                          .errorCode(ex.getCode().getLabel())
                                                          .build());
    }

    @ExceptionHandler({Exception.class, NullPointerException.class,})
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        log.error("_____[GLOBAL EXCEPTION HANDLER : Generic Exception] >>", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(WsResponse.builder()
                                                                                      .timeStamp(ZonedDateTime.now())
                                                                                      .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                                                      .message(ex.getMessage())
                                                                                      .build());
    }

    /* Custom Exception handler for unique DB fields */
  /* Note: the default "handleExceptionInternal" method is only designed to handle validation-related exceptions
  as MethodArgumentNotValidException, MissingServletRequestPartException, etc.. Validation-related ONLY
  and not database exceptions (ex: DataIntegrityViolationException) -- without this handler, two
  concurrent requests racing past an idempotency check (e.g. ClientPaymentRequestService,
  check-then-act against uq_client_payment_requests_client_request) surface the second
  insert's unique-constraint violation as a raw, leaking 500 instead of a clean 400. */
    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<WsResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("_____[GLOBAL EXCEPTION HANDLER : Data Integrity Violation] >>", ex);

        return ResponseEntity.badRequest().body(WsResponse.builder()
                                                          .timeStamp(ZonedDateTime.now())
                                                          .status(HttpStatus.BAD_REQUEST)
                                                          .message("Request conflicts with an existing record.")
                                                          .errorCode(EExceptionCode.DATA_INCOHERENCE.getLabel())
                                                          .build());
    }

}
