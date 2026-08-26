# Project To-Do List

- [ ] 1. Microservices Setup
  - [x] Create a User Service for authentication, profiles, and `CLIENT` / `SELLER` roles.
  - [x] Create a Product Service for product CRUD operations and image references.
  - [x] Create a Media Service for image upload/download and validation, including a 2 MB limit.
  - [x] Configure Kafka (optional, recommended) for `PRODUCT_CREATED` and `IMAGE_UPLOADED` events—for audit trails, cache invalidation, and thumbnail generation.

- [ ] 2. Enhanced Database Design
  - [x] Define and document the database design for each service.

- [ ] 3. API Development Enhancement
  - [ ] User Service
    - [x] Implement `POST /auth/register` with `CLIENT` or `SELLER` role selection.
    - [x] Implement `POST /auth/login` returning a JWT/OAuth2 token.
    - [ ] Implement `GET /me` and `PUT /me` profile endpoints.
    - [ ] Delegate seller avatar upload/update to the Media Service.
  - [ ] Product Service
    - [x] Implement public endpoints: `GET /products` and `GET /products/{id}`.
    - [x] Implement seller-only endpoints: `POST /products`, `PUT /products/{id}`, and `DELETE /products/{id}`.
    - [ ] Enforce seller ownership for product updates and deletions.
    - [ ] Associate `imageUrls[]`; upload images through Media Service before linking them to products.
  - [ ] Media Service
    - [x] Implement seller-only `POST /media/images`.
    - [x] Validate MIME type (`image/*`) and a maximum file size of 2 MB.
    - [ ] Implement `GET /media/images/{id}` with appropriate caching headers.
    - [ ] Optionally implement `DELETE /media/images/{id}` and enforce media ownership.
  - [ ] Expose `/actuator/health` from every service.
  - [ ] Configure the gateway to route external traffic and apply CORS, auth propagation, and optional rate limiting.

- [ ] 4. Front-End Development with Angular
  - [ ] Build sign-in and sign-up pages with role selection.
  - [ ] Allow sellers to upload and update their avatar.
  - [ ] Build a seller dashboard to create, edit, delete, preview, and remove product images.
  - [ ] Show form validation messages, including required fields and `price > 0`.
  - [ ] Build a public product grid/list without search or filtering requirements.
  - [ ] Build a dedicated media-management view for sellers' product images.
  - [ ] Validate file type and size in the UI before calling the API.
  - [ ] Use route guards (`AuthGuard`, `RoleGuard`), HTTP interceptors for tokens and `401`/`403` handling, Reactive Forms, and Angular Material or Bootstrap.

- [ ] 5. Authentication & Authorization
  - [x] Use Spring Security with JWT or OAuth2 at the gateway and propagate authentication downstream.
  - [ ] Support `CLIENT` for browsing and `SELLER` for managing owned products/media.
  - [ ] Optionally add `ADMIN` for moderation.
  - [ ] Enforce ownership checks in Product and Media services: `sellerId == auth.subject`.

- [ ] 6. Error Handling & Validation
  - [ ] Return `400` for invalid input, invalid file type, or files that are too large.
  - [ ] Return `401` / `403` for unauthenticated or unauthorized requests.
  - [ ] Return `404` for missing products/media or resources not owned by the requester.
  - [ ] Add global exception handlers to avoid unhandled `5xx` responses.
  - [ ] Show inline Angular form errors and toast/snackbar messages for upload failures, oversized files, and forbidden actions.

- [ ] 7. Security Measures
  - [ ] Configure end-to-end HTTPS, such as with Let's Encrypt certificates.
  - [x] Hash and salt passwords with BCrypt in User Service; never expose passwords.
  - [x] Validate filenames and MIME types, sniff content headers, and reject non-image payloads.
  - [ ] Ensure only the creating seller can modify or delete products and their images.
  - [x] Enforce allowed origins and headers through gateway CORS configuration.
  - [ ] Optionally add gateway rate limiting for authentication and media endpoints.
