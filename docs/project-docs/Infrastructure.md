## infrastruture

```
graph TD
    %% Client Layer
    subgraph Frontend [" Frontend Layer "]
        Angular[" Angular App <br/> (http://localhost:4200) "]
    end

    %% Gateway & Discovery Layer
    subgraph Gateway_Layer [" API Gateway & Core Infrastructure "]
        Gateway[" API Gateway (Port 8443) <br/> • Global CORS <br/> • Predicates Routing <br/> • Load Balancer (lb://) "]
        Redis[(" Redis Server <br/> (Rate Limiter: 10 req/s) ")]
        Eureka[" Eureka Registry (Port 8761) <br/> (Service Discovery HashMap) "]
    end

    %% Microservices Cluster
    subgraph Microservices [" Microservices Cluster "]
        UserSvc[" User Service <br/> (USER-SERVICE) "]
        ProductSvc[" Product Service <br/> (PRODUCT-SERVICE) "]
        MediaSvc[" Media Service <br/> (MEDIA-SERVICE) "]
    end

    %% Database Layer
    subgraph Databases [" Databases (Database Per Service) "]
        MongoUser[(" MongoDB User <br/> Port 27017 ")]
        MongoProduct[(" MongoDB Product <br/> Port 27019 ")]
        MongoMedia[(" MongoDB Media <br/> Port 27018 ")]
    end

    %% Interactions & Flows
    Angular -->|"1. Request /api/users/login"| Gateway
    Gateway <-->|"2. Check Rate Limit (Token Bucket)"| Redis
    Gateway -.->|"3. Fetch IPs Cache (Every 30s)"| Eureka
    
    UserSvc -.->|"Heartbeat & Register"| Eureka
    ProductSvc -.->|"Heartbeat & Register"| Eureka
    MediaSvc -.->|"Heartbeat & Register"| Eureka

    Gateway -->|"4. Load Balance & Forward"| UserSvc
    Gateway --> ProductSvc
    Gateway --> MediaSvc

    UserSvc --> MongoUser
    ProductSvc --> MongoProduct
    MediaSvc --> MongoMedia
```


## kafka 
```
graph TD
    %% Producer Component
    subgraph ProducerLayer [" Producer Layer (Media Service) "]
        MediaSvc[" Media Service <br/> (KafkaTemplate) "]
        EventPayload[" Event Object: MediaUploadedEvent <br/> • Key: 'prod-101' <br/> • Payload: { imageUrl: '...' } <br/> • Header: __TypeId__ = 'mediaUploaded' "]
    end

    %% Kafka Broker Internal Structure
    subgraph Broker [" Apache Kafka Broker "]
        subgraph Topic [" Topic: media-uploaded-topic "]
            
            subgraph Partition0 [" Partition 0 (Calculated via Hash) "]
                P0_O0[" Offset 0: Event A "]
                P0_O1[" Offset 1: Event B "]
                P0_O2[" Offset 2: [prod-101] MediaUploadedEvent "]
            end

            subgraph Partition1 [" Partition 1 "]
                P1_O0[" Offset 0: Event C "]
                P1_O1[" Offset 1: Event D "]
            end

        end
    end

    %% Consumer Groups Layer
    subgraph Consumers [" Independent Consumer Groups "]
        
        subgraph ProductGroup [" Consumer Group: product-service-group "]
            ProdConsumer[" Product Service Instance <br/> (@KafkaListener) <br/> Current Offset: 2 "]
            ProdDB[(" Product DB ")]
        end

        subgraph UserGroup [" Consumer Group: user-service-group "]
            UserConsumer[" User Service Instance <br/> (@KafkaListener) <br/> Current Offset: 2 "]
            UserDB[(" User DB ")]
        end

    end

    %% Workflow Connections
    MediaSvc -->|"1. Creates Event"| EventPayload
    EventPayload -->|"2. Partitioner: MurmurHash2('prod-101') % 2"| Partition0
    Partition0 -->|"3. Appends & Assigns Offset 2"| P0_O2

    P0_O2 -->|"4. Polled by product-service-group"| ProdConsumer
    P0_O2 -->|"4. Polled independently by user-service-group"| UserConsumer

    ProdConsumer -->|"5. Update DB & Commit Offset 3"| ProdDB
    UserConsumer -->|"5. Update DB & Commit Offset 3"| UserDB
```

