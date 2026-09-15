package com.sahelys.payBridge.domain.parser;

import com.sahelys.payBridge.controllers.interfaces.IMoovMoneyGatewayController;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;

@Component @Slf4j
public class Parser {

    public IMoovMoneyGatewayController.MerchantPaymentResponse parseMerchantPaymentResponse(String xml) {
        try {
            JAXBContext context = JAXBContext.newInstance(IMoovMoneyGatewayController.MerchantPaymentResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (IMoovMoneyGatewayController.MerchantPaymentResponse) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception ex) {
            log.error("SOAP Parsing error", ex);
            throw new CustomException(ex);
        }
    }
}
