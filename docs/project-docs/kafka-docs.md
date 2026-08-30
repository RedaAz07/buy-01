
```
ConsumerFactory
      ↓
Kafka Consumer
      ↓
ContainerFactory
      ↓
Listener Container
      ↓
@KafkaListener
      ↓
    messages
      ↓
handleUserDeleted()
```

# ConcurrentKafkaListenerContainerFactory

```
user-deleted-topic

Partition 0 ── messages
Partition 1 ── messages
Partition 2 ── messages
```
```
Consumer
   │
   ├── Partition 0
   ├── Partition 1
   └── Partition 2
```

```
KafkaListenerContainerFactory
        │
        │ interface
        ▼
ConcurrentKafkaListenerContainerFactory
        │
        ├── ConsumerFactory
        ├── concurrency
        ├── error handler
        ├── acknowledgment
        └── listener container
```