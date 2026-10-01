# Buy Platform

Buy is a marketplace application built as a collection of Spring Boot
microservices and an Angular frontend. Users can register as clients or
sellers. Clients browse products, while sellers manage their products and
associated images.

The project uses REST for request/response operations and Apache Kafka for
events that cross service boundaries. Each business service owns its own
MongoDB database. The API Gateway is the only public backend entry point.

## Architecture

```mermaid
flowchart LR
		Browser[Angular frontend\nlocalhost:4200] -->|HTTPS REST| Gateway[API Gateway\n8443]
		Gateway -->|Eureka load-balanced routes| User[User Service]
		Gateway -->|Eureka load-balanced routes| Product[Product Service]
		Gateway -->|Eureka load-balanced routes| Media[Media Service]

		Registry[Netflix Eureka\n8761] -. service discovery .-> Gateway
		Registry -. service discovery .-> User
		Registry -. service discovery .-> Product
		Registry -. service discovery .-> Media

		User --> UserDB[(User MongoDB)]
		Product --> ProductDB[(Product MongoDB)]
		Media --> MediaDB[(Media MongoDB)]
		Media --> Cloudinary[Cloudinary]

		User <--> Kafka[(Apache Kafka)]
		Product <--> Kafka
		Media <--> Kafka
		Gateway --> Redis[(Redis)]
```


## CI/CD: Jenkins, Docker, and ngrok

The repository includes a Jenkins controller definition in `jenkins/` and the
pipeline definition in `Jenkinsfile`. Jenkins receives a GitHub push webhook
through an ngrok HTTPS tunnel, checks out the repository, builds and tests the
application, then deploys the root Docker Compose stack through the mounted
container-engine socket.

```mermaid
flowchart TD
    subgraph DevEnv["1. Local Development"]
        Dev["Developer\n(ranniz / RedaAz07)"] -->|git push origin main| GitHub["GitHub Repository\n(RedaAz07/buy-01)"]
    end

    subgraph TriggerLayer["2. Trigger & Tunneling"]
        GitHub -->|Webhook Payload| Ngrok["ngrok Tunnel\n(Port 8090)"]
        Ngrok -->|/github-webhook/| JenkinsController["Jenkins Controller\n(Jenkins Docker Container)"]
    end

    subgraph PipelineExecution["3. Jenkins CI/CD Pipeline (Jenkinsfile)"]
        JenkinsController --> S1

        subgraph S1["Stage 1: Prepare Secrets"]
            Vault[("Jenkins Vault\nCredentials")] -.->|Inject| Secrets["Copy .env & SSL Certs\n- gateway-keystore.p12\n- cert.pem & key.pem"]
        end

        S1 --> S2

        subgraph S2["Stage 2: Build Stage"]
            B1["Spring Boot Microservices\n./mvnw clean package -DskipTests\n(registry, user, product, media, gateway)"]
            B2["Angular Frontend\nnpm ci && npm run build"]
        end

        S2 --> S3

        subgraph S3["Stage 3: Test Stage (Quality Gate)"]
            T1["Backend Tests\n./mvnw test"]
            T2["Frontend Tests\nnpm test"]
        end

        S3 --> S4

        subgraph S4["Stage 4: Deploy & Health Check"]
            D1["docker compose up -d --build"]
            D2{"Health Check\n(Containers Running?)"}
            D1 --> D2
        end

        subgraph Rollback["Automated Rollback (On Failure)"]
            R1["git checkout HEAD~1"]
            R2["Re-inject Secrets & Re-build"]
            R3["docker compose up -d"]
            R1 --> R2 --> R3
        end

        D2 -->|Exited / Crash| Rollback
    end

    subgraph TargetHost["4. Deployment Target (Docker Host)"]
        D1 -->|via /var/run/docker.sock| HostDocker["Docker Engine (Rootless)"]
        HostDocker --> Containers["Active Stack:\n- Microservices & Gateway\n- Angular Frontend (Nginx)\n- Databases & Infra (Mongo, Kafka, Redis)"]
    end

    subgraph PostExecution["5. Post-Build Notifications"]
        D2 -->|All Passed| MailSuccess["Email: SUCCESS\n(smtp.gmail.com:587 TLS)"]
        Rollback --> MailFail["Email: FAILED & Rolled Back\n(smtp.gmail.com:587 TLS)"]
    end
```

