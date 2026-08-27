# Database Design

This document follows the supplied model: one **User** owns many **Products**,
and one **Product** has many **Media** records.

```text
User (1) ───── owns ───── (n) Product (1) ───── has ───── (n) Media
```

## User

**Service/database:** User Service / `user_service`
**Collection:** `users`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | String | Yes | Primary key. |
| `name` | String | Yes | Unique display/login name. |
| `email` | String | Yes | Unique email address. |
| `password` | String | Yes | BCrypt password hash; never returned by the API. |
| `role` | Enum | Yes | `ROLE_CLIENT` or `ROLE_SELLER`. |
| `avatar` | String | No | Media URL or Media ID for the user's avatar. |

**Indexes:** unique `name`; unique `email`.

## Product

**Service/database:** Product Service / `product_service`
**Collection:** `products`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | String | Yes | Primary key. |
| `name` | String | Yes | Product name. |
| `description` | String | No | Product description. |
| `price` | Decimal / integer minor units | Yes | Must be greater than zero. |
| `quantity` | Integer | Yes | Must be zero or greater. |
| `userId` / `sellerId` | String | Yes | ID of the owning seller from User Service. The current code names it `sellerId`. |
| `imageUrls` | Array<String> | No | References to images supplied by Media Service. |

**Indexes:** `sellerId` (or `userId`) for seller dashboards and ownership checks.

## Media

**Service/database:** Media Service / `media_service`
**Collection:** `media`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | String | Yes | Primary key. |
| `imagePath` | String | Yes | Object-storage key or public image URL. |
| `productId` | String | Yes | ID of the product this image belongs to. |

**Indexes:** `productId` for listing a product's images; unique `imagePath`.

## Relationship rules

- A seller can own zero or many products; each product has exactly one seller.
- A product can have zero or many images; each image belongs to one product.
- Since the services own separate databases, `sellerId`/`userId` and `productId`
  are logical references, not database foreign keys.
- Product Service stores only image references (`imageUrls`); Media Service stores
  image metadata and the actual object-storage location.
