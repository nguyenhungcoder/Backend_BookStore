# Admin APIs Quick Reference

## API Endpoints Summary

| Module      | HTTP Method | Endpoint                  | Purpose                     | Response       |
| ----------- | ----------- | ------------------------- | --------------------------- | -------------- |
| **BOOK**    | POST        | `/api/books`              | Create book                 | 201 Created    |
|             | PUT         | `/api/books/{id}`         | Update book                 | 200 OK         |
|             | DELETE      | `/api/books/{id}`         | Delete book                 | 204 No Content |
| **ORDER**   | GET         | `/api/orders`             | List all orders (paginated) | 200 OK         |
|             | PUT         | `/api/orders/{id}/status` | Update order status         | 200 OK         |
| **VOUCHER** | POST        | `/api/vouchers`           | Create voucher              | 201 Created    |
|             | PUT         | `/api/vouchers/{id}`      | Update voucher              | 200 OK         |
|             | DELETE      | `/api/vouchers/{id}`      | Delete voucher              | 204 No Content |
| **USER**    | GET         | `/api/users`              | List all users (paginated)  | 200 OK         |
|             | PUT         | `/api/users/{id}/status`  | Update user status          | 200 OK         |

---

## Authentication & Authorization

All endpoints require:

- ✓ Valid JWT Token in `Authorization: Bearer <token>` header
- ✓ User role must be `ADMIN`
- ✓ User status must be `ACTIVE` (checked by Spring Security)

**Error Responses**:

- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Valid JWT but user is not admin
- `404 Not Found` - Resource doesn't exist
- `400 Bad Request` - Validation error

---

## Request/Response Examples

### 1. Create Book

**Request**:

```
POST /api/books
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Clean Code",
  "description": "A Handbook of Agile Software Craftsmanship",
  "coverImage": "https://...",
  "isbn": "978-0132350884",
  "publicationYear": 2008,
  "language": "English",
  "pageCount": 464,
  "weight": 0.68,
  "dimensions": "22.5 x 16.5 x 3.5 cm",
  "price": 45.99,
  "stockQuantity": 100,
  "publisherId": 1,
  "status": "ACTIVE",
  "categoryIds": [1, 2, 3],
  "authorIds": [1, 2]
}
```

**Response** (201 Created):

```json
{
  "bookId": 1,
  "title": "Clean Code",
  "price": 45.99,
  "stockQuantity": 100,
  "status": "ACTIVE",
  "createdAt": "2024-07-20T10:30:00",
  "updatedAt": "2024-07-20T10:30:00",
  "categories": [{ "categoryId": 1, "name": "Programming" }],
  "authors": [{ "authorId": 1, "name": "Robert Martin" }]
}
```

---

### 2. Get All Orders

**Request**:

```
GET /api/orders?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <token>
```

**Response** (200 OK):

```json
{
  "content": [
    {
      "orderId": 1,
      "user": {
        "userId": 5,
        "username": "johndoe",
        "email": "john@example.com",
        "phone": "0123456789",
        "fullName": "John Doe",
        "role": "CUSTOMER",
        "status": "ACTIVE"
      },
      "address": {
        "addressId": 1,
        "receiverName": "John Doe",
        "province": "Hồ Chí Minh",
        "district": "Quận 1",
        "ward": "Phường Bến Nghé",
        "detailAddress": "123 Main St"
      },
      "totalAmount": 299.99,
      "discountAmount": 30.0,
      "shippingFee": 10.0,
      "status": "PENDING",
      "createdAt": "2024-07-20T09:15:00",
      "updatedAt": "2024-07-20T09:15:00"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 150,
  "totalPages": 8
}
```

---

### 3. Update Order Status

**Request**:

```
PUT /api/orders/1/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

**Response** (200 OK):

```json
{
  "orderId": 1,
  "status": "SHIPPED",
  "updatedAt": "2024-07-20T10:45:00"
}
```

---

### 4. Create Voucher

**Request**:

```
POST /api/vouchers
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "SUMMER2024",
  "description": "20% off on all books",
  "discountType": "PERCENT",
  "discountValue": 20,
  "minOrderValue": 100.00,
  "maxDiscountAmount": 50.00,
  "usageLimit": 1000,
  "startDate": "2024-06-01T00:00:00",
  "endDate": "2024-08-31T23:59:59",
  "status": "ACTIVE"
}
```

**Response** (201 Created):

```json
{
  "voucherId": 1,
  "code": "SUMMER2024",
  "description": "20% off on all books",
  "discountType": "PERCENT",
  "discountValue": 20,
  "minOrderValue": 100.0,
  "maxDiscountAmount": 50.0,
  "usageLimit": 1000,
  "usedCount": 0,
  "status": "ACTIVE"
}
```

---

### 5. Get All Users

**Request**:

```
GET /api/users?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <token>
```

**Response** (200 OK):

```json
{
  "content": [
    {
      "userId": 1,
      "username": "johndoe",
      "email": "john@example.com",
      "phone": "0123456789",
      "fullName": "John Doe",
      "avatar": "https://...",
      "role": "CUSTOMER",
      "status": "ACTIVE"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 250,
  "totalPages": 13
}
```

---

### 6. Update User Status (Lock Account)

**Request**:

```
PUT /api/users/5/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "BLOCKED"
}
```

**Response** (200 OK):

```json
{
  "userId": 5,
  "username": "johndoe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "status": "BLOCKED"
}
```

---

## Book Management Details

### Book Categories (Many-to-Many)

- Categories are managed via `categoryIds` array in request
- Existing book categories are replaced on update
- Join table `book_category` is automatically managed

### Book Authors (Many-to-Many)

- Authors are managed via `authorIds` array in request
- Existing book authors are replaced on update
- Join table `book_author` is automatically managed

### Status Values

- `ACTIVE` - Book is available for purchase
- `INACTIVE` - Book is hidden from customers
- `OUT_OF_STOCK` - Book is out of stock but may exist

---

## Pagination Query Parameters

For GET requests that return paginated results:

```
page      - Zero-indexed page number (default: 0)
size      - Number of items per page (default: 20)
sort      - Sort field and direction (default: createdAt,desc)
```

**Examples**:

```
GET /api/orders?page=0&size=50
GET /api/users?page=1&size=25&sort=username,asc
GET /api/orders?page=0&size=20&sort=createdAt,desc
```

---

## Common Validation Errors

| Field               | Error                | Solution                      |
| ------------------- | -------------------- | ----------------------------- |
| title               | Cannot be blank      | Provide non-empty title       |
| price               | Must be positive     | Price must be > 0             |
| stockQuantity       | Must be positive     | Stock must be >= 0            |
| categoryIds         | Cannot be null/empty | Include at least one category |
| authorIds           | Cannot be null/empty | Include at least one author   |
| publisherId         | Publisher not found  | Verify publisher exists       |
| code (voucher)      | Already exists       | Use unique voucher code       |
| startDate > endDate | Invalid date range   | Start must be before end date |

---

## Best Practices

1. **Always include valid JWT token** - All endpoints require authentication
2. **Use pagination** - For large datasets, always use page/size parameters
3. **Check status codes** - Proper error handling based on HTTP status
4. **Validate input** - Client-side validation before sending requests
5. **Handle timestamps** - `createdAt` and `updatedAt` are server-managed
6. **Many-to-many updates** - Categories/Authors are fully replaced, not merged

---

## Troubleshooting

| Issue                | Cause                     | Solution                                   |
| -------------------- | ------------------------- | ------------------------------------------ |
| 401 Unauthorized     | Missing token             | Add `Authorization: Bearer <token>` header |
| 403 Forbidden        | Not admin user            | Ensure user has ADMIN role                 |
| 404 Not Found        | Resource doesn't exist    | Verify correct ID in URL                   |
| 400 Bad Request      | Invalid input             | Check request body format                  |
| Duplicate code error | Voucher code exists       | Use different voucher code                 |
| Foreign key error    | Referenced entity missing | Verify publisher/category/author IDs exist |
