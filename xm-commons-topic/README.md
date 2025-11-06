## xm-commons-topic
XM^online commons project for dynamic message consumption from kafka.

This functionality allows configuring **Kafka topic behavior** per tenant through a YAML configuration file `topic-consumers.yml`.  
The configuration defines how the microservice consumes, retries, and processes messages from Kafka topics.

### Example configuration (`topic-consumers.yml`)
```yaml
---
topics:
- key: "example-event"
  typeKey: "USER_EVENT"
  topicName: "webhook-event-user"
  retriesCount: 3
  backOffPeriod: 5000       # in milliseconds
  deadLetterQueue: "webhook-event-user-dlq"
  groupId: "user-group" # consumer group id
  logBody: true
  maxPollInterval: 300000   # in milliseconds
  isolationLevel: "read_committed"
  autoOffsetReset: "latest"
  metadataMaxAge: "300000"  # in milliseconds
  consumeMessagePerSecondLimit: 100 # messages per second
  concurrency: 1
```

### Description

| Property | Type | Description                                                                                             |
|-----------|------|---------------------------------------------------------------------------------------------------------|
| **key** | `String` | Logical key identifying the topic configuration.                                                        |
| **typeKey** | `String` | Type or category of the topic (e.g., `USER_EVENT`, `SYSTEM_EVENT`). Using as segment in LEP key.        |
| **topicName** | `String` | Kafka topic name used by the consumer.                                                      |
| **retriesCount** | `Integer` | Number of times to retry message processing before moving it to the dead-letter queue.                  |
| **backOffPeriod** | `Long` | Delay between retries (in milliseconds).                                                                |
| **deadLetterQueue** | `String` | Name of the Kafka topic where failed messages will be redirected after all retries.                     |
| **groupId** | `String` | Kafka consumer group ID used for partition assignment.                                                  |
| **logBody** | `Boolean` | Whether to log message payloads.                                                      |
| **maxPollInterval** | `Integer` | Maximum time (in milliseconds) between poll invocations before the consumer is considered unresponsive. |
| **isolationLevel** | `String` | Kafka isolation level (`read_uncommitted` or `read_committed`).                                         |
| **autoOffsetReset** | `String` | Determines where to start reading messages if no offset is found (`latest`, `earliest`, or `none`).     |
| **metadataMaxAge** | `String` | Time (in milliseconds) after which topic metadata is refreshed.                                         |
| **consumeMessagePerSecondLimit** | `Integer` | Limits the number of messages consumed per second to control load.                                      |
| **concurrency** | `Integer` | Number of concurrent Kafka listener threads (parallel message consumption).                             |

