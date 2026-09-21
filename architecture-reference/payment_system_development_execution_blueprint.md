# Payment System --- Development Execution Blueprint

## Handoff Purpose

This document is the **development execution blueprint** for the
standalone Mobile Money Payment System.

It is intended to be provided to **Claude Code or another implementation
agent** together with:

> `payment_communication_architecture_reference.md`

The architecture reference defines **what the system is and how it is
designed**.

This document defines **how the implementation work should be
decomposed, sequenced, parallelized, integrated, and validated**.

### Source-of-truth rule

When an implementation decision conflicts with the architecture
reference, the architecture reference takes precedence unless the
architecture is explicitly revised.

Do not invent new domain concepts, rename identifiers, or introduce
provider-specific concepts into the Client Application without an
explicit architectural decision.

------------------------------------------------------------------------

# 1. Development Objective

Build two independently deployable applications:

``` text
CLIENT APPLICATION
       |
       | ClientPaymentRequest
       v
PAYMENT MODULE
       |
       | provider integration
       v
MOBILE MONEY PROVIDER
       |
       | result / callback
       v
PAYMENT MODULE
       |
       | PaymentTransactionCallback
       v
CLIENT APPLICATION
```

The system must allow several Client Applications to consume the same
Payment Module.

The Payment Module owns payment processing.

The Client Application owns its business transaction.

The Mobile Money Provider owns its provider transaction.

------------------------------------------------------------------------

# 2. Canonical Domain Vocabulary

Do not mix entity names with generic transaction terminology.

Use these identifiers consistently:

  Object                       Identifier
  ---------------------------- --------------------------------
  Client Application           `clientAppId`
  Client Payment Request       `clientPaymentRequestId`
  Payment Module Transaction   `paymentTransactionId`
  Payment Attempt              `paymentAttemptId`
  Provider Transaction         `providerPaymentTransactionId`

Core objects:

``` text
ClientPaymentRequest
PaymentTransaction
PaymentAttempt
PaymentTransactionCallback
```

Relationship:

``` text
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

This correlation chain must remain intact throughout implementation,
logging, callbacks, and troubleshooting.

------------------------------------------------------------------------

# 3. Contracts-First Development

Before implementing large features, establish and freeze the contracts.

The primary contracts are:

1.  Client → Payment Module payment request
2.  Payment Module → Client payment response
3.  Payment Module → Client payment callback
4.  Payment Module internal provider abstraction
5.  Provider → Payment Module callback/result contract
6.  Payment transaction state model
7.  Error model
8.  Authentication/security contract
9.  Idempotency rules

Once a contract is stable, developers should be able to work
independently against it.

Use mocks/stubs where the real implementation is not yet available.

Example:

``` text
Developer A
Transaction Core
       |
       +---- contract ----+
                          |
Developer B              |
Client API <--------------+
                          |
Developer C              |
Payment UI <--------------+
                          |
