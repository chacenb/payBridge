# Moov Money asynchronous integration: architecture and completion contract

## Purpose

PayBridge is an operator-neutral payment gateway.  Its first adapter is Moov Money (Huawei CPS SOAP).  A payment submission is **asynchronous**: receiving an HTTP/SOAP acknowledgement only confirms that Moov Money accepted the request for processing; it is not the financial outcome.

The application must retain the request, accept Moov Money's later callback, correlate and process that callback exactly once, and make the definitive outcome available to the initiating client.

## Confirmed business flow

```text
Client
  |  1. Submit debit / other Moov Money operation
  v
PayBridge API
  |  2. Validate, allocate an immutable correlation ID, persist PENDING
  |  3. Build the Huawei SOAP request with that ID and a public callback URL
  v
Moov Money
  |  4. Immediate acknowledgement (transport/submission result)
  v
PayBridge API --> return 202 Accepted + paymentId + status URL

Moov Money
  |  5. Later POSTs the definitive outcome to PayBridge
  v
PayBridge callback endpoint
  |  6. Authenticate/validate, persist raw message, correlate, apply outcome once
  v
Database --> payment becomes SUCCEEDED / FAILED / REJECTED / UNKNOWN
  |
  +--> client observes outcome through polling (initial delivery); websocket/webhook is optional later
```

## Operator commands

The code presently contains four Huawei command templates:

| Command | Current endpoint family | Expected treatment |
| --- | --- | --- |
| `InitTrans_OnlineMerchantPayment` | payment | asynchronous; debit customer |
| `InitTrans_GiveChange` | payment | asynchronous; credit/refund customer |
| `QueryOrganizationBalance` | payment | classify with Moov Money documentation/test evidence before implementation |
| `SearchTransactionByExtID` | sync | reconciliation/query operation; not an original transaction |

The stated "three async endpoints" must be frozen in an operator contract before implementation. This memo deliberately does not guess the third command: the SOAP acknowledgement and callback payloads captured during the approved Linux tests are the source of truth.

## API contract to implement

### Submit an operation

`POST /api/v1/payments` (or preserve the existing Moov-specific routes behind this façade) must:

1. validate the request: operator, operation type, client reference, MSISDN, amount/currency, and caller authorization;
2. enforce idempotency using a caller-provided `Idempotency-Key` or unique client reference;
3. create one `paymentId` and one unique `originatorConversationId` before calling Moov Money;
4. store the record as `PENDING_SUBMISSION`, then `SUBMITTED` only after a valid acknowledgement;
5. return `202 Accepted`, never a successful financial result, with `paymentId`, `originatorConversationId`, status `PENDING`, and `GET /api/v1/payments/{paymentId}`.

An acknowledgement rejection moves the record to `REJECTED` and must be represented separately from a final operator failure.

### Callback

`POST /api/v1/operators/moov-money/callback` must accept the exact content type and payload format sent by Moov Money. Its responsibilities are:

1. verify the source/authentication mechanism defined by Moov Money (mTLS, signature, allow-list, credential, or a combination);
2. save the raw body and essential headers before business processing, with secrets redacted;
3. parse the message using DTOs derived from real captured callback samples;
4. find the payment by `OriginatorConversationID` (and validate any returned operator conversation ID);
5. map the provider result code to a canonical final state;
6. make processing idempotent: duplicate delivery must return a successful HTTP response without reapplying financial side effects;
7. return the HTTP response required by Moov Money only after durable acceptance. If temporary processing fails, return an appropriate retryable status according to the provider contract.

Unknown or malformed callbacks must be retained in an exception/reconciliation queue and alerted; they must not create a financial transaction.

### Client outcome retrieval

The first supported delivery mechanism is polling:

`GET /api/v1/payments/{paymentId}` returns the current state, operator identifiers, final result where available, and timestamps.

WebSocket or client webhooks can be added after persistence and idempotency are reliable. They are notification channels only; polling remains the source of truth.

## Data model (minimum)

### `payment`