### What each component does

| Component | Location / port | Purpose |
| --- | --- | --- |
| Jenkins controller | `jenkins/docker-compose.yml`, `http://localhost:8090` | Runs the pipeline and exposes the GitHub webhook endpoint. |
| Jenkins image | `jenkins/Dockerfile` | Adds Docker CLI, Docker Compose, Buildx, Node.js, and npm to the Jenkins LTS image. |
| Jenkins state | `jenkins_home` Docker volume | Persists Jenkins configuration, plugins, jobs, and build metadata. |
| Container-engine socket | Mounted at `/var/run/docker.sock` | Lets pipeline commands create and manage the deployment containers on the host engine. |
| ngrok | `https://<public-id>.ngrok...` → local port `8090` | Gives GitHub a public HTTPS route to Jenkins while developing from a local machine. |
| GitHub webhook | `POST /github-webhook/` | Triggers the Jenkins job after a push. |

The Jenkins Compose file currently maps host ports `8090:8080` and
`50000:50000`. Its socket source is configured for a rootless Podman socket:
`/run/user/67984/podman/podman.sock`. If the host uses Docker or a different
rootless user, update only that source path before starting Jenkins.

### One-time Jenkins setup

1. Start the controller from the repository root:

   ```bash
   cd jenkins
   docker compose up -d --build
   docker compose ps
   ```

2. Open `http://localhost:8090` and complete the Jenkins setup wizard. To get
   the initial administrator password:

   ```bash
   docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```

3. Install the plugins required by this pipeline: Pipeline, Git, GitHub,
   Credentials Binding, and a mail plugin configured for the `mail` pipeline
   step. Configure SMTP before relying on build emails.

4. Add these **Secret file** credentials in Jenkins. The IDs must match the
   `Jenkinsfile` exactly; do not commit any of these files.

   | Credential ID | File to upload | Used for |
   | --- | --- | --- |
   | `buy01-env` | Deployment `.env` file | MongoDB, JWT, Cloudinary, service ports, and gateway password values. |
   | `gateway-keystore.p12` | Gateway PKCS12 keystore | HTTPS for the API gateway. |
   | `buy01-frontend-cert` | `cert.pem` | HTTPS certificate for the frontend container. |
   | `buy01-frontend-key` | `key.pem` | Private key for the frontend certificate. |

5. Create a Pipeline job that uses Pipeline script from SCM, points to this
   repository and branch, and uses `Jenkinsfile` as the script path. Enable
   **GitHub hook trigger for GITScm polling**. The pipeline itself also declares
   `githubPush()`.

The deployment `.env` needs the variables referenced by the root
`docker-compose.yml`: `API_GATEWAY_SSL_PASS`, `JWT_SECRET`, `JWT_EXPIRATION`,
the `USER_*`, `MEDIA_*`, and `PRODUCT_*` MongoDB values, service-port values,
and the three `MEDIA_CLOUDINARY_*` values. Keep this file in Jenkins
credentials, not in Git.

### Connect GitHub with ngrok

1. Install ngrok and authenticate it once on the machine running Jenkins:

   ```bash
   ngrok config add-authtoken YOUR_TOKEN
   ```

2. In another terminal, publish the Jenkins HTTP port:

   ```bash
   ngrok http 8090
   ```

3. Copy the HTTPS forwarding URL shown by ngrok. In the GitHub repository go
   to **Settings → Webhooks → Add webhook** and set:

   | Setting | Value |
   | --- | --- |
   | Payload URL | `https://<ngrok-host>/github-webhook/` |
   | Content type | `application/json` |
   | Events | `Just the push event` |
   | Active | Enabled |

4. Save the webhook, use GitHub's **Recent Deliveries** to confirm a `2xx`
   response, then push a commit. Jenkins should start one build for that push.