Developer D              |
Provider abstraction <----+
```

The objective is to minimize direct developer-to-developer blocking.

------------------------------------------------------------------------

# 4. Development Workstreams

## Payment Module

The Payment Module is divided into these major workstreams:

  -----------------------------------------------------------------------
  ID                      Workstream              Responsibility
  ----------------------- ----------------------- -----------------------
  PM-01                   Foundation              Application/platform
                                                  skeleton

  PM-02                   Transaction Core        PaymentTransaction and
                                                  PaymentAttempt
                                                  lifecycle

  PM-03                   Client API              Client-facing payment
                                                  APIs

  PM-04                   Payment UI              User-facing payment
                                                  flow

  PM-05                   Provider Abstraction    Generic provider
                                                  contract

  PM-06                   Moov Integration        Moov-specific
                                                  implementation

  PM-07                   Callback Engine         Provider and Client
                                                  callbacks

  PM-08                   Security & Reliability  Authentication,
                                                  idempotency, retries,
                                                  observability
  -----------------------------------------------------------------------

## Client Application

  ----------------------------------------------------------------------------
  ID                      Workstream              Responsibility
  ----------------------- ----------------------- ----------------------------
  CA-01                   Payment Integration     HTTP client, configuration,
                          Foundation              DTOs

  CA-02                   Business Transaction    Connect payment to the
                          Integration             business process

  CA-03                   Payment UX              Redirect/status experience

  CA-04                   Callback Processing     Receive and process
                                                  PaymentTransactionCallback

  CA-05                   Reliability             Idempotency, reconciliation,
                                                  error handling
  ----------------------------------------------------------------------------

------------------------------------------------------------------------

# 5. Payment Module --- Detailed Backlog

# PM-01 --- Foundation

## Objective

Create a runnable Payment Module skeleton that other developers can
build on.

### PM-01.1 Project Setup

Tasks:

-   Create Spring Boot application
-   Configure Java version
-   Configure build system
-   Establish base package structure
-   Establish application profiles
-   Establish configuration conventions

### PM-01.2 Database

Tasks:

-   Configure PostgreSQL
-   Configure database connection
-   Configure Flyway
-   Establish migration naming convention
-   Establish development/test database configuration

### PM-01.3 REST Foundation

Tasks:

-   Configure REST application
-   Establish JSON conventions
-   Establish HTTP error handling
-   Establish validation conventions
-   Establish global exception handling

### PM-01.4 Containerization

Tasks:

-   Create Dockerfile
-   Create Docker Compose development environment
-   Configure application/database networking
-   Document local startup

### PM-01.5 CI

Tasks:

-   Build pipeline
-   Unit-test execution
-   Packaging
-   Container build validation

### Acceptance Criteria

-   Application starts successfully.
-   Database connection works.
-   Flyway runs successfully.
-   A clean checkout can be built and tested.
-   Application can run locally using the documented procedure.
-   No business logic is introduced into the foundation layer.

------------------------------------------------------------------------

# PM-02 --- Payment Transaction Core

## Objective

Implement the internal payment transaction model and lifecycle.

## PM-02.1 PaymentTransaction

Implement:

``` text
PaymentTransaction
```

Required concepts include:

``` text
paymentTransactionId
clientAppId
clientPaymentRequestId
amount
currency
description
callbackUrl
status
createdAt
```

Tasks:

-   Entity/model
-   Database migration
-   Repository
-   Service
-   Validation
-   Creation
-   Retrieval

## PM-02.2 PaymentAttempt

Implement:

``` text
PaymentAttempt
```

Required concepts include:

``` text
paymentAttemptId
paymentTransactionId
provider
status
providerPaymentTransactionId
createdAt
```

Tasks:

-   Entity/model
-   Database migration
-   Repository
-   Service
-   Attempt creation
-   Attempt lookup

## PM-02.3 Transaction State Machine

Implement controlled state transitions.

Base transaction states:

``` text
PENDING
PROCESSING
SUCCESS
FAILED
CANCELLED
EXPIRED
```

Tasks:

-   Define legal transitions
-   Reject invalid transitions
-   Persist state changes
-   Record timestamps where required
-   Ensure provider events cannot arbitrarily corrupt the state

## Acceptance Criteria

-   PaymentTransaction can be created and persisted.
-   PaymentAttempt can be created and linked to PaymentTransaction.
-   IDs are unique and correctly correlated.
-   Invalid state transitions are rejected.
-   Multiple attempts can be represented where the business flow
    requires them.
-   Unit tests cover the transaction lifecycle.

------------------------------------------------------------------------

# PM-03 --- Client API

## Objective

Expose the public API consumed by Client Applications.

## PM-03.1 Create Payment Transaction API

Endpoint:

``` http
POST /api/payment-transactions
```

Input:

``` text
ClientPaymentRequest
```

Expected concepts:

``` text
clientAppId
clientPaymentRequestId
amount
currency
description
callbackUrl
```

Processing:

``` text
ClientPaymentRequest
        |
        v
Validation
        |
        v
PaymentTransaction creation
        |
        v
PaymentResponse
```

## PM-03.2 Payment Response

Response contains:

``` text
paymentTransactionId
paymentUrl
status
```

## PM-03.3 Payment Transaction Query

Provide transaction retrieval where required by the architecture.

## PM-03.4 Validation

Validate:

-   Client identity
-   Request identity
-   Amount
-   Currency
-   Callback URL
-   Required fields
-   Duplicate requests

## PM-03.5 API Documentation

Document:

-   Request
-   Response
-   HTTP statuses
-   Errors
-   Authentication
-   Idempotency behavior

## Acceptance Criteria

A valid ClientPaymentRequest produces exactly one appropriate
PaymentTransaction for an idempotent request.

Invalid requests return controlled errors.

The API exposes no provider-specific implementation details.

------------------------------------------------------------------------

# PM-04 --- Payment UI

## Objective

Provide the user-facing payment flow.

Expected flow:

``` text
paymentUrl
    |
    v
