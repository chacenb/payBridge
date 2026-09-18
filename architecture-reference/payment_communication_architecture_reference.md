# Decoupled Payment Communication Architecture

## 1. Purpose

This document defines the communication architecture between:

- a **Client Application**, which owns a business process and needs payment as one step of that process;
- a standalone **Payment Module**, which owns the payment workflow and Mobile Money integrations;
- one or more **Mobile Money Providers**, which execute the actual payment.

The architecture deliberately decouples the Client Application from provider-specific payment logic.

The core communication pattern is:

```text
Client Application
        |
        | ClientPaymentRequest
        v
Payment Module
        |
        | Payment processing
        v
Mobile Money Provider
        |
        | Provider result
        v
Payment Module
        |
        | PaymentTransactionCallback
        v
Client Application
```

---

# 2. Architectural Boundary

## Client Application

The Client Application owns:

- the original business transaction;
- the decision that payment is required;
- creation of the `ClientPaymentRequest`;
- redirecting the user to the Payment Module;
- receiving the `PaymentTransactionCallback`;
- updating its own business transaction;
- continuing its business process after payment.

The Client Application does **not** need to know:

- how Mobile Money is integrated;
- which provider-specific API is used;
- provider-specific request/response formats;
- provider authentication;
- provider retry mechanisms;
- provider callback formats.

## Payment Module

The Payment Module owns:

- payment request validation;
- creation and lifecycle of the `PaymentTransaction`;
- payment UI;
- provider selection;
- payment attempts;
- Mobile Money provider integration;
- provider response handling;
- payment status normalization;
- payment transaction finalization;
- callback delivery to the Client Application.

## Mobile Money Provider

The provider owns:

- actual Mobile Money authorization/execution;
- provider-specific transaction processing;
- provider-specific response/status mechanisms.

---

# 3. Complete Communication Flow

```text
CLIENT APPLICATION                         PAYMENT MODULE                         MOBILE MONEY
──────────────────                         ───────────────                       ────────────

1. Business transaction
        |
        v
2. Create ClientPaymentRequest
        |
        | ClientPaymentRequest
        +---------------------------->
                                         3. Validate request
                                                 |
                                                 v
                                         4. Create PaymentTransaction
                                                 |
                                                 v
                                         5. Return PaymentResponse
        <----------------------------+
        |
        v
6. Redirect user to paymentUrl
        |
        | GET /pay/{paymentTransactionId}
        +---------------------------->
                                         7. Display payment page
                                                 |
                                                 v
                                         8. User selects provider
                                                 |
                                                 v
                                         9. Create PaymentAttempt
                                                 |
                                                 | ProviderPaymentRequest
                                                 +---------------------------->
                                                                                10. Process payment
                                                                                       |
                                                                                       v
                                                                                11. Provider result
                                                 <----------------------------+
                                                 |
                                                 v
                                         12. Update PaymentAttempt
                                                 |
                                                 v
                                         13. Finalize PaymentTransaction
                                                 |
                                                 v
                                         14. Create PaymentTransactionCallback
                                                 |
                                                 | callback
        <----------------------------------------+
        |
        v
15. Validate/process callback
        |
        v
16. Update business transaction
        |
        v
17. Continue business process
```

---

# 4. Detailed Step-by-Step Flow

## Step 1 — Client business transaction

The Client Application starts with its own business process.

Example:

```text
Customer places an order
        |
        v
Order ORD-1001 created
        |
        v
Payment required
```

Minimal example:

```java
class Order {
    String orderId;
    BigDecimal amount;
    String currency;
    OrderStatus status;
}
```

Example:

```text
orderId = ORD-1001
amount = 25000
currency = XAF
status = PAYMENT_PENDING
```

The Payment Module does not need the complete `Order`.

---

## Step 2 — Client creates ClientPaymentRequest

The Client Application prepares the payment request.

### ClientPaymentRequest

```java
class ClientPaymentRequest {

    String clientAppId;

    String clientPaymentRequestId;

    BigDecimal amount;

    String currency;

    String description;

    String callbackUrl;
}
```

Example:

```json
{
  "clientAppId": "FTA",
  "clientPaymentRequestId": "CPR-1001",
  "amount": 25000,
  "currency": "XAF",
  "description": "Order ORD-1001",
  "callbackUrl": "https://fta.example.com/api/payment/callback"
}
```

### Identifier meanings

```text
clientAppId
    Identifies the Client Application.

clientPaymentRequestId
    Identifies the payment request within the Client Application.
```

The Client Application owns both identifiers.

---

## Step 3 — Payment Module receives and validates the request

The Payment Module receives the request through its API.

Example:

```http
POST /api/payment-requests
```

The Payment Module validates:

```text
- clientAppId exists
- clientAppId is authorized
- clientPaymentRequestId is present
- amount is valid
- currency is supported
- callbackUrl is valid
- request is not a duplicate
```

Invalid requests are rejected.

The validation logic remains inside the Payment Module.

---

# 5. Step 4 — Create PaymentTransaction

After successful validation, the Payment Module creates its own payment transaction.

### PaymentTransaction

```java
class PaymentTransaction {

    String paymentTransactionId;

    String clientAppId;

    String clientPaymentRequestId;

    BigDecimal amount;

    String currency;

    String description;

    String callbackUrl;

    PaymentTransactionStatus status;

    Instant createdAt;
}
```

Example:

```text
paymentTransactionId  = PTX-8F42A1
clientAppId            = FTA
clientPaymentRequestId = CPR-1001
amount                 = 25000
currency               = XAF
status                 = PENDING
```

The Payment Module therefore has its own identifier:

```text
paymentTransactionId = PTX-8F42A1
```

while retaining the Client's identifier:

```text
clientPaymentRequestId = CPR-1001
```

---

# 6. Step 5 — Return PaymentResponse

The Payment Module returns enough information for the Client Application to redirect the user.

### PaymentResponse

```java
class PaymentResponse {

    String paymentTransactionId;

    String paymentUrl;

    PaymentTransactionStatus status;
}
```

Example:

```json
{
  "paymentTransactionId": "PTX-8F42A1",
  "paymentUrl": "https://payment.example.com/pay/PTX-8F42A1",
  "status": "PENDING"
}
```

The Client Application does not need to know how the payment page works.

---

# 7. Step 6 — Redirect user to Payment Module

The Client Application redirects the user to:

```text
https://payment.example.com/pay/PTX-8F42A1
```

The Payment Module retrieves the corresponding `PaymentTransaction`.

Example endpoint:

```http
GET /pay/{paymentTransactionId}
```

The Payment Module verifies that the transaction is still payable.

---

# 8. Step 7 — Display payment page

The Payment Module presents the payment interface.

Minimal view model:

```java
class PaymentPage {

    String paymentTransactionId;

    BigDecimal amount;

    String currency;

    String description;

    List<String> availableProviders;
}
```

Example:

```text
Payment

25,000 XAF

Order ORD-1001

Choose payment method:

[ Moov Money ]
[ Airtel Money ]
```

---

# 9. Step 8 — User selects payment provider

The user selects a Mobile Money provider.

Example:

```text
MOOV
```

The Payment Module does not modify the Client Payment Request.

It creates a payment attempt associated with the Payment Transaction.

---

# 10. Step 9 — Create PaymentAttempt

A Payment Transaction can potentially have one or more attempts.

### PaymentAttempt

```java
class PaymentAttempt {

    String paymentAttemptId;

    String paymentTransactionId;

    String provider;

    PaymentAttemptStatus status;

    String providerPaymentTransactionId;

    Instant createdAt;
}
```

Example:

```text
paymentAttemptId              = PAT-001
paymentTransactionId          = PTX-8F42A1
provider                      = MOOV
status                        = INITIATED
providerPaymentTransactionId  = MOOV-998877
```

This model allows scenarios such as:

```text
Payment Transaction PTX-8F42A1

Attempt 1 -> MOOV   -> FAILED
Attempt 2 -> AIRTEL -> SUCCESS
```

The Payment Transaction remains the overall payment record.

The Payment Attempt represents a specific attempt against a provider.

---

# 11. Step 10 — Create provider-specific payment request

The Payment Module translates its internal payment model into a provider-specific request.