ngrok URLs change whenever a free tunnel is restarted. Update the GitHub
webhook when that happens, or use a reserved domain if the ngrok plan supports
one. Treat the public URL as an ingress point: do not expose Jenkins without
authentication, and restrict the webhook with a GitHub webhook secret if the
Jenkins GitHub integration is configured to verify one.

### Pipeline behavior

For each push, Jenkins runs the following sequence:

1. **Prepare Secrets** — copies the Jenkins secret files into the checked-out
   workspace for the build.
2. **Build** — packages the five Spring Boot services with Maven and creates
   the production Angular build.
3. **Test** — runs Maven tests for every backend service and the frontend test
   command; JUnit reports are published when available.
4. **Deploy & Health Check** — runs `docker compose up -d --build` from the
   repository root, waits 25 seconds, and fails when Compose reports an exited,
   dead, restarting, or unhealthy container.
5. **Rollback on deployment failure** — if Jenkins knows a previous successful
   commit, it stops the stack, checks out that commit, restores the secret
   files, and rebuilds the stable version.
6. **Notification and cleanup** — sends a success or failure email, then
   removes the Jenkins workspace with `cleanWs()`.

`disableConcurrentBuilds()` prevents two deployments from running at the same
time. The controller runs Docker commands through the mounted socket; whoever
can administer Jenkins can therefore control containers on that host. Limit
Jenkins administrator access and keep the host dedicated to trusted builds.

### Day-to-day commands

```bash
# Jenkins lifecycle
cd jenkins
docker compose logs -f jenkins
docker compose restart jenkins
docker compose down                 # preserves the jenkins_home volume

# Application deployment status (run from repository root)
cd ..
docker compose ps
docker compose logs -f api-gateway
curl -k https://localhost:8443/actuator/health
```

Use `docker compose down -v` only when intentionally discarding Jenkins
configuration and build history, because it deletes the `jenkins_home` volume.
### How the services work together

The normal request path is synchronous. The browser sends one request to the
gateway, the gateway discovers the correct service through Eureka, and that
service reads or changes only its own data. The response then travels back
through the gateway to the browser.

```mermaid
sequenceDiagram
	autonumber
	participant UI as Angular frontend
	participant GW as API Gateway
	participant ER as Eureka Registry
	participant US as User Service
	participant PS as Product Service
	participant MS as Media Service
	participant DB as Service MongoDB

	UI->>GW: Request with JWT
	GW->>ER: Resolve service name
	ER-->>GW: Service instance
	GW->>US: /api/auth or /api/users
	US->>DB: Read or write user data
	DB-->>US: User result
	US-->>GW: HTTP response
	GW-->>UI: HTTP response

	UI->>GW: Product request
	GW->>PS: /api/products
	PS->>DB: Read or write product data
	DB-->>PS: Product result
	PS-->>GW: HTTP response
	GW-->>UI: HTTP response

	UI->>GW: Multipart image upload
	GW->>MS: /api/media/images
	MS->>DB: Save media metadata
	MS-->>GW: Image URL or media result
	GW-->>UI: Upload response
```

The three service calls use separate data stores even when they participate in
one user workflow. Kafka connects the follow-up work that does not need to
block the original HTTP response.

### Kafka event architecture

The event flow below shows the implemented producers and consumers. Each
consumer uses its own group so a service receives the events relevant to its
responsibility.

```mermaid
flowchart LR
	Media[Media Service]
	User[User Service]
	Product[Product Service]
	Kafka[(Apache Kafka)]

	Media -->|avatar uploaded| AvatarTopic[avatar-uploaded-topic]
	AvatarTopic -->|update avatar| User

	Media -->|product image uploaded| UploadTopic[media-uploaded-topic]
	UploadTopic -->|append image URL| Product

	Media -->|media deleted| DeleteMediaTopic[media-deleted-topic]
	DeleteMediaTopic -->|remove image URL| Product

	Product -->|product deleted| DeleteProductTopic[product-deleted-topic]
	DeleteProductTopic -->|delete product media| Media

	User -.->|user deleted event flow\nproducer exists; invocation pending| DeleteUserTopic[user-deleted-topic]
	DeleteUserTopic -.->|delete user media| Media

	AvatarTopic --- Kafka
	UploadTopic --- Kafka
	DeleteMediaTopic --- Kafka
	DeleteProductTopic --- Kafka
	DeleteUserTopic --- Kafka
```