Payment Page
    |
    v
Provider Selection
    |
    v
Customer Payment Information
    |
    v
Payment Initiation
    |
    v
Processing
    |
    +---- SUCCESS
    |
    +---- FAILED
    |
    +---- CANCELLED
    |
    +---- EXPIRED
```

## Tasks

-   Payment transaction lookup
-   Payment page
-   Amount/currency display
-   Provider selection
-   Customer information
-   Payment initiation
-   Processing state
-   Success state
-   Failure state
-   Cancellation handling
-   Expiration handling

## Important Boundary

The Client Application does not reproduce this provider-specific payment
UI.

The Payment Module owns the payment experience.

------------------------------------------------------------------------

# PM-05 --- Provider Abstraction

## Objective

Prevent provider-specific implementation from leaking into the
transaction core.

Create a generic provider boundary conceptually equivalent to:

``` java
PaymentProvider
```

The interface should support the operations required by the
architecture, such as:

``` text
initiatePayment(...)
checkPayment(...)
```

The exact method signatures should be finalized during implementation
without changing the architectural boundary.

## PM-05.1 Provider Request/Response

Define:

``` text
ProviderPaymentRequest
ProviderPaymentResponse
ProviderPaymentStatus
```

Provider-specific fields remain behind the PaymentProvider boundary
wherever possible.

## PM-05.2 Provider Registry/Selection

Implement a mechanism allowing:

``` text
MOOV
AIRTEL
...
```

to resolve to the correct provider implementation.

## PM-05.3 Provider Error Mapping

Normalize provider-specific failures into Payment Module-level
errors/statuses.

## Acceptance Criteria

-   Transaction Core depends on the generic provider abstraction, not
    Moov-specific classes.
-   A second provider can be added without rewriting Transaction Core.
-   Provider-specific status terminology does not leak into Client
    Application contracts.

------------------------------------------------------------------------

# PM-06 --- Moov Integration

## Objective

Implement the first concrete Mobile Money provider.

``` text
PaymentProvider
       |
       v
MoovPaymentProvider
       |
       v
Moov API
```

## Tasks

### Authentication

-   Provider authentication
-   Token management where applicable
-   Credential configuration
-   Credential security

### Payment Initiation

-   Map ProviderPaymentRequest to Moov request
-   Send request
-   Parse response
-   Extract `providerPaymentTransactionId`

### Status

-   Map Moov status to ProviderPaymentStatus
-   Map provider result to PaymentTransactionStatus

### Errors

-   Provider rejection
-   Timeout
-   Network error
-   Authentication failure
-   Invalid response
-   Unexpected provider response

### Callback

Implement provider callback processing where required by the provider
integration.

## Acceptance Criteria

-   Moov can be invoked through the generic PaymentProvider abstraction.
-   Provider transaction IDs are persisted.
-   Provider statuses are normalized.
-   Provider failures do not crash the application.
-   Secrets are not hard-coded.

------------------------------------------------------------------------

# PM-07 --- Callback Engine

This workstream contains two distinct callback directions.

## PM-07.1 Provider → Payment Module

``` text
Provider
   |
   v
ProviderCallbackController
   |
   v
Provider callback validation
   |
   v
PaymentAttempt
   |
   v
PaymentTransaction
```

Tasks:

-   Provider callback endpoint
-   Authentication/signature validation where applicable
-   Payload validation
-   Provider transaction lookup
-   Attempt update
-   Transaction state update
-   Duplicate callback handling
-   Unknown transaction handling
-   Logging

## PM-07.2 Payment Module → Client

``` text
PaymentTransaction
       |
       v
PaymentCallbackService
       |
       v
PaymentTransactionCallback
       |
       v