## all 
```
graph TD
    %% Frontend Layer
    subgraph Frontend [" Frontend Layer "]
        Angular[" Angular App <br/> (http://localhost:4200) "]
    end

    %% Gateway & Service Discovery Layer
    subgraph Infrastructure [" API Gateway & Discovery Infrastructure "]
        Gateway[" API Gateway (Port 8443) <br/> • Global CORS Configuration <br/> • Path Predicates (/api/users, /api/products, /api/media) <br/> • Load Balancer (lb://) "]
        Redis[(" Redis Server <br/> (Token Bucket Rate Limiter) ")]
        Eureka[" Eureka Registry (Port 8761) <br/> (Service Discovery HashMap) "]
    end

    %% Event Messaging Infrastructure (Kafka)
    subgraph EventBus [" Event-Driven Infrastructure (Asynchronous) "]
        Kafka[(" Apache Kafka Broker (Port 9092) <br/> • Topics: media-uploaded-topic, avatar-uploaded-topic <br/> • Partitions & Offsets <br/> • Type Tokens Mapping (mediaUploaded) ")]
    end

    %% Microservices Cluster
    subgraph Microservices [" Microservices Cluster "]
        UserSvc[" User Service (USER-SERVICE) <br/> • Consumer Group: user-service-group <br/> • OpenFeign Client (Token Propagation) "]
        ProdSvc[" Product Service (PRODUCT-SERVICE) <br/> • Consumer Group: product-service-group <br/> • Resilience4j Circuit Breaker & Fallback "]
        MediaSvc[" Media Service (MEDIA-SERVICE) <br/> • Kafka Producer (KafkaTemplate) "]
    end

    %% Database Layer
    subgraph Databases [" Database Layer (Database Per Service) "]
        MongoUser[(" MongoDB User <br/> Port 27017 ")]
        MongoProd[(" MongoDB Product <br/> Port 27019 ")]
        MongoMedia[(" MongoDB Media <br/> Port 27018 ")]
    end

    %% Flows & Relationships

    %% 1. Client to Gateway
    Angular -->|"1. HTTP Requests"| Gateway
    Gateway <-->|"2. Check Rate Limit (10 req/s)"| Redis
    Gateway -.->|"3. Fetch IPs Local Cache (Every 30s)"| Eureka

    %% 2. Service Registration
    UserSvc -.->|"Register & Heartbeat"| Eureka
    ProdSvc -.->|"Register & Heartbeat"| Eureka
    MediaSvc -.->|"Register & Heartbeat"| Eureka

    %% 3. Gateway Routing
    Gateway -->|"4. Load Balanced Forward"| UserSvc
    Gateway --> ProdSvc
    Gateway --> MediaSvc

    %% 4. Synchronous Inter-Service (Feign + Circuit Breaker)
    UserSvc -->|"5. Sync HTTP Call via OpenFeign <br/> + JWT Relay (@RequestHeader) <br/> + Resilience4j Fallback Protection"| ProdSvc

    %% 5. Asynchronous Event-Driven (Kafka)
    MediaSvc -->|"6. Publish Event (mediaUploaded / avatarUploaded) <br/> [Key = productId / userId]"| Kafka
    Kafka -->|"7. Consume Event (user-service-group)"| UserSvc
    Kafka -->|"8. Consume Event (product-service-group)"| ProdSvc

    %% 6. Databases Persistence
    UserSvc --> MongoUser
    ProdSvc --> MongoProd
    MediaSvc --> MongoMedia
```
