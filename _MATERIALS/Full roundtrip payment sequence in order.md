Full sequence, in order, each with what it does and why it's there.

---------
---------
**1. Create the payment request, FROM THE CLIENT APP (BILLETIQUE SETRAG)**
```bash
curl -X POST http://172.31.63.50:9000/api/paybridge/v1/submit-payment-request \
  -H "Content-Type: application/json" \
  -d '{
    "clientAppId": "CHACE",
    "clientPaymentRequestId": "CHACE-LIVE-TEST-001",
    "amount": 100,
    "currency": "XAF",
    "description": "Live Moov round-trip test",
    "callbackUrl": "https://example.com/callback"
  }'
```
------
RESPONSE
```
{
  data: {
    paymentTransactionId: "7b9fe661-28cc-4b6d-96f8-c46a7cce713f",
    paymentUrl:"http://localhost:9000/pay/7b9fe661-28cc-4b6d-96f8-c46a7cce713f",
    status: "PENDING",
  },
  error: null,
  errorCode: null,
  extra: null,
  message: null,
  page: null,
  status: "200 OK",
  timeStamp: "2026-09-23T14:13:52.00241331Z",
  total: null,
};
```
Creates the `ClientPaymentRequest` + linked `PaymentTransaction` (status `PENDING`). Grab `paymentTransactionId` from the response — every following call uses it.


---------
---------
---------
**2. Trigger the actual payment**
```bash
curl -X POST http://172.31.63.50:9000/api/paybridge/v1/payment-transactions/7b9fe661-28cc-4b6d-96f8-c46a7cce713f/proceed-payment \
  -H "Content-Type: application/json" \
  -d '{
    "providerCode": "MOOV_MONEY",
    "customerPhone": "24166855158"
  }'
```
------
RESPONSE 1 [SYNCHRONOUS ACKNOWLEDGEMENT]
```
{
  data: {
    amount: 100.0,
    availableProviders: ["MOOV_MONEY", "AIRTEL_MONEY"],
    currency: "XAF",
    description: "Live Moov round-trip test",
    paymentTransactionId: "7b9fe661-28cc-4b6d-96f8-c46a7cce713f",
    selectedProvider: "MOOV_MONEY",
    status: "PROCESSING",
  },
  error: null,
  errorCode: null,
  extra: null,
  message: null,
  page: null,
  status: "200 OK",
  timeStamp: "2026-09-23T15:14:16.316428343Z",
  total: null,
};
```

---------
---------
---------
**3. Poll for the real outcome**
```bash
curl http://172.31.63.50:9000/api/paybridge/v1/payment-transactions/7b9fe661-28cc-4b6d-96f8-c46a7cce713f
```
Repeat this every few seconds. `status` moves from `PROCESSING` to `SUCCESS`/`FAILED` once Moov's real async callback arrives and gets correlated — that's the actual proof the round trip works, not just step 3's sync ack.
------
RESPONSE
```
{
  data: {
    amount: 100.0,
    availableProviders: ["MOOV_MONEY", "AIRTEL_MONEY"],
    currency: "XAF",
    description: "Live Moov round-trip test",
    paymentTransactionId: "7b9fe661-28cc-4b6d-96f8-c46a7cce713f",
    selectedProvider: "MOOV_MONEY",
    status: "SUCCESS",
  },
  error: null,
  errorCode: null,
  extra: null,
  message: null,
  page: null,
  status: "200 OK",
  timeStamp: "2026-09-23T15:20:29.085776342Z",
  total: null,
};
```


---------
---------
---------
**4. Once terminal, client notification**
```bash
curl -X POST http://172.31.63.50:9000/api/paybridge/v1/payment-transactions/7b9fe661-28cc-4b6d-96f8-c46a7cce713f/notify-client
```

------
RESPONSE [SENT BACK TO THE CLIENT APP]
```
{
  data: {
    amount: 100.0,
    clientAppId: "CHACE",
    clientPaymentRequestId: "CHACE-LIVE-TEST-001",
    completedAt: "2026-09-23T15:41:08.053099052Z",
    currency: "XAF",
    paymentTransactionId: "7b9fe661-28cc-4b6d-96f8-c46a7cce713f",
    provider: "MOOV_MONEY",
    providerPaymentTransactionId: "AG_20260923_7020399089209a655d0c",
    status: "SUCCESS",
  },
  error: null,
  errorCode: null,
  extra: null,
  message: null,
  page: null,
  status: "200 OK",
  timeStamp: "2026-09-23T15:41:08.324129264Z",
  total: null,
};
```