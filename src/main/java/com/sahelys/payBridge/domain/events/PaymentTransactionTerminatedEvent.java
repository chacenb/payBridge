package com.sahelys.payBridge.domain.events;

import com.sahelys.payBridge.domain.entities.PaymentTransaction;

/**
 * Published by {@link com.sahelys.payBridge.services.PaymentTransactionService} when a
 * PaymentTransaction is saved in a terminal status -- picked up after commit to notify the
 * client app.
 */
public record PaymentTransactionTerminatedEvent(PaymentTransaction transaction) {
}
