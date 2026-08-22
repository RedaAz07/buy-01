# Database Design

Each microservice owns its database. Services must never read or write another
service's collections directly; they exchange identifiers and communicate through
their APIs or events.

## 1. User Service

**Database:** MongoDB database `user_service`  
**Collection:** `users`

| Field | Type | Rules / purpose |
| --- | --- | --- |
| `_id` | ObjectId / String | Primary identifier. Exposed as `id`. |
| `name` | String | Required; unique; 3–15 alphanumeric characters. |
| `email` | String | Required; unique; normalized to lowercase. |
| `password` | String | Required BCrypt hash only; never return it in an API response. |
| `role` | String | Required enum: `ROLE_CLIENT` or `ROLE_SELLER`. |
| `avatarMediaId` | String, nullable | Media Service image identifier for the user's avatar. |
| `createdAt` | DateTime | Set when the user is created. |
| `updatedAt` | DateTime | Updated whenever profile data changes. |

**Indexes**

- Unique index on `name`.
- Unique index on `email`.
- Index on `role` only if role-based administration/reporting needs it.

`avatarMediaId` is a reference, not a database foreign key. The Media Service
validates that the seller owns the referenced media before accepting an avatar
update.

## 2. Product Service

**Database:** MongoDB database `product_service`  
**Collection:** `products`

| Field | Type | Rules / purpose |
| --- | --- | --- |
| `_id` | ObjectId / String | Primary identifier. Exposed as `id`. |
| `name` | String | Required product name. |
| `description` | String | Optional product description. |
| `price` | Decimal128 | Required and strictly greater than zero; do not use floating point for currency. |
| `quantity` | Integer | Required; zero or greater. |
| `sellerId` | String | Required User Service user ID of the creating seller. |
| `imageMediaIds` | Array<String> | Ordered references to Media Service images. |
| `createdAt` | DateTime | Set when created. |
| `updatedAt` | DateTime | Updated when product data changes. |

**Indexes**

- Index on `sellerId` for a seller dashboard and ownership checks.
- Index on `{ sellerId: 1, createdAt: -1 }` for recent seller products.
- Index on `{ createdAt: -1 }` for the public product list.

`sellerId` is immutable after creation and must come from the authenticated token,
never from a request body. `imageMediaIds` only stores Media Service IDs; image
files are not stored in this collection.

## 3. Media Service

**Database:** MongoDB database `media_service`  
**Collection:** `media`

The database stores image metadata. Store image bytes in object storage (for
example, MinIO or S3) and keep only the object key in MongoDB. This permits
efficient downloads, CDN caching, and future thumbnail generation.

| Field | Type | Rules / purpose |
| --- | --- | --- |
| `_id` | ObjectId / String | Primary identifier. Exposed as `id`. |
| `ownerId` | String | Required User Service seller ID that uploaded the media. |
| `objectKey` | String | Required; unique object-storage key, not the original filename. |
| `originalFilename` | String | Sanitized display/audit name; never use as a filesystem path. |
| `contentType` | String | Required verified image MIME type such as `image/jpeg`, `image/png`, or `image/webp`. |
| `sizeBytes` | Long | Required; must be at most `2,097,152` bytes. |
| `checksum` | String | SHA-256 checksum of the uploaded content. |
| `createdAt` | DateTime | Set when uploaded. |
| `deletedAt` | DateTime, nullable | Supports soft deletion/audit; omit this field for active media. |

**Indexes**

- Unique index on `objectKey`.
- Index on `{ ownerId: 1, createdAt: -1 }` for media management.
- Index on `checksum` if duplicate-upload detection is required.
- Optional TTL index on `deletedAt` for delayed physical cleanup of soft-deleted
  media.

The upload flow validates the MIME type and file signature before storing an
object. The Media Service authorizes reads/deletes using `ownerId`; Product
Service retains only the media IDs it is allowed to link.

## Cross-service rules

- No cross-database foreign keys or joins.
- All service-to-service references use stable string IDs.
- Deleting media that is referenced by a product should be rejected, or the
  Product Service must remove the reference through an explicit workflow/event.
- Each service owns its backup, migration, retention, and access policy.
- Future Kafka events should contain IDs and metadata only—never password hashes
  or image bytes.
