package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.services.CallbackInboxService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Each operator gets its own callback path (its own registered ResultURL in reality), so
 * which {@link com.sahelys.payBridge.provider.CallbackParser} to use is already known here,
 * explicitly, per endpoint -- no dynamic lookup needed.
 */
@RestController
@RequiredArgsConstructor
public class CallbackInboxController {

    private final CallbackInboxService callbackInboxService;

    @PostMapping("${paybridge.callback.path}")
    public ResponseEntity<Void> receiveMoovCallback(HttpServletRequest request) throws IOException {
        callbackInboxService.receive(EPaymentOperator.MOOV_MONEY, request, _readBody(request));
        return ResponseEntity.ok().build();
    }

    private String _readBody(HttpServletRequest request) throws IOException {
        return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
