package com.sahelys.payBridge.domain.dto;

import com.sahelys.payBridge.domain.enums.EProviderPaymentResultCode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * What a provider-specific callback parser must produce before correlation can run.
 * Deliberately provider-agnostic: nothing here is Moov/Huawei-specific.
 *
 * <p>Confirmed against the real Moov captures in _MATERIALS: we mint {@code
 * OriginatorConversationID} ourselves before ever calling the provider (see
 * MoovMoneyXmlRequestBuilder), and Moov echoes it back unchanged in both the immediate sync
 * ack and the later async result. That means {@link #paymentTransactionId} -- our own
 * correlation handle -- is known and reliable even if the sync ack itself was lost, and is
 * therefore the correlation key (MVP simplification: no separate PaymentAttempt id anymore).
 * {@link #providerPaymentTransactionId} (Moov's own {@code ConversationID}, only known once
 * the sync ack is received) is kept only as a secondary reference for reconciliation.
 *
 * <p>The real Moov XML -> ProviderCallbackResult parser (extracting these fields from the
 * captured envelope shape) is PM-06 work, not yet built.
 */
@Getter @Setter @ToString @Builder
public class ProviderCallbackResult {
    private UUID                       paymentTransactionId;
    private String                     providerPaymentTransactionId;
    private EProviderPaymentResultCode providerPaymentResultCode;
    private String                     message;
}
