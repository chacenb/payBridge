# Process: Consume Async Callback -- MOOV only

Scope discipline for this process: **MOOV only, this process only.** Do not extend this to
AIRTEL (no real captured data exists for it yet) or to other processes (payment
initiation/UI, reconciliation, etc.) until this one is fully real end to end.

## What "done" means for this process

The Provider -> Payment Module leg: a real Moov async callback arrives at
`POST /sahelyspay/payment/callback`, gets parsed, correlated to the right `PaymentAttempt`,
and drives the transaction to its real final state -- with the client notified.

## Status as of 2026-09-18

Built and validated (with a hand-substituted attempt id standing in for the one real gap
below):

- `CallbackController` -> `CallbackInboxService` (durable raw store, unchanged from before)
- `MoovCallbackParser` (real XML shape from `_MATERIALS/async callbacks/...`) -> `ProviderCallbackResult`
- `PaymentAttemptCorrelationService.correlate(...)` -> `PaymentOutcomeApplier` -> attempt/transaction transitions
- `PaymentCallbackDeliveryService` -> client notified on terminal state
- Duplicate delivery correctly ignored; malformed/non-UUID reference correctly fails loudly (`FAILED`, recorded), not silently

Live-tested: success path (attempt+transaction -> SUCCESS, delivery attempted), duplicate
resend (ignored), and the raw unmodified real sample (fails loudly as expected, see below).

**Reception-side completeness fix (still within this process, no sync-side changes):**
`CallbackInboxService.process()` originally only caught `CustomException` -- any genuinely
unexpected failure (a bug, a DB hiccup mid-correlation, anything not deliberately thrown)
would propagate out uncaught and leave the row stuck at `RECEIVED` forever, with no error
recorded. Added a second `catch (Exception ex)` branch so every callback reaches a
terminal, recorded status (`FAILED` + message, full stack trace logged) no matter what goes
wrong -- never silently lost track of. Re-validated live: happy path and the malformed-XML
path both still behave identically after the change.

## The one remaining gap for MOOV to be real (not just validated by hand)

`MoovCallbackParser` trusts that the async callback's `OriginatorConversationID` **is** the
`paymentAttemptId`. That is only true once the *outbound* side sends it that way -- and it
does not yet:

- `MoovMoneyXmlRequestBuilder.buildConversationInfos()` currently mints its own
  `OriginatorConversationID` (`"SAPAYID_" + timestamp`), not the caller's `paymentAttemptId`.
- `MoovPaymentProvider.initiatePayment(...)` is still a bare skeleton (throws/returns null) --
  it doesn't call the builder/client at all yet.

**Next code-generation task, precisely scoped:**

1. Change `MoovMoneyXmlRequestBuilder`'s payment-initiating methods (`buildMerchantPaymentRequest`,
   `buildGiveChangeRequest`) to accept the caller's `paymentAttemptId` (a `UUID`) and use it
   (as a string) as `OriginatorConversationID` in `buildHeader(...)`, instead of
   `buildConversationInfos()`'s own timestamp-based value. `SearchTransactionByExtID` /
   `QueryOrganizationBalance` are out of scope -- they're plain sync queries, not part of
   this process (see `PaymentProvider`'s javadoc).
2. Implement `MoovPaymentProvider.initiatePayment(ProviderPaymentRequest request)` for real:
   build the request via the builder above (passing `request.getPaymentAttemptId()`), send it
   via `MoovMoneyWebClient` (also currently a stub returning `null` -- needs the actual HTTP
   call restored), and map the sync ack (`ResponseCode`/`ResponseDesc`/`ServiceStatus`) into a
   `ProviderPaymentResponse` (`ResponseCode == 0` -> `PENDING`, accepted; anything else ->
   `FAILED`, rejected at submission -- mirrors the same "no failure sample, standard-convention
   assumption" caveat already on `MoovCallbackParser`).
3. Once both land, remove the "hand-substituted attempt id" caveat from this doc and re-run
   the same live validation end to end with zero manual DB steps -- a real `startAttempt` call
   should produce an attempt whose real async callback correlates on its own.

Do not touch `checkPayment(...)`, `AirtelPaymentProvider`, or `AirtelCallbackParser` as part
of this task -- explicitly out of scope per the scope discipline above.