Client callbackUrl
```

Tasks:

-   Build callback payload
-   Deliver callback
-   Handle HTTP responses
-   Retry failed delivery
-   Prevent duplicate business effects
-   Record delivery result
-   Log delivery attempts

## Acceptance Criteria

Provider completion results correctly update PaymentAttempt and
PaymentTransaction.

Client receives a normalized PaymentTransactionCallback.

Temporary Client callback failure does not destroy the completed payment
state.

Repeated callbacks do not cause duplicate business effects.

------------------------------------------------------------------------

# PM-08 --- Security & Reliability

## PM-08.1 Authentication

Define and implement authentication between:

``` text
Client Application → Payment Module
```

and:

``` text
Payment Module → Client Application
```

where required.

## PM-08.2 Callback Security

Define how the Client verifies that a PaymentTransactionCallback
genuinely originates from the Payment Module.

## PM-08.3 Idempotency

Define idempotency for:

-   Client payment creation
-   Provider callbacks
-   Client callbacks

Use:

``` text
clientAppId
+
clientPaymentRequestId
```

as the primary client-side correlation identity.

## PM-08.4 Retry

Define retry behavior for:

-   Provider communication
-   Client callback delivery

Do not retry blindly.

Retries must respect transaction state and idempotency.

## PM-08.5 Observability

Implement:

-   Correlation IDs
-   Structured logs
-   Transaction identifiers in logs
-   Provider identifiers in logs
-   Callback delivery logs
-   Health checks
-   Useful metrics

------------------------------------------------------------------------

# 6. Payment Module Sprint Plan

# Sprint 0 --- Architecture and Contracts

Goal:

> Remove ambiguity before implementation begins.

Deliverables:

``` text
Domain vocabulary
API contracts
State machine
Provider abstraction
Error model
Security model
Idempotency model
Database model
Sequence flows
```

No major business implementation should begin until these are
sufficiently stable.

------------------------------------------------------------------------

# Sprint 1 --- Backbone

Parallel tracks:

### Developer A

PM-01 Foundation

### Developer B

PM-02 Transaction Core

### Developer C

PM-03 Client API

### Developer D

PM-05 Provider Abstraction

The objective is:

``` text
Client API
    |
    v
PaymentTransaction
    |
    v
PaymentAttempt
    |
    v
PaymentProvider
```

At this point, the provider can be a mock.

------------------------------------------------------------------------

# Sprint 2 --- Functional Payment Flow

Parallel tracks:

### Developer A

PM-04 Payment UI

### Developer B

Transaction lifecycle refinement

### Developer C

Client API completion and validation

### Developer D

PM-06 Moov integration

### Developer E

PM-07 Provider callback

### Developer F

PM-08 Security/idempotency foundations

Objective:

A complete payment can be initiated through the Payment Module and reach
the provider.

------------------------------------------------------------------------

# Sprint 3 --- Completion and Integration

Parallel tracks:

### Track A

Client callback delivery

### Track B

Provider callback processing

### Track C

Failure/retry/idempotency

### Track D

Observability

### Track E

End-to-end testing

Primary scenario:

``` text
Client
  |
  | ClientPaymentRequest
  v
Payment Module
  |
  | PaymentTransaction
  v
PaymentAttempt
  |
  | ProviderPaymentRequest
  v
Moov
  |
  | result/callback
  v
Payment Module
  |
  | PaymentTransactionCallback
  v
Client
```

------------------------------------------------------------------------

# 7. Parallelization Rules

## Rule 1 --- Work by capability, not technical layer

Avoid:

``` text
Developer 1 = all entities
Developer 2 = all repositories
Developer 3 = all controllers
Developer 4 = all services
```

Prefer:

``` text
Developer 1 = Transaction Core
Developer 2 = Client API
Developer 3 = Payment UI
Developer 4 = Provider abstraction
Developer 5 = Moov
Developer 6 = Callback engine
```

Each developer owns a coherent capability.

------------------------------------------------------------------------

## Rule 2 --- Contracts unblock people

If Developer A owns Moov and Developer B owns Client API, Developer B
must not wait for Moov.

Use:

``` text
Mock Provider
Mock Payment Module
Mock Callback
```

until the real implementation is available.

------------------------------------------------------------------------

## Rule 3 --- Integration happens continuously

Do not wait until the final sprint to discover that the components do
not fit.

Every sprint should produce an integration checkpoint.

------------------------------------------------------------------------

## Rule 4 --- Keep provider logic isolated

Never allow code such as:

``` text
if provider == MOOV
```

to spread through transaction, controller, or Client-facing code.

Provider selection belongs behind the provider abstraction.

------------------------------------------------------------------------

## Rule 5 --- Keep business ownership clear

The Client owns:

``` text
Business Transaction
```

The Payment Module owns:

``` text
PaymentTransaction
PaymentAttempt
Payment processing
```

The Provider owns:

``` text
Provider Payment Transaction
```

------------------------------------------------------------------------

# 8. Dependency Map

Primary dependencies:

``` text
PM-01 Foundation
    |
    +--------------------+
    |                    |
    v                    v
