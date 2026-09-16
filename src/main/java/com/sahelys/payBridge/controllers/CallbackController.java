package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.services.CallbackInboxService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class CallbackController {

    private final CallbackInboxService callbackInboxService;

    @PostMapping("${paybridge.callback.path}")
    public ResponseEntity<Void> receiveMoovCallback(HttpServletRequest request) throws IOException {
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        callbackInboxService.receive(request, rawBody);
        return ResponseEntity.ok().build();
    }
}
