package com.sahelys.payBridge.domain.parser;

import com.sahelys.payBridge.controllers.IPayBridgeController;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component @Slf4j
public class Parser {

    public IPayBridgeController.MerchantPaymentResponse parseMerchantPaymentResponse(String xml) {
        try {
            return null;
        } catch (Exception ex) {
            log.error("SOAP Parsing error", ex);
            throw new CustomException(ex);
        }
    }
}