PM-02 Transaction     PM-05 Provider
    Core               Abstraction
    |                    |
    v                    v
PM-03 Client API      PM-06 Moov
    |                    |
    +---------+----------+
              |
              v
          PM-07 Callback
              |
              v
          PM-08 Reliability
              |
              v
          E2E Testing
```

Important non-blocking relationships:

``` text
PM-06 Moov
```

must not block:

``` text
PM-03 Client API
PM-04 Payment UI
```

because those can work against mocks.

Similarly, Client Application development must not wait for the
production provider integration.

------------------------------------------------------------------------

# 9. Client Application Development

The Client Application is an integration consumer, not a second Payment
Module.

It should remain intentionally small.

------------------------------------------------------------------------

# CA-01 --- Payment Integration Foundation

## Tasks

-   Configure Payment Module base URL
-   Configure HTTP client
-   Configure timeouts
-   Configure authentication
-   Create `ClientPaymentRequest`
-   Create `PaymentResponse`
-   Create `PaymentTransactionCallback`
-   Create API client/service
-   Define error handling

Expected flow:

``` text
Business Application
       |
       v
PaymentService
       |
       v
Payment Module API
```

------------------------------------------------------------------------

# CA-02 --- Business Transaction Integration

## Objective

Connect payment to the Client's existing business process.

Example:

``` text
Create Business Transaction
        |
        v
Payment Required
        |
        v
Create ClientPaymentRequest
        |
        v
Payment Module
        |
        v
Store paymentTransactionId
```

Tasks:

-   Identify payment initiation point
-   Generate `clientPaymentRequestId`
-   Build `ClientPaymentRequest`
-   Call Payment Module
-   Persist correlation information
-   Associate `paymentTransactionId` with business transaction
-   Handle payment initiation errors

------------------------------------------------------------------------

# CA-03 --- Payment UX

The Client Application should consume the returned:

``` text
paymentUrl
```

Tasks:

-   Redirect/open payment URL
-   Display payment initiation state
-   Handle return/navigation
-   Display appropriate business-level result
-   Avoid implementing provider-specific payment UI

------------------------------------------------------------------------

# CA-04 --- Callback Processing

Expose the Client callback endpoint.

Conceptually:

``` http
POST /api/payment/callback
```

Flow:

``` text
PaymentTransactionCallback
        |
        v
Validate callback
        |
        v
Find business transaction
        |
        v
Update business payment state
        |
        v
Continue business process
```

Tasks:

-   Callback endpoint
-   Authentication verification
-   Payload validation
-   Find business transaction
-   Validate correlation
-   Idempotent processing
-   Update business state
-   Return appropriate response

Example:

``` text
SUCCESS
   |
   v
Business Transaction = PAID
   |
   v
Continue business workflow
```

------------------------------------------------------------------------

# CA-05 --- Client Reliability

Tasks:

-   Duplicate callback handling
-   Unknown payment transaction handling
-   Invalid callback handling
-   Timeout handling
-   Payment status reconciliation where required
-   Logging
-   Monitoring
-   Error recovery

------------------------------------------------------------------------

# 10. Client Application Sprint Plan

# Client Sprint 0

Define:

``` text
Payment integration point
ClientPaymentRequest mapping
PaymentResponse handling
Callback contract
Business transaction mapping
Idempotency strategy
Authentication
```

------------------------------------------------------------------------

# Client Sprint 1

Parallel work:

### Developer A

CA-01 integration foundation

### Developer B

CA-02 business workflow integration

### Developer C

CA-04 callback endpoint

### Developer D

CA-03 UX

All work can use a mocked Payment Module.

------------------------------------------------------------------------

# Client Sprint 2

Integrate real Payment Module:

``` text
Business Transaction
       |
       v
