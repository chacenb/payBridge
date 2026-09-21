package com.sahelys.payBridge.domain.entities;

import com.sahelys.payBridge.domain.enums.EOperatorCallbackProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "operator_callbacks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorCallback {

    @Id
    private UUID id;

    /**
     * Deliberately a plain String, not {@code EPaymentOperator} -- this row's whole job is
     * to durably capture a raw inbound callback verbatim, even a malformed or unrecognized
     * one. An enum-typed column would make that capture fail exactly when it matters most
     * (an unexpected operator/value hitting the endpoint), which defeats the point of a
     * durable inbox. In practice it's always a valid {@code EPaymentOperator.name()} today,
     * since routing is fixed per endpoint -- but the column stays untyped on purpose.
     */
    @Column(name = "operator_code", nullable = false)
    private String operatorCode;

    @Column(name = "received_at", insertable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "request_method", nullable = false)
    private String requestMethod;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "source_ip")
    private String sourceIp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", nullable = false)
    private Map<String, String> requestHeaders;

    @Column(name = "raw_payload", nullable = false)
    private String rawPayload;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private EOperatorCallbackProcessingStatus processingStatus;

    @Column(name = "processing_error")
    private String processingError;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;
}
