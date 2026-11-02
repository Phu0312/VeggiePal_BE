# Checkout preview and plan changes

Authenticated users can call `GET /api/payments/checkout-preview?plan=PRO` through the gateway before `POST /api/payments/checkout`. The response contains `currentPlan`, `targetPlan`, `remainingDays`, `bonusDays`, `newExpiresAt`, and `calculatedAt`. The preview is an estimate at `calculatedAt`; the final expiry is recalculated when the verified payOS webhook arrives.

Rules:

- Same active plan: retain the original start date and add the purchased duration to the current expiry.
- Upgrade (`FREE < PLUS < PRO`): activate the higher tier immediately at payment confirmation. Bonus days are `floor(whole remaining days × old plan price ÷ new plan price)`. For 20 PLUS days, `floor(20 × 29000 ÷ 59000) = 9`, so a 30-day PRO purchase expires after 39 days.
- Downgrade while a higher tier remains active: checkout and preview return HTTP 409, code `6010`, with the current tier and expiry date. A lower-tier order that was started earlier but paid after a newer upgrade is credited as days on the higher tier; it never replaces the higher tier.
- An old plan expiring exactly at the payment confirmation instant has zero remaining days. The new plan starts at that instant without a bonus.

`remainingDays` counts complete 24-hour periods and rounds down. Existing payOS prices are unchanged.