#### Example: product image upload

```mermaid
sequenceDiagram
	autonumber
	participant UI as Angular frontend
	participant GW as API Gateway
	participant MS as Media Service
	participant C as Cloudinary
	participant K as Kafka
	participant PS as Product Service

	UI->>GW: POST /api/media/images
	GW->>MS: Forward multipart file and JWT
	MS->>MS: Validate seller, MIME type, and 2 MB limit
	MS->>C: Upload image
	C-->>MS: Cloudinary URL
	MS->>MS: Save media metadata
	MS->>K: Publish media-uploaded-topic
	K-->>PS: MediaUploadedEvent(productId, imageUrl)
	PS->>PS: Append image URL to product
	MS-->>GW: Return uploaded image result
	GW-->>UI: Return response
```

#### Example: product deletion cleanup

```mermaid
sequenceDiagram
	participant UI as Angular frontend
	participant GW as API Gateway
	participant PS as Product Service
	participant K as Kafka
	participant MS as Media Service

	UI->>GW: DELETE /api/products/{id}
	GW->>PS: Forward authenticated request
	PS->>PS: Verify seller ownership
	PS->>PS: Delete product from Product MongoDB
	PS->>K: Publish product-deleted-topic
	K-->>MS: ProductDeletedEvent(productId)
	MS->>MS: Delete media metadata for product
	MS->>MS: Clean up associated media storage
	PS-->>GW: 204 No Content
	GW-->>UI: 204 No Content
```

### Runtime components

| Component | Responsibility | Default port |
| --- | --- | ---: |
| Angular frontend | Authentication screens, product browsing, seller dashboard | 4200 |
| API Gateway | TLS termination, CORS, routing, rate limiting, public entry point | 8443 |
| User Service | Registration, login, profiles, roles, JWT creation | 8081 |
| Product Service | Product CRUD, pagination, seller ownership checks | 8082 |
| Media Service | Image validation, Cloudinary upload/delete, media metadata | 8083 |
| Eureka Registry | Service registration and discovery | 8761 |
| Redis | Gateway request-rate limiter state | 6379 |
| Kafka | Domain event transport | 9092 / 29092 |
| User MongoDB | User Service persistence | 27017 |
| Media MongoDB | Media Service persistence | 27018 (host) |
| Product MongoDB | Product Service persistence | 27019 (host) |
| Kafka UI | Local Kafka inspection | 8085 |

## Main design strategies

### 1. Microservices and bounded ownership

The backend is split by business capability:

- **User Service** owns users, credentials, roles, profiles, and JWT issuance.
- **Product Service** owns product data and seller-to-product ownership.
- **Media Service** owns media metadata and image storage operations.
- **API Gateway** owns the external HTTP boundary and routing concerns.
- **Registry** owns service discovery, not business data.

Services are independently buildable and packaged with their own Maven project
and Dockerfile.

### 2. Database-per-service

Each business service has a separate MongoDB database and collection. Services
do not use database foreign keys across service boundaries. Instead, they store
logical identifiers:

```text
User (1) ---- owns ---- (many) Product (1) ---- has ---- (many) Media
```

- A product stores `sellerId`, referring to a user in User Service.
- Product image references are stored on the product, while image metadata is
	stored by Media Service.
- Cross-service ownership and existence checks happen in application code.

This keeps service data isolated and allows each service to evolve its schema
independently.

### 3. Service discovery and gateway routing

Services register with Eureka. The gateway routes public paths to logical
service names using load-balanced URIs such as `lb://USER-SERVICE`.

External clients should call the gateway rather than calling backend services
directly:

```text
/api/auth/**       -> User Service
/api/users/**      -> User Service
/api/products/**   -> Product Service
/api/media/**      -> Media Service
```

This gives one place to configure TLS, CORS, rate limits, and route policy.

### 4. Hybrid communication: REST plus events

REST is used when the caller needs an immediate response:

- Angular calls the gateway for authentication and product operations.
- User Service delegates media-related work to Media Service when required.
- Media Service uses a product client for ownership-related checks.

Kafka is used for decoupled side effects and cleanup:

- `avatar-uploaded-topic`
- `user-deleted-topic`
- `media-uploaded-topic`
- `media-deleted-topic`
- `product-deleted-topic`

For example, when media is uploaded for a product, Media Service publishes a
media-uploaded event. Product Service consumes it and links the returned image
reference to the product. When a product is deleted, a product-deleted event
can trigger media cleanup without making Product Service manage Cloudinary
directly.

Kafka consumers use Spring Kafka listener containers and service-specific
consumer groups. This allows consumers to process their own copy of an event
and supports concurrent partition processing when configured.

### 5. JWT authentication, roles, and ownership

User Service authenticates credentials and returns a JWT. The token subject is
the authenticated user's identifier. The frontend stores the token and an HTTP
interceptor adds it to API requests.

Authorization has two layers:

- **Role authorization:** `CLIENT` can browse; `SELLER` can manage products and
	media.
- **Resource ownership:** Product Service compares the authenticated subject
	with the product's `sellerId` before update or delete. Media deletion also
	verifies the owning seller.

Passwords are hashed with BCrypt and are never returned in user responses.

### 6. Validation and defensive media handling

The backend uses Jakarta validation for request DTOs. Product rules include a
positive price and a non-negative quantity. Media uploads are restricted to
images, have a 2 MB limit, and use content-type sniffing to reject payloads
that do not match an image.

The frontend also uses Angular Reactive Forms and route guards. It provides
client-side feedback, but backend validation remains authoritative.

### 7. Gateway protection

The gateway enables CORS for the Angular development origin and applies Redis
backed token-bucket rate limiting:

- User/authentication routes: replenish rate 10, burst capacity 20.
- Product routes: replenish rate 10, burst capacity 20.
- Media routes: replenish rate 30, burst capacity 50.

Redis makes limiter state shareable between gateway instances, unlike an
in-memory limiter.

### 8. External media storage

Media Service stores image metadata in MongoDB and the actual image in
Cloudinary. Other services receive stable media references or URLs rather than
handling Cloudinary credentials or storage implementation details.

## Data model

### User collection

Database: `user_service`, collection: `users`.

| Field | Meaning |
| --- | --- |
| `id` | User identifier and JWT subject |
| `name` | Unique display/login name |
| `email` | Unique email address |
| `password` | BCrypt hash |
| `role` | `ROLE_CLIENT` or `ROLE_SELLER` |
| `avatar` | Media URL or media identifier |

### Product collection

Database: `product_service`, collection: `products`.

| Field | Meaning |
| --- | --- |
| `id` | Product identifier |
| `name` | Product name |
| `description` | Optional description |
| `price` | Positive product price |
| `quantity` | Available quantity, zero or greater |
| `sellerId` | Logical reference to the owning user |
| `imageUrls` | References to media supplied by Media Service |

### Media collection

Database: `media_service`, collection: `media`.

| Field | Meaning |
| --- | --- |
| `id` | Media identifier |
| `imagePath` | Cloudinary path or URL |
| `productId` | Logical reference to the related product |

## API inventory

All paths below are intended to be called through the gateway.

### Authentication and users

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a client or seller |
| `POST` | `/api/auth/login` | Authenticate and receive a JWT |
| `GET` | `/api/users/me` | Read the authenticated profile |
| `PUT` | `/api/users/me` | Update the authenticated profile |

### Products

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/api/products` | Authenticated users |
| `GET` | `/api/products/{id}` | Authenticated users |
| `GET` | `/api/products/my?page=0&size=10` | Seller |
| `POST` | `/api/products` | Seller |
| `PUT` | `/api/products/{id}` | Owning seller |
| `DELETE` | `/api/products/{id}` | Owning seller |

### Media

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/media/images` | Upload one or more images |
| `GET` | `/api/media/images/{id}` | Resolve media metadata and URL |
| `DELETE` | `/api/media/images?url=...` | Delete an image owned by the seller |

