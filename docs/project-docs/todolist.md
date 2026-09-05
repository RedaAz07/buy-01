# Project To-Do List

> Audit status: 2026-09-05. `[x]` means implemented and integrated locally. Unchecked items may be missing, partial, or implemented with known issues; see the notes below each item.

- [ ] 1. Microservices Setup
  - [x] Create a User Service for authentication, profiles, and `CLIENT` / `SELLER` roles.
  - [x] Create a Product Service for product CRUD operations and image references.
  - [x] Create a Media Service for image upload/download and validation, including a 2 MB limit.
  - [x] Configure Kafka for `avatar-uploaded-topic` and `user-deleted-topic` events.
  - [x] Configure Kafka for `product-deleted-topic` and `media-uploaded-topic` events (Product ↔ Media).
  - [x] Dockerize all Java services with Dockerfiles and a unified docker-compose.
  - [ ] Fix `UserEventProducer.sendUserDeletedEvent()` — the method exists, but no user-delete flow invokes it.

- [ ] 2. Enhanced Database Design
  - [x] Define and document the database design for each service.

- [ ] 3. API Development Enhancement
  - [ ] User Service
    - [x] Implement `POST /api/auth/register` with `CLIENT` or `SELLER` role selection.
    - [x] Implement `POST /api/auth/login` returning a JWT token.
    - [x] Implement `GET /api/users/me` returning the authenticated user profile.
    - [x] Implement `PUT /api/users/me` profile update endpoint. (`UserController.updateUser()` and `UserService.updateUser()` exist.)
    - [x] Delegate seller avatar upload/update to the Media Service through a User Service flow. (User Service exposes `/api/users/me/avatar` and forwards the multipart request and bearer token to Media Service.)
  - [ ] Product Service
    - [x] Implement public endpoints: `GET /api/products` and `GET /api/products/{id}`.
    - [x] Implement seller-only endpoints: `POST /api/products`, `PUT /api/products/{id}`, and `DELETE /api/products/{id}`.
    - [x] Enforce seller ownership — extract `sellerId` from JWT, not from request body.
    - [x] Enforce ownership on update/delete — sellers can only modify their own products.
    - [x] Associate `imageUrls[]` via Kafka — `media-uploaded-topic` auto-links uploaded images to products.
  - [ ] Media Service
    - [x] Implement seller-only `POST /api/media/images`.
    - [x] Validate MIME type (`image/*`) and a maximum file size of 2 MB.
    - [x] Implement `GET /api/media/images/{id}` returning the Cloudinary URL.
    - [x] Implement Media image deletion with ownership enforcement. (The endpoint deletes by URL at `DELETE /api/media/images?url=...`.)
    - [ ] Add `Cache-Control` and `ETag` headers to `GET /api/media/images/{id}`.
    - [x] Delete the image from Cloudinary on `DELETE` (`MediaService.deleteImageByUrl()` calls Cloudinary cleanup; the DELETE route contract still needs alignment.)
    - [ ] Add `GET /api/media/images?productId=X` to list images by product.
  - [ ] Expose `/actuator/health` from every service (only the API Gateway has the Actuator dependency/configuration.)
  - [ ] Configure the gateway to route external traffic and apply CORS and auth propagation. (Routes and CORS exist, but downstream services independently parse the bearer token instead of consuming `X-Authenticated-User`.)
  - [x] Add gateway rate limiting for authentication and media endpoints. (In-memory per-gateway-instance fixed windows: 10 auth requests/minute and 30 media requests/minute; use a shared store such as Redis for multi-instance production deployment.)

- [ ] 4. Front-End Development with Angular
  - [x] Build sign-in and sign-up pages with role selection.
  - [x] Allow sellers to upload and update their avatar. (Dashboard calls User Service; User Service delegates upload/delete to Media Service.)
  - [ ] Build a seller dashboard to create, edit, delete, preview, and remove product images. (Create/edit/delete/preview/remove exist in `OwnerActions`; update-image validation and end-to-end persistence still need work.)
  - [x] Show form validation messages, including required fields and `price > 0`.
  - [x] Build a public product grid/list without search or filtering requirements.
  - [ ] Build a dedicated media-management view for sellers' product images.
  - [ ] Validate file type and size in the UI before calling the API. (Avatar/create flows have checks, but `OwnerActions.onFilesSelected()` does not validate selected update files.)
  - [x] Use route guards (`AuthGuard`, `RoleGuard`), HTTP interceptors for tokens and `401`/`403` handling, Reactive Forms, and Angular Material or Bootstrap.

