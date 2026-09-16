package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.entities.OperatorCallback;
import com.sahelys.payBridge.repository.OperatorCallbackRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Durable callback processor: persists every Moov Money callback verbatim before any attempt to
 * correlate or act on it -- see docs/moov-money-async-architecture.md. Correlating a stored
 * callback to a payment is a separate, later concern once a real callback payload has been
 * captured.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackInboxService {

    private static final String OPERATOR_CODE = "MOOV_MONEY";
    private static final Set<String> SENSITIVE_HEADERS =
            Set.of("authorization", "cookie", "set-cookie", "x-api-key");

    private final OperatorCallbackRepository repository;

    public void receive(HttpServletRequest request, String rawBody) {
        OperatorCallback callback = OperatorCallback.builder()
                .id(UUID.randomUUID())
                .operatorCode(OPERATOR_CODE)
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .contentType(request.getContentType())
                .sourceIp(request.getRemoteAddr())
                .requestHeaders(sanitizedHeaders(request))
                .rawPayload(rawBody)
                .payloadSha256(sha256Hex(rawBody))
                .processingStatus("RECEIVED")
                .build();

        repository.save(callback);
        log.info("Stored operator callback {} ({} bytes)", callback.getId(), rawBody.length());
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
