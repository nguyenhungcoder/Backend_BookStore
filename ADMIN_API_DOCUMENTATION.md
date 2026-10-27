# Admin API Documentation - Step 9

## Overview

This document outlines all the admin management APIs added to support the Dashboard Admin functionality. All admin APIs are protected with `@PreAuthorize("hasRole('ADMIN')")` annotation.

## Security Configuration

- **Enabled Method-Level Security**: Added `@EnableMethodSecurity(prePostEnabled = true)` to `SecurityConfig.java`
- **Authentication**: All admin endpoints require valid JWT token with ROLE_ADMIN
- **Authorization**: Admin role validation is enforced at method level using Spring Security

---

## 1. Book Management APIs

### Create Book

- **Method**: `POST /api/books`
- **Access**: Admin only
- **Request Body**:

```json
{
  "title": "string (required)",
  "description": "string",
  "coverImage": "string (URL)",
  "isbn": "string (required)",
  "publicationYear": 2024,
  "language": "string",
  "pageCount": 300,
  "weight": 0.5,
  "dimensions": "string",
  "price": 299.99,
  "stockQuantity": 100,
  "publisherId": 1,
  "status": "ACTIVE|INACTIVE",
  "categoryIds": [1, 2, 3],
  "authorIds": [1, 2]
}
```

- **Response**: Returns created BookResponseDTO with all details

### Update Book

- **Method**: `PUT /api/books/{id}`
- **Access**: Admin only
- **Request Body**: Same as Create (all fields optional)
- **Response**: Returns updated BookResponseDTO

### Delete Book

- **Method**: `DELETE /api/books/{id}`
- **Access**: Admin only
- **Response**: 204 No Content

**Features**:

- Automatically handles many-to-many relationships with categories and authors
- Updates `updatedAt` timestamp
- Validates publisher existence
- Creates/updates join table records for `book_category` and `book_author`

---

## 2. Order Management APIs

### Get All Orders (with Pagination)

- **Method**: `GET /api/orders`
- **Access**: Admin only
- **Query Parameters**:
  - `page`: 0 (default)
  - `size`: 20 (default)
  - `sort`: createdAt,desc (default)
- **Response**: Returns paginated AdminOrderResponse with user and address details

**Example**: `GET /api/orders?page=0&size=20&sort=createdAt,desc`

### Update Order Status

- **Method**: `PUT /api/orders/{id}/status`
- **Access**: Admin only
- **Request Body**:

```json
{
  "status": "PENDING|CONFIRMED|SHIPPING|DELIVERED|CANCELLED"
}
```

- **Response**: Returns updated AdminOrderResponse

**Features**:

- Updates order status and `updatedAt` timestamp
- Returns user information (username, email, phone, fullName)
- Returns address information (receiver, province, district, ward, detail)
- Includes order totals (total, discount, shipping)

---

## 3. Voucher Management APIs

### Create Voucher

- **Method**: `POST /api/vouchers`
- **Access**: Admin only
- **Request Body**:

```json
{
  "code": "SUMMER2024",
  "description": "Summer discount",
  "discountType": "PERCENT|FIXED",
  "discountValue": 10.0,
  "minOrderValue": 100.0,
  "maxDiscountAmount": 50.0,
  "usageLimit": 1000,
  "startDate": "2024-06-01T00:00:00",
  "endDate": "2024-08-31T23:59:59",
  "status": "ACTIVE|INACTIVE"
}
```

- **Response**: Returns created VoucherResponse

### Update Voucher

- **Method**: `PUT /api/vouchers/{id}`
- **Access**: Admin only
- **Request Body**: Same as Create (all fields optional)
- **Response**: Returns updated VoucherResponse

### Delete Voucher

- **Method**: `DELETE /api/vouchers/{id}`
- **Access**: Admin only
- **Response**: 204 No Content

**Features**:

- Validates voucher code uniqueness
- Converts code to uppercase
- Validates date ranges (start < end)
- Prevents duplicate codes
- Tracks usage count automatically

---

## 4. User Management APIs

### Get All Users (with Pagination)

- **Method**: `GET /api/users`
- **Access**: Admin only
- **Query Parameters**:
  - `page`: 0 (default)
  - `size`: 20 (default)
  - `sort`: createdAt,desc (default)
