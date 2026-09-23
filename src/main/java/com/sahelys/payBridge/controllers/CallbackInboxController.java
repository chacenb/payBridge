package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.services.CallbackInboxService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static com.sahelys.payBridge.globals.utils.Utils.readBody;

/**
 * Each operator gets its own callback path (its own registered ResultURL in reality), so
 * which {@link com.sahelys.payBridge.provider.ProviderXmlParser} parsing method to use is
 * already known here, explicitly, per endpoint -- no dynamic lookup needed.
 */
@RestController
@RequiredArgsConstructor
public class CallbackInboxController {

    private final CallbackInboxService callbackInboxService;

    @PostMapping("${paybridge.callback.path}")
    public ResponseEntity<Void> receiveAsyncCallbackResponse(HttpServletRequest request) throws IOException {
        callbackInboxService.receiveAsyncCallbackResponse(EPaymentOperator.MOOV_MONEY, request, readBody(request));
        return ResponseEntity.ok().build();
    }

}