### ProviderPaymentRequest

```java
class ProviderPaymentRequest {

    String paymentTransactionId;

    String paymentAttemptId;

    BigDecimal amount;

    String currency;

    String customerPhone;
}
```

The actual provider implementation may transform this further into the provider's own API/SOAP/REST model.

Recommended abstraction:

```java
interface PaymentProvider {

    ProviderPaymentResponse initiatePayment(
        ProviderPaymentRequest request
    );
}
```

Implementations can include:

```text
MoovPaymentProvider
AirtelPaymentProvider
...
```

Provider-specific logic remains behind this boundary.

---

# 12. Step 11 — Provider processes payment

The Payment Module calls the selected provider.

Conceptually:

```text
Payment Module
      |
      v
PaymentProvider
      |
      +----> Moov
      |
      +----> Airtel
      |
      +----> Other provider
```

The provider processes the Mobile Money payment.

The interaction may be:

- synchronous;
- asynchronous;
- callback-based;
- polling-based;

depending on the provider.

The Payment Module hides these differences from the Client Application.

---

# 13. Step 12 — Provider returns result

The provider returns a provider-specific response.

### ProviderPaymentResponse

```java
class ProviderPaymentResponse {

    String providerPaymentTransactionId;

    ProviderPaymentStatus status;

    String message;
}
```

Example:

```json
{
  "providerPaymentTransactionId": "MOOV-998877",
  "status": "PENDING",
  "message": "Payment initiated"
}
```

The Payment Module stores the provider reference against the `PaymentAttempt`.

---

# 14. Step 13 — Provider completion callback/result

If the provider completes the transaction asynchronously, the provider calls the Payment Module.

Conceptually:

```text
Mobile Money Provider
        |
        | provider callback
        v
Payment Module
```

The Payment Module identifies the corresponding:

```text
Provider Payment Transaction
        |
        v
PaymentAttempt
        |
        v
PaymentTransaction
```

The provider-specific status is then translated into the Payment Module's normalized status.

Example:

```text
MOOV SUCCESS
    ->
PaymentTransaction SUCCESS

MOOV FAILED
    ->
PaymentTransaction FAILED

MOOV PENDING
    ->
PaymentTransaction PROCESSING
```

---

# 15. Step 14 — Finalize PaymentTransaction

The Payment Module updates:

```text
PaymentTransaction
------------------
paymentTransactionId = PTX-8F42A1
status                = SUCCESS
```

The Payment Attempt is also updated:

```text
PaymentAttempt
--------------
paymentAttemptId = PAT-001
status           = SUCCESS
```

The Payment Module is now the authoritative source for the payment status.

---

# 16. Step 15 — Create PaymentTransactionCallback

The Payment Module prepares the callback for the Client Application.

### PaymentTransactionCallback

```java
class PaymentTransactionCallback {

    String paymentTransactionId;

    String clientAppId;

    String clientPaymentRequestId;

    BigDecimal amount;

    String currency;

    PaymentTransactionStatus status;

    String provider;

    String providerPaymentTransactionId;

    Instant completedAt;
}
```

Example:

```json
{
  "paymentTransactionId": "PTX-8F42A1",
  "clientAppId": "FTA",
  "clientPaymentRequestId": "CPR-1001",
  "amount": 25000,
  "currency": "XAF",
  "status": "SUCCESS",
  "provider": "MOOV",
  "providerPaymentTransactionId": "MOOV-998877",
  "completedAt": "2026-09-18T09:25:30Z"
}
```

---

# 17. Step 16 — Payment Module calls Client callback

The Payment Module performs:

```http
POST https://fta.example.com/api/payment/callback
```

with the `PaymentTransactionCallback`.

The callback URL was supplied by the Client in the original `ClientPaymentRequest`.

---

# 18. Step 17 — Client receives callback

The Client Application exposes a callback endpoint.

Example:

```java
@PostMapping("/api/payment/callback")
public ResponseEntity<Void> paymentCallback(
        @RequestBody PaymentTransactionCallback callback) {

    // validate callback
    // locate business transaction
    // update business transaction

    return ResponseEntity.ok().build();
}
```

The Client uses:

```text
clientAppId
+
clientPaymentRequestId
```

to correlate the callback with its original payment request.

It may also retain:

```text
paymentTransactionId
```

for payment traceability.

---

# 19. Step 18 — Client updates its business transaction

The Client Application updates its own business transaction.

Example:

```text
Order ORD-1001

PAYMENT_PENDING
        |
        v
PAID
```

The Client may store:

```java
class Order {

    String orderId;

    BigDecimal amount;

    String currency;

    OrderStatus status;

    String paymentTransactionId;
}
```

Example:

```text
orderId              = ORD-1001
status               = PAID
paymentTransactionId = PTX-8F42A1
```

---

# 20. Step 19 — Client continues business process

Once payment is confirmed, the Client Application continues its own workflow.

Example:

```text
Payment SUCCESS
       |
       v
Order PAID
       |
       +--> Generate invoice
       |
       +--> Provision service
       |
       +--> Send notification
       |
       +--> Complete order
```

The Payment Module's responsibility ends with the payment lifecycle and callback delivery.

---

# 21. Core Entity Model

## ClientPaymentRequest

```java
class ClientPaymentRequest {

    String clientAppId;

    String clientPaymentRequestId;

    BigDecimal amount;

    String currency;

    String description;

    String callbackUrl;
}
```

## PaymentTransaction

```java
class PaymentTransaction {

    String paymentTransactionId;

    String clientAppId;

    String clientPaymentRequestId;

    BigDecimal amount;

    String currency;

    String description;

    String callbackUrl;

    PaymentTransactionStatus status;

    Instant createdAt;
}
```

## PaymentAttempt

```java
class PaymentAttempt {

    String paymentAttemptId;

    String paymentTransactionId;

    String provider;

    PaymentAttemptStatus status;

    String providerPaymentTransactionId;

    Instant createdAt;
}
```

## PaymentTransactionCallback

```java
class PaymentTransactionCallback {

    String paymentTransactionId;

    String clientAppId;

    String clientPaymentRequestId;

    BigDecimal amount;

    String currency;

    PaymentTransactionStatus status;

    String provider;

    String providerPaymentTransactionId;

    Instant completedAt;
}
```

---

# 22. Supporting DTOs

## PaymentResponse

```java
class PaymentResponse {

    String paymentTransactionId;

    String paymentUrl;

    PaymentTransactionStatus status;
}
```

## ProviderPaymentRequest

```java
class ProviderPaymentRequest {

    String paymentTransactionId;

    String paymentAttemptId;

    BigDecimal amount;

    String currency;

    String customerPhone;
}
```

## ProviderPaymentResponse

```java
class ProviderPaymentResponse {

    String providerPaymentTransactionId;

    ProviderPaymentStatus status;

    String message;
}
```

---

# 23. Status Models

## PaymentTransactionStatus

The Payment Module should expose its own normalized status vocabulary.

```java
enum PaymentTransactionStatus {

    PENDING,

    PROCESSING,

    SUCCESS,

    FAILED,

    CANCELLED,

    EXPIRED
}
```

Provider-specific statuses should not leak into the Client Application.

## PaymentAttemptStatus

```java
enum PaymentAttemptStatus {

    INITIATED,

    PROCESSING,

    SUCCESS,

    FAILED,

    CANCELLED
}
```

## ProviderPaymentStatus

This remains internal to the provider integration layer.

```java
enum ProviderPaymentStatus {

    PENDING,

    SUCCESS,

    FAILED
}
```

The exact provider statuses can vary.

---

# 24. Identifier Convention

The architecture uses explicit ownership in identifier names.

| Identifier | Owner | Purpose |
|---|---|---|
| `clientAppId` | Client/Payment Module | Identifies the Client Application |
| `clientPaymentRequestId` | Client | Identifies the Client Payment Request |
| `paymentTransactionId` | Payment Module | Identifies the Payment Transaction |
| `paymentAttemptId` | Payment Module | Identifies one payment attempt |
| `providerPaymentTransactionId` | Provider | Identifies the provider-side payment |

The correlation chain is:

```text
clientAppId
    +
clientPaymentRequestId
        |
        v
paymentTransactionId
        |
        v
paymentAttemptId
        |
        v
providerPaymentTransactionId
```