- `id` (PayBridge UUID)
- `operator` (`MOOV_MONEY`; designed for a future second operator)
- `operation_type` (merchant payment, give change, ...)
- `client_reference` and idempotency key
- `originator_conversation_id` (unique)
- `operator_conversation_id` (unique when supplied)
- MSISDN and amount/currency (protect sensitive fields appropriately)
- `status`: `PENDING_SUBMISSION`, `SUBMITTED`, `SUCCEEDED`, `FAILED`, `REJECTED`, `UNKNOWN`, `EXPIRED`
- acknowledgement code/message and final code/message
- `created_at`, `submitted_at`, `completed_at`, `version`

### `operator_callback`

- callback ID, operator, received time, content type, sanitized headers, encrypted/raw payload reference, payload hash, parsed correlation ID, processing status, and processing error.

A unique constraint on `originator_conversation_id` and a uniqueness/idempotency strategy for callbacks are mandatory.

## Application structure to introduce

```text
api/                 public submit and status controllers
operator/moov/       SOAP client, command builders, acknowledgement/callback mappers
application/         payment lifecycle/use-case services
domain/              canonical payment model, states, events, ports
infrastructure/      JPA repositories, migrations, security, outbound notifications
callback/            callback controller and durable callback processor
```

Keep Moov SOAP DTOs and XML details inside `operator/moov`; the public API and domain model must not expose Huawei-specific fields except diagnostic identifiers where necessary.

## Required implementation work

1. **Capture and codify the Moov contract.** Save sanitized examples of each outbound request, immediate acknowledgement, and final callback; document result codes, retries, security, required callback response, and the exact three async commands.
2. **Persistence and migrations.** Add a supported relational database, Flyway/Liquibase migrations, payment and callback entities, indexes, optimistic locking, and encryption/redaction policy.
3. **Correct SOAP transport.** Verify the expected SOAP envelope/payload treatment with the approved terminal requests. Add connection/read timeouts, TLS/certificate configuration, SOAP fault handling, and safe structured logging.
4. **Lifecycle implementation.** Generate collision-safe IDs (UUID/ULID-based—not a second-resolution timestamp), persist before submission, parse acknowledgements, and implement all approved commands.
5. **Callback implementation.** Add the public callback route, DTO/XML parser, authentication, durable inbox, correlation, idempotent transition logic, and reconciliation handling.
6. **Public client contract.** Add validation, idempotency, `202` submission semantics, status polling, standard error responses, and API documentation.
7. **Security and operations.** Move all credentials and URLs out of committed profile files; rotate exposed credentials; redact SOAP logs; configure secret injection, health/readiness checks, metrics, audit logs, alerts, and a dead-letter/reconciliation process.
8. **Testing.** Add unit tests for XML generation/mapping and state transitions; integration tests with a mock Moov SOAP server; callback duplicate/out-of-order tests; database migration tests; and an end-to-end test reproducing the approved Linux interaction.

## Definition of done for the Moov adapter

- All three agreed asynchronous commands create an idempotent payment record and return `202` after a valid operator acknowledgement.
- A callback received at the configured public URL is authenticated, persisted, correlated, and changes the right record exactly once.
- Clients can reliably retrieve the final state through the status endpoint.
- Duplicate, unknown, malformed, delayed, and provider-error callbacks are observable and safe.
- No credential, security credential, MSISDN, or unredacted SOAP payload is emitted to normal logs.
- Automated tests cover the documented acknowledgement and callback samples, including failure paths.
- A reconciliation job can query Moov Money for payments left non-final beyond the agreed timeout.

## Inputs needed before coding the callback and persistence layers

1. Sanitized terminal captures for each of the three async requests: request, immediate acknowledgement, and later callback.
2. Moov Money's callback security and retry specification, including expected HTTP success response body if any.
3. The exact business meaning and final result-code mapping for each command.
4. The target database and deployment environment/public callback address.
5. The client-facing outcome preference after polling: webhook, WebSocket, or both.

Until those inputs are available, we can safely build the shared domain model, persistence, request validation, `202` API, and a callback inbox, but must not invent provider field mappings or callback acknowledgement semantics.
