Here's the full sequence, in order, each with what it does and why it's there.

**1. Sanity check the server is up**
```bash
curl http://172.31.63.50:9000/actuator/health
```
Confirms the app is running before testing anything real — expect `{"status":"UP"}`.


---------
---------
---------
**2. Create the payment request**
```bash
curl -X POST http://172.31.63.50:9000/api/paybridge/v1/submit-payment-request \
  -H "Content-Type: application/json" \
  -d '{
    "clientAppId": "FTA",
    "clientPaymentRequestId": "CPR-LIVE-TEST-001",
    "amount": 500,
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
**3. Trigger the actual payment**
```bash
curl -X POST http://172.31.63.50:9000/api/paybridge/v1/payment-transactions/7b9fe661-28cc-4b6d-96f8-c46a7cce713f/proceed-payment \
  -H "Content-Type: application/json" \
  -d '{
    "providerCode": "MOOV_MONEY",
    "customerPhone": "24166855158"
  }'
```
------
RESPONSE
```

```
This is the real one: builds the real XML with `<TXN_ID>` as `OriginatorConversationID`, sends it to Moov, and applies the sync ack. Expect `200`/`PROCESSING` (accepted, real outcome pending) or a `400 RUNTINE_EXCEPTION` if the call itself failed (network/TLS/credentials) — check the app log's `SOAP Request`/`SOAP Response` lines either way.








---------
---------
---------
**4. Poll for the real outcome**
```bash
curl http://172.31.63.50:9000/api/paybridge/v1/payment-transactions/<TXN_ID>
```
Repeat this every few seconds. `status` moves from `PROCESSING` to `SUCCESS`/`FAILED` once Moov's real async callback arrives and gets correlated — that's the actual proof the round trip works, not just step 3's sync ack.
------
RESPONSE
```

```






---------
---------
---------
**5. Once terminal, simulate the client notification**
```bash
curl -X POST http://172.31.63.50:9000/api/paybridge/v1/payment-transactions/<TXN_ID>/notify-client
```
------
RESPONSE
```

```
Only valid once step 4 shows a terminal status. Returns the full outcome payload (`clientAppId`, `amount`, `status`, `provider`, `providerPaymentTransactionId`, `completedAt`) — exactly what a real client app would receive. This is also the last check that the whole chain (transaction → linked `ClientPaymentRequest` → outbound payload construction) is intact.







---------
---------
---------

**6. If step 4 never moves past `PROCESSING` — check whether the callback even arrived**
```bash
docker exec paybridge-postgres psql -U paybridge -d paybridge -c \
  "SELECT id, operator_code, processing_status, processing_error, received_at FROM operator_callbacks ORDER BY received_at DESC LIMIT 5;"
```

------
RESPONSE
```

```
Run this on the preprod host itself. If a row shows up with `processing_status = FAILED`, `processing_error` tells you exactly why correlation failed (e.g. an `OriginatorConversationID` mismatch). If no row shows up at all, the callback never reached the server — a network/routing/registered-`ResultURL` problem on Moov's side, not an app bug.