ClientPaymentRequest
       |
       v
Payment Module
       |
       v
PaymentResponse
       |
       v
paymentUrl
```

And separately:

``` text
Payment Module
       |
       v
PaymentTransactionCallback
       |
       v
Client
       |
       v
Business Transaction update
```

------------------------------------------------------------------------

# Client Sprint 3

Reliability and end-to-end validation:

``` text
SUCCESS
FAILED
CANCELLED
EXPIRED
TIMEOUT
DUPLICATE CALLBACK
UNKNOWN PAYMENT
CLIENT CALLBACK UNAVAILABLE
```

------------------------------------------------------------------------

# 11. Cross-System Integration Milestones

## Milestone 1 --- Contract Compatibility

Verify:

``` text
ClientPaymentRequest
PaymentResponse
PaymentTransactionCallback
```

match exactly between systems.

------------------------------------------------------------------------

## Milestone 2 --- Payment Creation

Scenario:

``` text
Client → Payment Module
```

Expected:

``` text
PaymentTransaction created
paymentTransactionId returned
paymentUrl returned
```

------------------------------------------------------------------------

## Milestone 3 --- Provider Initiation

Scenario:

``` text
Payment Module → Provider
```

Expected:

``` text
PaymentAttempt created
providerPaymentTransactionId stored
```

------------------------------------------------------------------------

## Milestone 4 --- Successful Completion

Scenario:

``` text
Provider
   ↓
Payment Module
   ↓
Client
```

Expected:

``` text
PaymentTransaction = SUCCESS
Client receives PaymentTransactionCallback
Business transaction becomes paid
```

------------------------------------------------------------------------

## Milestone 5 --- Failure Completion

Verify:

``` text
Provider failure
PaymentTransaction = FAILED
Client receives FAILED callback
Business transaction is not incorrectly marked paid
```

------------------------------------------------------------------------

## Milestone 6 --- Reliability

Verify:

``` text
Duplicate provider callback
Duplicate client callback
Provider timeout
Client callback timeout
Network interruption
Unknown transaction
Invalid callback
```

------------------------------------------------------------------------

# 12. Jira Structure

Recommended hierarchy:

``` text
EPIC
  |
  +-- FEATURE
        |
        +-- STORY
              |
              +-- TASK
              +-- TEST
```

Example:

``` text
EPIC: PM-02 Payment Transaction Core

FEATURE:
Create PaymentTransaction

STORY:
As the Payment Module, I need to create and persist a PaymentTransaction.

TASK:
Create model

TASK:
Create Flyway migration

TASK:
Create repository

TASK:
Create service

TASK:
Implement validation

