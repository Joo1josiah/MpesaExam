# TransactionController API Overview

This document summarizes the REST controller defined in `TransactionController.java`.

## Base path

`/api/v1/transactions`

## Endpoints

### 1) Get all transactions
- Method: `GET`
- Endpoint: `/api/v1/transactions`
- Security: Bearer authentication
- Purpose: Retrieve payment transaction records using optional filters.

Supported query parameters:
- `schoolId` (`UUID`)
- `studentId` (`UUID`)
- `invoiceId` (`UUID`)
- `paymentId` (`UUID`)

Behavior:
- If `paymentId` is provided, it fetches the transaction by payment ID.
- Else if `invoiceId` is provided, it fetches transactions by invoice ID.
- Else if `studentId` is provided, it fetches transactions by student ID.
- Else if `schoolId` is provided, it fetches transactions by school ID.
- Else it returns all transactions.

Response:
- Wraps results in `R<List<TransactionEntity>>`
- Returns `R.ok(list)` when successful

---

### 2) Get transaction history with pagination
- Method: `GET`
- Endpoint: `/api/v1/transactions/history`
- Security: Bearer authentication
- Required roles: `PARENT`, `STUDENT`
- Purpose: Fetch paginated transaction history for a student.

Parameters:
- `pageNo` (default `1`)
- `pageSize` (default `10`)
- `studentId` (`UUID`)

Flow:
1. Validates that the authenticated user has access to the given student.
2. Runs the history and count queries only after validation succeeds.
3. Builds a `PaginationDTO<PaymentHistoryDTO>` result.
4. Uses total pages based on the total number of elements.

Response:
- `R<PaginationDTO<PaymentHistoryDTO>>`
- Returns a custom success message depending on whether the list is empty.
- Handles `AccessDeniedException` and generic errors through `SchoolErrorCode`.

---

### 3) Get recent transactions
- Method: `GET`
- Endpoint: `/api/v1/transactions/recent-transactions`
- Security: Bearer authentication
- Required roles: `PARENT`, `STUDENT`
- Purpose: Fetch recent transactions for the authenticated user, including linked students for parents.

Parameters:
- `pageNo` (default `1`)
- `pageSize` (default `10`)

Behavior:
- Calls `paymentService.getRecentTransactionsForAuthenticatedUser(pageNo, pageSize)`
- Returns success or no-transactions responses depending on content
- Maps access denied and generic errors into `R` error objects

---

### 4) Get transaction by ID
- Method: `GET`
- Endpoint: `/api/v1/transactions/{id}`
- Security: Bearer authentication
- Purpose: Fetch a single transaction by UUID.

Behavior:
- Fetches a transaction from `transactionService.getTransactionById(id)`
- Returns `R.ok(tx)` when found
- Returns a structured not-found response with `SchoolErrorCode.SE001` if no matching transaction exists

---

## Notes

- The controller is annotated with `@Hidden`, meaning it is excluded from Swagger UI visibility.
- All successful responses are wrapped in the custom `R<T>` response type.
- Error handling uses `SchoolErrorCode` values such as `SE001` and `SE003` for consistent API error responses.
- The controller relies on reactive Spring WebFlux (`Mono`) for non-blocking request handling.