This gives each system its own identifier without confusing ownership.

---

# 25. Complete Object Relationship

```text
ClientPaymentRequest
────────────────────
clientAppId
clientPaymentRequestId
amount
currency
description
callbackUrl
        |
        | creates
        v
PaymentTransaction
──────────────────
paymentTransactionId
clientAppId
clientPaymentRequestId
amount
currency
description
callbackUrl
status
        |
        | has one or more
        v
PaymentAttempt
───────────────
paymentAttemptId
paymentTransactionId
provider
providerPaymentTransactionId
status
        |
        | communicates with
        v
Mobile Money Provider
        |
        | result
        v
PaymentTransaction
        |
        | callback
        v
PaymentTransactionCallback
───────────────────────────
paymentTransactionId
clientAppId
clientPaymentRequestId
amount
currency
status
provider
providerPaymentTransactionId
completedAt
```

---

# 26. Recommended Payment Module Package Structure

```text
payment-module/
└── src/main/java/com/example/payment/
    │
    ├── controller/
    │   ├── PaymentController.java
    │   └── ProviderCallbackController.java
    │
    ├── dto/
    │   ├── ClientPaymentRequest.java
    │   ├── PaymentResponse.java
    │   ├── PaymentTransactionCallback.java
    │   ├── ProviderPaymentRequest.java
    │   └── ProviderPaymentResponse.java
    │
    ├── domain/
    │   ├── PaymentTransaction.java
    │   ├── PaymentAttempt.java
    │   ├── PaymentTransactionStatus.java
    │   ├── PaymentAttemptStatus.java
    │   └── ProviderPaymentStatus.java
    │
    ├── provider/
    │   ├── PaymentProvider.java
    │   ├── MoovPaymentProvider.java
    │   └── AirtelPaymentProvider.java
    │
    ├── service/
    │   ├── PaymentTransactionService.java
    │   ├── PaymentAttemptService.java
    │   ├── PaymentCallbackService.java
    │   └── ProviderPaymentService.java
    │
    └── repository/
        ├── PaymentTransactionRepository.java
        └── PaymentAttemptRepository.java
```

---

# 27. Client Application Package Structure

```text
client-app/
└── payment/
    │
    ├── dto/
    │   ├── ClientPaymentRequest.java
    │   ├── PaymentResponse.java
    │   └── PaymentTransactionCallback.java
    │
    ├── service/
    │   └── PaymentService.java
    │
    └── controller/
        └── PaymentCallbackController.java
```

The Client Application should not contain Mobile Money provider-specific models.

---

# 28. API-Level Communication

A minimal API contract could look like this.

## Client → Payment Module

```http
POST /api/payment-transactions
```

Request:

```json
{
  "clientAppId": "FTA",
  "clientPaymentRequestId": "CPR-1001",
  "amount": 25000,
  "currency": "XAF",
  "description": "Order ORD-1001",
  "callbackUrl": "https://fta.example.com/api/payment/callback"
}
```

Response:

```json
{
  "paymentTransactionId": "PTX-8F42A1",
  "paymentUrl": "https://payment.example.com/pay/PTX-8F42A1",
  "status": "PENDING"
}
```

## Payment Module → Client

```http
POST /api/payment/callback
```

Request:

```json
{
  "paymentTransactionId": "PTX-8F42A1",
  "clientAppId": "FTA",
  "clientPaymentRequestId": "CPR-1001",
  "amount": 25000,
  "currency": "XAF",
  "status": "SUCCESS",
  "provider": "MOOV",
  "providerPaymentTransactionId": "MOOV-998877",
  "completedAt": "2026-09-18T09:25:30Z"
}
```

---

# 29. Multi-Client Example

The Payment Module can serve multiple applications.

```text
                    PAYMENT MODULE
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
         FTA            SIRA        OTHER_APP
          |              |              |
          |              |              |
      CPR-001         CPR-002         CPR-003
          |              |              |
          v              v              v
       PTX-001         PTX-002         PTX-003
```

Example:

```text
FTA:
clientAppId = FTA
clientPaymentRequestId = CPR-001
paymentTransactionId = PTX-001

SIRA:
clientAppId = SIRA
clientPaymentRequestId = CPR-002
paymentTransactionId = PTX-002
```

The Payment Module therefore acts as a centralized payment service while maintaining clear client/application ownership.

---

# 30. Complete Sequence Diagram

```text
Client App        Payment Module        Provider        User
    |                   |                  |             |
    | Create business  |                  |             |
    | transaction      |                  |             |
    |                  |                  |             |
    | ClientPayment    |                  |             |
    | Request           |                  |             |
    |------------------>|                  |             |
    |                  |                  |             |
    |                  | Validate         |             |
    |                  | request          |             |
    |                  |                  |             |
    |                  | Create Payment   |             |
    |                  | Transaction      |             |
    |                  |                  |             |
    |<-----------------| PaymentResponse  |             |
    |                  | + paymentUrl     |             |
    |                  |                  |             |
    | Redirect user ------------------------------->    |
    |                  |                  |             |
    |                  |<----------------------------- |
    |                  | Display payment page          |
    |                  |                  |             |
    |                  |<----------------------------- |
    |                  | User selects provider         |
    |                  |                  |             |
    |                  | Create PaymentAttempt         |
    |                  |                  |             |
    |                  | ProviderPaymentRequest        |
    |                  |----------------->|             |
    |                  |                  |             |
    |                  |                  | Process     |
    |                  |                  | payment     |
    |                  |                  |             |
    |                  | Provider result |             |
    |                  |<-----------------|             |
    |                  |                  |             |
    |                  | Update attempt   |             |
    |                  |                  |             |
    |                  | Finalize PaymentTransaction   |
    |                  |                  |             |
    |                  | PaymentTransactionCallback    |
    |<-----------------|                  |             |
    |                  |                  |             |
    | Validate callback|                  |             |
    |                  |                  |             |
    | Update business  |                  |             |
    | transaction      |                  |             |
    |                  |                  |             |
    | Continue business|                  |             |
    | process          |                  |             |
```

---

# 31. Final Architecture Shortlist

For the initial implementation, the architecture can be reduced to these elements.

### Client Application

```text
1. Business Transaction
2. ClientPaymentRequest
3. PaymentResponse
4. PaymentTransactionCallback
5. Callback Controller
6. Business Transaction Update
```

### Payment Module

```text
1. ClientPaymentRequest
2. PaymentTransaction
3. PaymentAttempt
4. PaymentTransactionStatus
5. PaymentAttemptStatus
6. PaymentResponse
7. ProviderPaymentRequest
8. ProviderPaymentResponse
9. PaymentTransactionCallback
10. PaymentProvider abstraction
11. Provider implementations
12. Callback delivery service
```

### Mobile Money Provider

```text
1. Provider-specific request
2. Provider-specific transaction
3. Provider-specific response/callback
```

### Core identifiers

```text
clientAppId
clientPaymentRequestId
paymentTransactionId
paymentAttemptId
providerPaymentTransactionId
```

### Core communication

```text
ClientPaymentRequest
        ↓
PaymentTransaction
        ↓
PaymentAttempt
        ↓
Mobile Money Provider
        ↓
PaymentTransaction
        ↓
PaymentTransactionCallback
        ↓
Client Business Transaction
```

---

# 32. Architectural Baseline

The baseline principle is:

> **The Client Application owns the business transaction. The Payment Module owns the payment transaction. The Mobile Money Provider owns the provider transaction. Each boundary has its own identifier and contract.**

Therefore:

```text
CLIENT
  |
  | ClientPaymentRequest
  v
PAYMENT MODULE
  |
  | ProviderPaymentRequest
  v
MOBILE MONEY PROVIDER
  |
  | Provider result
  v
PAYMENT MODULE
  |
  | PaymentTransactionCallback
  v
CLIENT
```

The Client and Payment Module communicate through a **generic payment contract**, while the Payment Module isolates all provider-specific complexity behind its provider abstraction.

This is the foundational communication model to use before introducing additional concerns such as authentication/signatures, idempotency, retry policies, callback security, transaction reconciliation, persistence details, and observability.
