# Transaction API Failure Scenarios

This document summarizes the failure and empty-result cases described for the
transaction endpoints in [TransactionController.md](./TransactionController.md).

## Error responses

Transaction endpoints return responses using the API's `R<T>` wrapper. The
controller maps handled failures to structured error responses with
`SchoolErrorCode` values. The available endpoint summary identifies `SE001` for
a missing transaction and refers to `SE003` for a generic error; it does not
specify the complete response schema or HTTP status codes.

| Scenario | Endpoint(s) | Behavior |
| --- | --- | --- |
| Transaction not found | `GET /api/v1/transactions/{id}` | Returns a structured not-found response using `SE001`. |
| Authenticated user cannot access the requested student's history | `GET /api/v1/transactions/history` | Returns an access-denied error. History and count queries are not run unless access validation succeeds. |
| Access is denied while loading recent transactions | `GET /api/v1/transactions/recent-transactions` | Returns an error response mapped from the access-denied exception. |
| Unexpected error while loading history or recent transactions | `/history`, `/recent-transactions` | Returns a generic error response mapped through `SchoolErrorCode` (the summary identifies `SE003`). |

## Empty results

An empty result is handled separately from an error:

- `GET /api/v1/transactions/history` returns a success response with a message
  indicating that the history is empty.
- `GET /api/v1/transactions/recent-transactions` returns the controller's
  no-transactions response.
- `GET /api/v1/transactions` returns the results for the selected filter (or
  all transactions when no filter is provided); the summary does not describe
  an empty-list response as an error.

## Details not specified

The controller summary does not define HTTP status codes, exact error-message
text, the full `R<T>` error payload, or validation behavior for malformed
query parameters. Confirm those details against the controller and response
types before relying on them in a client.