The upload request uses multipart field `media` and accepts an optional
`productId` and `type`.

## Frontend architecture

The Angular application is organized around feature areas:

- `auth`: login and registration flows.
- `home`: authenticated product browsing.
- `product`: product details.
- `dashboard`: seller operations.
- `core`: layouts, guards, interceptors, and shared application services.

The router uses:

- `guestGuard` for login and registration pages.
- `authGuard` for authenticated pages.
- `roleGuard` for seller-only dashboard access.
- `authInterceptor` to attach JWT credentials and handle unauthorized
	responses.

The frontend uses Angular Reactive Forms, Angular Material, RxJS, and
`jwt-decode`. Run it separately from the backend with `npm start`.

## Running locally

### Prerequisites

- Java and Maven-compatible JDK for the Spring Boot services.
- Docker and Docker Compose.
- Node.js and npm for Angular.
- Environment values for JWT, MongoDB, Cloudinary, and the gateway keystore.

### Option A: Docker Compose

The root compose file starts infrastructure and containerized backend
services:

```bash
docker compose up --build
```

The gateway is exposed at `https://localhost:8443`, Kafka UI at
`http://localhost:8085`, and the containerized frontend at
`http://localhost:8080` (or `https://localhost:8444`). Before building, place
the gateway PKCS12 keystore and `frontend/certs/cert.pem` and
`frontend/certs/key.pem` in the workspace; the frontend Dockerfile requires
those certificate files even when using the HTTP port. The root Compose file
reads its interpolated values from a root `.env` file. For a local frontend
development server instead, run:

```bash
cd frontend
npm install
npm start
```

### Option B: local Spring Boot processes

Each service can also be run independently from its directory with its Maven
wrapper, for example:

```bash
cd Backend/product-service
./mvnw spring-boot:run
```

When running outside Docker, update environment values and Kafka/MongoDB host
names as needed. The default local backend ports are 8761, 8081, 8082, and
8083; the gateway configuration exposes HTTPS on 8443.

## Project structure

```text
buy-01/
├── Backend/
│   ├── api-gateway/       # Public gateway, TLS, CORS, routing, rate limits
│   ├── media-Service/     # Image and media operations
│   ├── product-service/   # Product operations and ownership
│   ├── registry/          # Eureka server
│   └── user-service/      # Authentication and profiles
├── frontend/              # Angular application
├── docs/project-docs/     # Database, Kafka, and project audit documents
├── jenkins/               # Jenkins controller image and Compose definition
├── Jenkinsfile            # CI/CD pipeline executed by Jenkins
├── docker-compose.yml     # Containerized local environment
└── README.md
```

## Current implementation status

The main authentication, product CRUD, seller ownership, image upload, Kafka
integration, Dockerfiles, frontend guards, and gateway rate limiting are
implemented. The project audit in `docs/project-docs/todolist.md` records the
remaining work. The most relevant gaps are:

- User deletion does not currently invoke the user-deleted producer flow.
- Media listing by `productId` is not exposed yet.
- Media caching headers (`ETag` and `Cache-Control`) are not implemented.
- Health endpoints are not consistently exposed by every service.
- Some media exception handling and frontend upload validation are incomplete.
- The gateway keystore and some required environment files are not present in
	the repository, so a clean Docker startup requires local configuration.
- API and security test coverage is still limited, especially for Kafka and
	cross-service flows.

For the detailed database model, see `docs/project-docs/database-design.md`.
For Kafka consumer flow notes, see `docs/project-docs/kafka-docs.md`. For the
full implementation audit, see `docs/project-docs/todolist.md`.

## Engineering principles

1. Keep business data inside the service that owns it.
2. Use the gateway as the public boundary.
3. Use synchronous REST when the caller needs an immediate result.
4. Use Kafka for decoupled updates, integration, and cleanup.
5. Derive identity from the verified JWT, never from a client-supplied seller
	 identifier.
6. Validate at both the API boundary and the domain/service layer.
7. Keep secrets in environment variables and never commit credentials.
8. Prefer observable, independently deployable services over shared database
	 coupling.
