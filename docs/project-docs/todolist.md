# Project To-Do List

- [ ] 1. Microservices Setup
  - [x] Create a User Service for authentication, profiles, and `CLIENT` / `SELLER` roles.
  - [x] Create a Product Service for product CRUD operations and image references.
  - [x] Create a Media Service for image upload/download and validation, including a 2 MB limit.
  - [x] Configure Kafka for `avatar-uploaded-topic` and `user-deleted-topic` events.
  - [x] Configure Kafka for `product-deleted-topic` and `media-uploaded-topic` events (Product ↔ Media).
  - [x] Dockerize all Java services with Dockerfiles and a unified docker-compose.
  - [ ] Fix `UserEventProducer.sendUserDeletedEvent()` — currently defined but never invoked.

- [ ] 2. Enhanced Database Design
  - [x] Define and document the database design for each service.

- [ ] 3. API Development Enhancement
  - [ ] User Service
    - [x] Implement `POST /api/auth/register` with `CLIENT` or `SELLER` role selection.
    - [x] Implement `POST /api/auth/login` returning a JWT token.
    - [x] Implement `GET /api/users/me` returning the authenticated user profile.
    - [ ] Implement `PUT /api/users/me` profile update endpoint. (Only `GET /me` exists in `UserController`.)
    - [ ] Delegate seller avatar upload/update to the Media Service (trigger flow from User Service). (`avatar-uploaded-topic` consumer exists, but no endpoint triggers the avatar upload.)
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
    - [x] Implement `DELETE /api/media/images/{id}` with ownership enforcement.
    - [ ] Add `Cache-Control` and `ETag` headers to `GET /api/media/images/{id}`.
    - [ ] Delete the image from Cloudinary on `DELETE` (currently only removes the DB record).
    - [ ] Add `GET /api/media/images?productId=X` to list images by product.
  - [ ] Expose `/actuator/health` from every service (currently only the API Gateway has actuator).
  - [x] Configure the gateway to route external traffic and apply CORS and auth propagation.
  - [ ] Add gateway rate limiting for authentication and media endpoints.

- [ ] 4. Front-End Development with Angular
  - [x] Build sign-in and sign-up pages with role selection.
  - [ ] Allow sellers to upload and update their avatar.
  - [ ] Build a seller dashboard to create, edit, delete, preview, and remove product images. (Create/delete/preview work; `updetProduct()` in `product.ts` is an empty stub, so edit is not wired.)
  - [x] Show form validation messages, including required fields and `price > 0`.
  - [x] Build a public product grid/list without search or filtering requirements.
  - [ ] Build a dedicated media-management view for sellers' product images.
  - [ ] Validate file type and size in the UI before calling the API. (Only the `accept="image/*"` attribute; no size check in `onFileSelected`.)
  - [x] Use route guards (`AuthGuard`, `RoleGuard`), HTTP interceptors for tokens and `401`/`403` handling, Reactive Forms, and Angular Material or Bootstrap.

- [ ] 5. Authentication & Authorization
  - [x] Use Spring Security with JWT at the gateway and propagate authentication downstream.
  - [x] Support `CLIENT` for browsing and `SELLER` for managing owned products/media.
  - [ ] Optionally add `ADMIN` for moderation.
  - [x] Enforce ownership checks in Product Service — `sellerId == auth.subject`.
  - [x] Enforce ownership checks in Media Service on DELETE.

- [ ] 6. Error Handling & Validation
  - [x] Return `400` for invalid input, invalid file type, or files that are too large.
  - [x] Return `401` / `403` for unauthenticated or unauthorized requests.
  - [x] Return `404` for missing products/media or resources not found.
  - [x] Add global exception handlers to avoid unhandled `5xx` responses.
  - [ ] Align Media Service exception handlers with User/Product Services (missing `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `ApiException` handlers — Media has `ApiException` but not the other two).
  - [ ] Show inline Angular form errors and toast/snackbar messages for upload failures, oversized files, and forbidden actions. (Create form + upload snackbars exist; update modal has none.)

- [ ] 7. Security Measures
  - [ ] Configure end-to-end HTTPS, such as with Let's Encrypt certificates.
  - [x] Hash and salt passwords with BCrypt in User Service; never expose passwords.
  - [x] Validate filenames and MIME types, sniff content headers, and reject non-image payloads.
  - [x] Ensure only the creating seller can modify or delete products and their images.
  - [x] Enforce allowed origins and headers through gateway CORS configuration.
  - [ ] Add gateway rate limiting for authentication and media endpoints.

- [ ] 8. Code Quality & Housekeeping
  - [x] Add Dockerfiles for user-service, product-service, and media-service.
  - [x] Create a unified docker-compose that starts all services and infrastructure together.
  - [ ] Fix naming inconsistencies (`` → `ProductController`, `productRspons` → `ProductResponse`, `media-Service` → `media-service`). (Still `productController` / `productRspons` in `product-service`.)
  - [ ] Add API tests for all services. (Only trivial context-load tests in user-service, api-gateway, registry.)