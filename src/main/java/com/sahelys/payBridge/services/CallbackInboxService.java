package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.dto.ProviderCallbackResult;
import com.sahelys.payBridge.domain.entities.OperatorCallback;
import com.sahelys.payBridge.domain.enums.ECorrelationOutcome;
import com.sahelys.payBridge.domain.enums.EOperatorCallbackProcessingStatus;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.provider.CallbackParser;
import com.sahelys.payBridge.provider.CallbackParserRegistry;
import com.sahelys.payBridge.repository.OperatorCallbackRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Durable callback processor: persists every operator callback verbatim before any attempt
 * to correlate or act on it -- see docs/moov-money-async-architecture.md. The initial _storeReceivedResponse
 * and the later parse/correlate step are two separate repository calls (Spring Data commits
 * each independently; neither is wrapped in an explicit @Transactional here) precisely so
 * the raw receipt stays durable even if parsing or correlation fails.
 *
 * <p>Which {@link CallbackParser} runs is decided by {@code CallbackInboxController} -- whichever
 * endpoint received the request already knows which operator it's for.
 *
 * <p>Errors are always made visible: {@code _processReceivedResponse} records every failure
 * (malformed payload, unknown transaction, or a genuinely unexpected error) as a terminal
 * {@code FAILED} row with the reason attached -- nothing is ever silently stuck at
 * {@code RECEIVED} with no explanation. Duplicate-delivery and already-terminal handling
 * are still commented out in {@link PaymentCallbackCorrelationService} (a different,
 * still-deliberate MVP simplification); reinstate those separately if needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackInboxService {

    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie", "set-cookie", "x-api-key");

    private final OperatorCallbackRepository        operatorCallbackRepository;
    private final PaymentCallbackCorrelationService correlationService;
    private final CallbackParserRegistry            parser;

    public void receive(EPaymentOperator operator, HttpServletRequest request, String rawBody) {
        OperatorCallback callback = _storeReceivedResponse(operator, request, rawBody);
        _processReceivedResponse(operator, callback);
    }

    private OperatorCallback _storeReceivedResponse(EPaymentOperator paymentOperator, HttpServletRequest request, String rawBody) {
        OperatorCallback callback = OperatorCallback.builder()
                                                    .id(UUID.randomUUID())
                                                    .operatorCode(paymentOperator.name())
                                                    .requestMethod(request.getMethod())
                                                    .requestPath(request.getRequestURI())
                                                    .contentType(request.getContentType())
                                                    .sourceIp(request.getRemoteAddr())
                                                    .requestHeaders(sanitizedHeaders(request))
                                                    .rawPayload(rawBody)
                                                    .payloadSha256(sha256Hex(rawBody))
                                                    .processingStatus(EOperatorCallbackProcessingStatus.RECEIVED)
                                                    .build();

        OperatorCallback saved = operatorCallbackRepository.save(callback);
        log.info("Stored paymentOperator callback {} ({} bytes) from {}", saved.getId(), rawBody.length(), paymentOperator);
        return saved;
    }

    private void _processReceivedResponse(EPaymentOperator operator, OperatorCallback callback) {
        try {
            ProviderCallbackResult callbackResult = parser.parse(operator, callback.getRawPayload());

            ECorrelationOutcome outcome = correlationService.correlate(callbackResult);
            callback.setProcessingStatus(EOperatorCallbackProcessingStatus.PROCESSED);
            log.info("Processed operator callback {} -> {}", callback.getId(), outcome);
        } catch (CustomException ex) {
            // Expected failure: malformed payload, unknown transaction, illegal transition --
            // the message alone is the useful diagnostic, no stack trace needed.
            log.error("Failed to process operator callback {}: {}", callback.getId(), ex.getMessage());
            callback.setProcessingStatus(EOperatorCallbackProcessingStatus.FAILED);
            callback.setProcessingError(ex.getMessage());
        } catch (Exception ex) {
            // Unexpected failure (bug, DB hiccup, anything not deliberately thrown). Without
            // this branch the row would stay stuck at RECEIVED forever with no error recorded
            // and no trail -- exactly the "malformed/unknown callback silently disappears"
            // failure mode the durable-inbox design exists to prevent. The raw payload is
            // already durably stored (see _storeReceivedResponse()); this just makes sure we
            // can never lose track of what happened to it.
            log.error("Unexpected error processing operator callback {}", callback.getId(), ex);
            callback.setProcessingStatus(EOperatorCallbackProcessingStatus.FAILED);
            callback.setProcessingError(ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
        callback.setProcessedAt(OffsetDateTime.now());
        operatorCallbackRepository.save(callback);
    }

    private Map<String, String> sanitizedHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return headers;
        }
        Collections.list(names).forEach(name -> {
            boolean sensitive = SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT));
            headers.put(name, sensitive ? "[REDACTED]" : request.getHeader(name));
        });
        return headers;
    }

    private String sha256Hex(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