TASK:
Write unit tests
```

Do not create a Jira ticket for every line of code.

A task should represent a meaningful unit of work.

------------------------------------------------------------------------

# 13. Definition of Ready

A story is ready for development when:

-   Objective is clear.
-   Architectural boundary is known.
-   Required contract is defined.
-   Dependencies are identified.
-   Acceptance criteria exist.
-   No unresolved domain ambiguity blocks implementation.
-   Required mock/stub behavior is defined when another component is
    unavailable.

------------------------------------------------------------------------

# 14. Definition of Done

A story is done when:

-   Implementation is complete.
-   Unit tests exist where applicable.
-   Integration tests exist where applicable.
-   Validation/error handling is implemented.
-   Logging is adequate.
-   API contracts are respected.
-   No provider-specific leakage violates the architecture.
-   Code is integrated into the main development line.
-   Documentation is updated when the contract changes.
-   Acceptance criteria are demonstrably satisfied.

------------------------------------------------------------------------

# 15. Testing Strategy

Testing should exist at multiple levels.

## Unit

Test:

``` text
Transaction state transitions
Validation
Provider mapping
Status mapping
Error mapping
Idempotency logic
```

## Integration

Test:

``` text
REST API
Database
Flyway
Provider adapter
Callback processing
```

## Contract

Verify:

``` text
ClientPaymentRequest
PaymentResponse
PaymentTransactionCallback
```

between Client and Payment Module.

## End-to-End

Verify real business scenarios:

``` text
Create payment
Initiate provider payment
Complete payment
Receive callback
Update business transaction
```

## Failure Testing

Explicitly test:

``` text
Provider unavailable
Provider timeout
Provider rejection
Duplicate callback
Client unavailable
Invalid callback
Unknown paymentTransactionId
Invalid state transition
```

------------------------------------------------------------------------

# 16. Mocking Strategy

Mocks are not temporary shortcuts; they are a deliberate
parallel-development mechanism.

## Payment Module

Provide a mock provider:

``` text
MockPaymentProvider
```

with deterministic outcomes:

``` text
SUCCESS
FAILED
TIMEOUT
PENDING
```

## Client Application

Provide a mock Payment Module API for early development.

Example:

``` text
POST /api/payment-transactions
```

returns:

``` json
{
  "paymentTransactionId": "PTX-MOCK-001",
  "paymentUrl": "http://mock-payment/pay/PTX-MOCK-001",
  "status": "PENDING"
}
```

The mock should support simulated callbacks.

------------------------------------------------------------------------

# 17. Integration Ownership

Every cross-system interface should have an owner.

  Interface                   Owner
  --------------------------- ----------------------------
  Client → Payment Module     Payment Module API owner
  Payment Module → Client     Callback owner
  Payment Module → Provider   Provider integration owner
  Provider → Payment Module   Provider callback owner

Ownership means:

-   Contract maintained
-   Breaking changes communicated
-   Test cases maintained
-   Integration issues investigated

------------------------------------------------------------------------

# 18. Recommended Developer Allocation

For a team of approximately 6 developers:

``` text
Developer A
Foundation + Infrastructure

Developer B
Transaction Core

Developer C
Client API

Developer D
Payment UI

Developer E
Provider Abstraction + Moov

Developer F
Callbacks + Reliability
```

If the team is smaller, combine workstreams.

For example:

``` text
4 developers:

A → Foundation + Transaction Core
B → Client API + Client integration
C → Payment UI + UX
D → Provider + Callback + Reliability
```

Do not force artificial ownership if the team is small.

The architectural boundaries matter more than the exact number of
developers.

------------------------------------------------------------------------

# 19. Recommended Implementation Order

The overall implementation sequence is:

``` text
PHASE 0
Architecture + Contracts
        |
        v
PHASE 1
Foundation
        |
        +----------------------+
        |                      |
        v                      v
Transaction Core       Provider Abstraction
        |                      |
        v                      v
Client API                  Moov
        |                      |
        +----------+-----------+
                   |
                   v
              Payment UI
                   |
                   v
             Callback Engine
                   |
                   v
          Security/Reliability
                   |
                   v
             E2E Testing
```

Client Application can begin much earlier:

``` text
Architecture
     |
     v
Client contracts
     |
     v
Mock Payment Module
     |
     v
Client integration
     |
     v
Real Payment Module
     |
     v
E2E validation
```

------------------------------------------------------------------------

# 20. Final Development Map

``` text
                    PAYMENT SYSTEM
                          |
          +---------------+---------------+
          |                               |
          v                               v
   PAYMENT MODULE                 CLIENT APPLICATION
          |                               |
   +------+------+------+            +----+----+
   |      |      |      |            |         |
 Core   API     UI   Provider       Business  Callback
   |      |             |            |         |
   |      |          Moov             |         |
   |      |             |            |         |
   +------+------+------+            +----+----+
          |                               |
          +---------------+---------------+
                          |
                          v
                    E2E Integration
```

The implementation should always preserve this fundamental ownership
model:

``` text
Client Application
    owns Business Transaction

Payment Module
    owns PaymentTransaction
    owns PaymentAttempt
    owns payment processing

Mobile Money Provider
    owns Provider Payment Transaction
```

And the fundamental correlation model:

``` text
clientAppId
    +
clientPaymentRequestId
        ↓
paymentTransactionId
        ↓
paymentAttemptId
        ↓