- [ ] 5. Authentication & Authorization
  - [ ] Use Spring Security with JWT at the gateway and propagate authentication downstream. (Gateway validation exists, but downstream services ignore the propagated user header and parse the original token independently.)
  - [x] Support `CLIENT` for browsing and `SELLER` for managing owned products/media.
  - [ ] Optionally add `ADMIN` for moderation.
  - [x] Enforce ownership checks in Product Service — `sellerId == auth.subject`.
  - [x] Enforce ownership checks in Media Service on DELETE. (The DELETE query-parameter route requires `SELLER`, and `MediaService.deleteImageByUrl()` verifies the authenticated owner.)

- [ ] 6. Error Handling & Validation
  - [ ] Return `400` for invalid input, invalid file type, or files that are too large. (Validation exists, but Media's multipart-size exception handler is commented out.)
  - [ ] Return `401` / `403` for unauthenticated or unauthorized requests. (Most security handlers exist, but Media DELETE route protection is mismatched.)
  - [ ] Return `404` for missing products/media or resources not found. (Service-level not-found errors exist, but Media endpoint/error handling is incomplete.)
  - [ ] Add global exception handlers to avoid unhandled `5xx` responses. (User/Product are more complete; Media is missing several standard handlers.)
  - [ ] Align Media Service exception handlers with User/Product Services (missing `HttpMessageNotReadableException` and `MethodArgumentTypeMismatchException` handlers.)
  - [ ] Show inline Angular form errors and toast/snackbar messages for upload failures, oversized files, and forbidden actions. (Many snackbars exist, but update/delete error behavior is inconsistent.)

- [ ] 7. Security Measures
  - [ ] Configure end-to-end HTTPS, such as with Let's Encrypt certificates.
  - [x] Hash and salt passwords with BCrypt in User Service; never expose passwords.
  - [x] Validate filenames and MIME types, sniff content headers, and reject non-image payloads. (Tika MIME sniffing exists; filename validation and update-upload UI validation are incomplete.)
  - [x] Ensure only the creating seller can modify or delete products and their images. (Product ownership checks and Media seller/owner checks are aligned.)
  - [x] Enforce allowed origins and headers through gateway CORS configuration.
  - [x] Add gateway rate limiting for authentication and media endpoints. (In-memory per-gateway-instance fixed windows; shared distributed limiting is still needed if multiple gateway instances are deployed.)

- [ ] 8. Code Quality & Housekeeping
  - [x] Add Dockerfiles for user-service, product-service, and media-service.
  - [ ] Create a unified docker-compose that starts all services and infrastructure together. (Compose exists, but required environment files and the gateway TLS keystore are missing from the workspace.)
  - [ ] Fix naming inconsistencies (`` → `ProductController`, `productRspons` → `ProductResponse`, `media-Service` → `media-service`). (Still `productController` / `productRspons` in `product-service`.)
  - [ ] Add API tests for all services. (Existing tests are mostly context-load tests; Product/Media have no meaningful API/security/Kafka coverage.)

## Audit Findings

- Product-image deletion still uses the Media Service URL-based DELETE contract; avatar upload/deletion now goes through User Service at `/api/users/me/avatar`.
- Only the gateway has Actuator configured; User, Product, Media, and Registry health endpoints are not exposed.
- Gateway rate limiting is configured in `RateLimitFilter` for authentication and media paths. It is in-memory and per gateway instance.
- Only `Backend/product-service/.env` was found, and no gateway `gateway-keystore.p12` was found; Docker startup cannot be considered verified.
- Product Service and Media Service Maven tests passed. The User Service context test fails without valid Mongo configuration. The Angular build could not run because frontend dependencies/`ng` are unavailable.