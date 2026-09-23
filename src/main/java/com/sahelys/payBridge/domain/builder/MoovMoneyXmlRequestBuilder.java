package com.sahelys.payBridge.domain.builder;

import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.sahelys.payBridge.globals.MoovConfiguration.*;
import static com.sahelys.payBridge.globals.utils.Utils.formatDate;

@Component
public class MoovMoneyXmlRequestBuilder {

    @Value("${momo.third-party-id}")
    private String thirdPartyId;

    @Value("${momo.password}")
    private String password;

    @Value("${momo.result-url}")
    private String resultUrl;

    @Value("${momo.key-owner:1}")
    private String keyOwner;

    @Value("${momo.channel-code:1010}")
    private String channelCode;

    /**
     * Merchant Payment. {@code paymentTransactionId} is sent as the real
     * {@code OriginatorConversationID} -- it's exactly what the later async callback echoes
     * back for {@link com.sahelys.payBridge.provider.ProviderXmlParser} to correlate to
     * the right transaction, so it must be the caller's real id, never a minted one.
     */
    public String buildMerchantPaymentRequest(UUID paymentTransactionId, String customerMsisdn, String amount) {
        ConversationId conversationInfos = buildConversationInfos(paymentTransactionId);
        return """
               <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                 xmlns:api="http://cps.huawei.com/cpsinterface/api_requestmgr"
                                 xmlns:req="http://cps.huawei.com/cpsinterface/request"
                                 xmlns:com="http://cps.huawei.com/cpsinterface/common">
                   <soapenv:Header/>
                   <soapenv:Body>
                       <api:Request>
                           %s
                           <req:Body>
                               <req:Identity>
                                   %s
                                   <req:PrimaryParty>
                                       <req:IdentifierType>1</req:IdentifierType>
                                       <req:Identifier>%s</req:Identifier>
                                   </req:PrimaryParty>
                                   <req:ReceiverParty>
                                       <req:IdentifierType>4</req:IdentifierType>
                                       <req:Identifier>PMUG</req:Identifier>
                                   </req:ReceiverParty>
                                   <req:Requester>
                                       <req:IdentifierType>4</req:IdentifierType>
                                       <req:Identifier>I0700</req:Identifier>
                                   </req:Requester>
                               </req:Identity>
                               <req:TransactionRequest>
                                   <req:Parameters>
                                       <req:Parameter>
                                           <com:Key>ChargePayer</com:Key>
                                           <com:Value>Subscriber</com:Value>
                                       </req:Parameter>
                                       <req:Amount>%s</req:Amount>
                                       <req:Currency>XAF</req:Currency>
                                   </req:Parameters>
                               </req:TransactionRequest>
                               <req:ReferenceData>
                                   <req:ReferenceItem>
                                       <com:Key>externalData1</com:Key>
                                       <com:Value>NO_DATA</com:Value>
                                   </req:ReferenceItem>
                                   <req:ReferenceItem>
                                       <com:Key>externalData2</com:Key>
                                       <com:Value>NO_DATA</com:Value>
                                   </req:ReferenceItem>
                               </req:ReferenceData>
                           </req:Body>
                       </api:Request>
                   </soapenv:Body>
               </soapenv:Envelope>
               """.formatted(
                buildHeader(INIT_PAYMENT, conversationInfos),
                buildInitiator(),
                customerMsisdn,
                amount);
    }


    /**
     * Give Change
     */
    public String buildGiveChangeRequest(String customerMsisdn, String amount) {
        ConversationId conversationInfos = buildConversationInfos();

        return """
               <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                 xmlns:api="http://cps.huawei.com/cpsinterface/api_requestmgr"
                                 xmlns:req="http://cps.huawei.com/cpsinterface/request"
                                 xmlns:com="http://cps.huawei.com/cpsinterface/common">
                   <soapenv:Header/>
                   <soapenv:Body>
                       <api:Request>
                           %s
                           <req:Body>
                               <req:Identity>
                                   %s
                                   <req:PrimaryParty>
                                       <req:IdentifierType>4</req:IdentifierType>
                                       <req:Identifier>PMUG</req:Identifier>
                                   </req:PrimaryParty>
                                   <req:ReceiverParty>
                                       <req:IdentifierType>1</req:IdentifierType>
                                       <req:Identifier>%s</req:Identifier>
                                   </req:ReceiverParty>
                                   <req:Requester>
                                       <req:IdentifierType>4</req:IdentifierType>
                                       <req:Identifier>I0700</req:Identifier>
                                   </req:Requester>
                               </req:Identity>
                               <req:TransactionRequest>
                                   <req:Parameters>
                                       <req:Parameter>
                                           <com:Key>ChargePayer</com:Key>
                                           <com:Value>Subscriber</com:Value>
                                       </req:Parameter>
                                       <req:Amount>%s</req:Amount>
                                       <req:Currency>XAF</req:Currency>
                                   </req:Parameters>
                               </req:TransactionRequest>
                               <req:ReferenceData>
                                   <req:ReferenceItem>
                                       <com:Key>externalData1</com:Key>
                                       <com:Value>NO_DATA</com:Value>
                                   </req:ReferenceItem>
                                   <req:ReferenceItem>
                                       <com:Key>externalData2</com:Key>
                                       <com:Value>NO_DATA</com:Value>
                                   </req:ReferenceItem>
                               </req:ReferenceData>
                           </req:Body>
                       </api:Request>
                   </soapenv:Body>
               </soapenv:Envelope>
               """.formatted(
                buildHeader(GIVE_CHANGE, conversationInfos),
                buildInitiator(),
                customerMsisdn,
                amount);
    }


