- [ ] Create the **User Service**
  - [ ] Define `User` entity: id, name, email, password, role,avatar.
  - [ ] Add roles: `CLIENT` and `SELLER`.
  - [ ] Build registration and login endpoints.
  - [ ] Secure passwords with BCrypt.
  - [ ] Add profile read/update endpoints.
  - [ ] Add role validation and authorization rules.
  - [ ] Create its own database/schema.

- [ ] Create the **Product Service**
  - [ ] Define `Product` entity: id, name, description, price, stock, sellerId, image URLs/IDs.
  - [ ] Implement product CRUD endpoints.
  - [ ] Ensure only the owning `SELLER` can update or delete their products.
  - [ ] Store image references only—do not upload image files here.
  - [ ] Add validation for required fields, price, and stock.
  - [ ] Create its own database/schema.

- [ ] Create the **Media Service**
  - [ ] Create upload endpoint, e.g. `POST /media`.
  - [ ] Accept only allowed image formats such as JPEG, PNG, and WebP.
  - [ ] Reject files larger than **2 MB**.
  - [ ] Generate a unique filename/ID for each file.
  - [ ] Store the file locally or in object storage.
  - [ ] Return an image ID/URL after upload.
  - [ ] Create download endpoint, e.g. `GET /media/{id}`.
  - [ ] Add delete endpoint if sellers need to remove product images.

- [ ] Connect the services
  - [ ] Configure API Gateway routes for `/users/**`, `/products/**`, and `/media/**`.
  - [ ] Have Product Service save the image URL/ID returned by Media Service.
  - [ ] Pass authenticated user identity from the gateway to protected services.
  - [ ] Add centralized error responses and request validation.

- [ ] Test
  - [ ] Test registration/login as `CLIENT` and `SELLER`.
  - [ ] Test that clients cannot modify seller products.
  - [ ] Test image upload with valid images.
  - [ ] Test rejection of invalid formats and files over 2 MB.
  - [ ] Test complete flow: upload image → create product with image reference → retrieve product/image.