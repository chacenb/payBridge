package com.sahelys.payBridge.globals.configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MoovConfiguration {
    public static final String INIT_PAYMENT             = "InitTrans_OnlineMerchantPayment";
    public static final String GIVE_CHANGE              = "InitTrans_GiveChange";
    public static final String SEARCH_TRANSACTION_BY_ID = "SearchTransactionByExtID";
    public static final String QUERY_BALANCE            = "QueryOrganizationBalance";
    public static final String DATE_FORMAT              = "yyyyMMddHHmmss";


    public static String formatDate(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return date.format(formatter);
    }

}