- **Response**: Returns paginated UserResponse with full user details

**Example**: `GET /api/users?page=0&size=20&sort=createdAt,desc`

### Update User Status (Lock/Unlock Account)

- **Method**: `PUT /api/users/{id}/status`
- **Access**: Admin only
- **Request Body**:

```json
{
  "status": "ACTIVE|INACTIVE|BLOCKED"
}
```

- **Response**: Returns updated UserResponse

**Features**:

- Updates user account status
- Status reflects in UserDetails for authentication
- Can lock/unlock accounts for customer management

---

## DTOs Created

### Request DTOs

- `CreateBookRequest.java` - For creating new books
- `UpdateBookRequest.java` - For updating books (all fields optional)
- `UpdateOrderStatusRequest.java` - For updating order status
- `CreateVoucherRequest.java` - For creating vouchers
- `UpdateVoucherRequest.java` - For updating vouchers
- `UpdateUserStatusRequest.java` - For updating user status

### Response DTOs

- `AdminOrderResponse.java` - Admin view of orders with user & address details

---

## Repositories Created

- `CategoryRepository.java` - JPA repository for Category entity
- `AuthorRepository.java` - JPA repository for Author entity
- `PublisherRepository.java` - JPA repository for Publisher entity

---

## Key Implementation Details

### Entity Relationships

- **Book**: Many-to-Many with Categories and Authors
  - Join table: `book_category` (book_id, category_id)
  - Join table: `book_author` (book_id, author_id)
- **Order**: Includes user, address, and optional voucher
- **Voucher**: Tracks usage count and date ranges
- **User**: Has status field for account lock/unlock

### Transaction Management

- All create/update/delete operations use `@Transactional`
- Read operations use `@Transactional(readOnly = true)`
- Automatic timestamp updates (createdAt, updatedAt)

### Error Handling

- `ResourceNotFoundException` - When entity not found
- `BadRequestException` - For validation errors
- HTTP 404 for not found resources
- HTTP 400 for bad requests
- HTTP 201 for successful creates
- HTTP 204 for successful deletes

---

## Usage Example

### Create a Book

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -d '{
    "title": "Clean Code",
    "isbn": "978-0132350884",
    "publicationYear": 2008,
    "price": 45.99,
    "stockQuantity": 50,
    "publisherId": 1,
    "status": "ACTIVE",
    "categoryIds": [1, 2],
    "authorIds": [1]
  }'
```

### Get All Orders

```bash
curl -X GET "http://localhost:8080/api/orders?page=0&size=20" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

### Update Order Status

```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -d '{
    "status": "SHIPPED"
  }'
```

### Create Voucher

```bash
curl -X POST http://localhost:8080/api/vouchers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -d '{
    "code": "SUMMER2024",
    "discountType": "PERCENT",
    "discountValue": 15,
    "status": "ACTIVE",
    "startDate": "2024-06-01T00:00:00",
    "endDate": "2024-08-31T23:59:59"
  }'
```

### Get All Users

```bash
curl -X GET "http://localhost:8080/api/users?page=0&size=20" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

### Update User Status

```bash
curl -X PUT http://localhost:8080/api/users/5/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -d '{
    "status": "BLOCKED"
  }'
```

---

## Validation Rules

### Book

- Title: Required, non-blank
- ISBN: Required, non-blank
- Price: Required, must be positive
- Stock Quantity: Required, must be positive
- Category IDs: Required, non-empty
- Author IDs: Required, non-empty
- Publisher ID: Required

### Voucher

- Code: Required, unique (case-insensitive)
- Discount Value: Required, must be positive
- Start Date: Required
- End Date: Required, must be after start date
- Status: Required

### Order Status

- Status: Required, non-blank

### User Status

- Status: Required, non-blank

---

## Middleware/Interceptor Notes

All admin endpoints are protected by:

1. JWT Authentication via `JwtAuthenticationFilter`
2. Role Authorization via `@PreAuthorize("hasRole('ADMIN')")`
3. Spring Security method-level security (enabled in SecurityConfig)

The authorization chain:

1. JWT token is validated
2. UserPrincipal is extracted with role information
3. Method security checks if user has ROLE_ADMIN
4. If not authorized, returns 403 Forbidden