    public String buildSearchTransactionByExtIdRequest(LocalDateTime startDate, LocalDateTime endDate) {
        ConversationId conversationInfos = buildConversationInfos();
        return """
               <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                 xmlns:api="http://cps.huawei.com/synccpsinterface/api_requestmgr"
                                 xmlns:req="http://cps.huawei.com/synccpsinterface/request"
                                 xmlns:com="http://cps.huawei.com/synccpsinterface/common"
                                 xmlns:cus="http://cps.huawei.com/synccpsinterface/customizedrequest">
                   <soapenv:Header/>
                   <soapenv:Body>
                       <api:Request>
                           %s
                           <req:Body>
                               <req:Identity>
                                   %s
                               </req:Identity>
                               <req:SearchTransactionByExtIDRequest>
                                   <req:OriginalConversationID>%s</req:OriginalConversationID>
                                   <req:StartDate>%s</req:StartDate>
                                   <req:EndDate>%s</req:EndDate>
                               </req:SearchTransactionByExtIDRequest>
                           </req:Body>
                       </api:Request>
                   </soapenv:Body>
               </soapenv:Envelope>
               """.formatted(
                buildHeader(SEARCH_TRANSACTION_BY_ID, conversationInfos),
                buildInitiator(),
                "SEARCH_" + conversationInfos.conversationId,
                formatDate(startDate),
                formatDate(endDate));
    }

    /**
     * Query Organization Balance
     */
    public String buildQueryOrganizationBalanceRequest() {
        ConversationId conversationInfos = buildConversationInfos();
        return """
               <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                 xmlns:api="http://cps.huawei.com/cpsinterface/api_requestmgr"
                                 xmlns:req="http://cps.huawei.com/cpsinterface/request"
                                 xmlns:com="http://cps.huawei.com/cpsinterface/common">

                   <soapenv:Header/>
                   <soapenv:Body>
                       <api:Request>
                           %s
                           <req:Body>
                               <req:Identity>
                                   %s
                                   <req:ReceiverParty>
                                       <req:IdentifierType>4</req:IdentifierType>
                                       <req:Identifier>2076</req:Identifier>
                                   </req:ReceiverParty>
                               </req:Identity>
                               <req:QueryOrganizationBalanceRequest>
                                   <req:AccountType>SAHELYS Main Account</req:AccountType>
                               </req:QueryOrganizationBalanceRequest>
                           </req:Body>
                       </api:Request>
                   </soapenv:Body>
               </soapenv:Envelope>
               """.formatted(
                buildHeader(QUERY_BALANCE, conversationInfos),
                buildInitiator());
    }


    private String buildHeader(String commandId, ConversationId conversation) {
        return """
               <req:Header>
                   <req:Version>1.0</req:Version>
                   <req:CommandID>%s</req:CommandID>
                   <req:OriginatorConversationID>%s</req:OriginatorConversationID>
                   <req:Caller>
                       <req:CallerType>2</req:CallerType>
                       <req:ThirdPartyID>%s</req:ThirdPartyID>
                       <req:Password>%s</req:Password>
                       <req:ResultURL>%s</req:ResultURL>
                   </req:Caller>
                   <req:KeyOwner>%s</req:KeyOwner>
                   <req:Timestamp>%s</req:Timestamp>
                   <req:ChannelCode>%s</req:ChannelCode>
               </req:Header>
               """.formatted(
                commandId,
                conversation.conversationId(),
                thirdPartyId,
                password,
                resultUrl,
                keyOwner,
                conversation.timestamp(),
                channelCode);
    }

    private String buildInitiator() {
        return """
               <req:Initiator>
                   <req:IdentifierType>14</req:IdentifierType>
                   <req:Identifier>%s</req:Identifier>
                   <req:SecurityCredential>%s</req:SecurityCredential>
               </req:Initiator>
               """.formatted(thirdPartyId, password);
    }


    private static ConversationId buildConversationInfos() {
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = formatDate(now);
        return ConversationId.builder()
                             .conversationId("SAPAYID_" + formattedDate)
                             .timestamp(formattedDate)
                             .build();
    }

    /**
     * Same shape as {@link #buildConversationInfos()}, but for callers that have a real
     * correlation id to send instead of minting one -- see {@link #buildMerchantPaymentRequest(UUID, String, String)}.
     */
    private static ConversationId buildConversationInfos(UUID paymentTransactionId) {
        return ConversationId.builder()
                             .conversationId(paymentTransactionId.toString())
                             .timestamp(formatDate(LocalDateTime.now()))
                             .build();
    }

    @Builder
    record ConversationId(String conversationId, String timestamp) {
    }


}