providerPaymentTransactionId
```

------------------------------------------------------------------------

# 21. Handoff Instructions for Claude Code

When this document and the architecture reference are provided together:

1.  Read both documents completely before implementing.
2.  Treat the architecture reference as the source of truth for system
    design.
3.  Treat this document as the source of truth for development
    decomposition and sequencing.
4.  Do not start by generating the entire application blindly.
5.  First identify the current implementation state.
6.  Map existing code to the workstreams in this document.
7.  Identify completed, partial, and missing items.
8.  Preserve existing valid implementation unless it conflicts with the
    architecture.
9.  Implement one coherent workstream/story at a time.
10. Respect the dependency graph.
11. Use mocks where another workstream is not yet available.
12. Keep provider-specific implementation isolated.
13. Keep Client Application business logic out of the Payment Module.
14. Keep Payment Module provider logic out of the Client Application.
15. Preserve all canonical identifier names.
16. Add tests alongside implementation.
17. Validate integration contracts before declaring a feature complete.
18. Do not silently introduce architectural changes. Flag them as
    decisions requiring confirmation.

Before implementation, produce a concise status assessment:

``` text
Architecture understood: YES/NO

Payment Module:
PM-01 Foundation:       NOT STARTED / PARTIAL / DONE
PM-02 Transaction Core: NOT STARTED / PARTIAL / DONE
PM-03 Client API:       NOT STARTED / PARTIAL / DONE
PM-04 Payment UI:       NOT STARTED / PARTIAL / DONE
PM-05 Provider Layer:   NOT STARTED / PARTIAL / DONE
PM-06 Moov:             NOT STARTED / PARTIAL / DONE
PM-07 Callback:         NOT STARTED / PARTIAL / DONE
PM-08 Reliability:      NOT STARTED / PARTIAL / DONE

Client Application:
CA-01 Integration:      NOT STARTED / PARTIAL / DONE
CA-02 Business Flow:    NOT STARTED / PARTIAL / DONE
CA-03 UX:               NOT STARTED / PARTIAL / DONE
CA-04 Callback:         NOT STARTED / PARTIAL / DONE
CA-05 Reliability:      NOT STARTED / PARTIAL / DONE
```

Then identify the smallest set of implementation tasks that can be
completed safely without violating the architecture.

------------------------------------------------------------------------

# 22. Architectural Guardrails

Never casually change these principles:

### Guardrail 1

``` text
ClientPaymentRequest
```

is the Client Application's request identity.

### Guardrail 2

``` text
PaymentTransaction
```

is the Payment Module's payment identity.

### Guardrail 3

``` text
PaymentAttempt
```

represents an attempt within the Payment Module.

### Guardrail 4

``` text
providerPaymentTransactionId
```

belongs to the provider boundary.

### Guardrail 5

``` text
PaymentTransactionCallback
```

is the normalized contract sent back to the Client Application.

### Guardrail 6

The Client Application must not depend on provider-specific
implementation details.

### Guardrail 7

The Payment Module must not own the Client Application's business
transaction.

### Guardrail 8

Provider-specific implementation must remain behind the provider
abstraction.

### Guardrail 9

Idempotency must be designed before production payment processing is
considered complete.

### Guardrail 10

A successful provider result and a successfully delivered Client
callback are two different concerns.

The Payment Module must preserve the payment result even when callback
delivery fails.

------------------------------------------------------------------------

# 23. Completion Criteria for the Overall Project

The implementation can be considered functionally complete when this
scenario works reliably:

``` text
1. Client creates business transaction
2. Client creates ClientPaymentRequest
3. Payment Module validates request
4. Payment Module creates PaymentTransaction
5. Payment Module returns paymentTransactionId + paymentUrl
6. User opens paymentUrl
7. User selects provider
8. Payment Module creates PaymentAttempt
9. Payment Module calls provider
10. Provider processes payment
11. Payment Module receives/obtains provider result
12. Payment Module updates PaymentAttempt
13. Payment Module updates PaymentTransaction
14. Payment Module creates PaymentTransactionCallback
15. Payment Module delivers callback to Client
16. Client validates callback
17. Client updates business transaction
18. Duplicate/failure scenarios remain safe and idempotent
```

The system should then be ready for further providers and further Client
Applications without redesigning the core payment architecture.

------------------------------------------------------------------------

# End of Development Execution Blueprint
