# Microservices scaling

The gateway sends requests to service names (`lb://...`), and Eureka keeps the
registered instances for each name. Application services now use a unique
Eureka instance ID and accept their port and infrastructure endpoints from
environment variables. This lets multiple instances run without port clashes.

## Run a scaled local stack

Docker Compose is the intended local scaling entry point. It starts MySQL,
MongoDB, Kafka, Eureka, Zipkin, the gateway, and the application services.

```powershell
$env:MYSQL_ROOT_PASSWORD = "change-this-before-sharing"
docker compose up --build --scale product-service=3 --scale inventory-service=3 --scale order-service=3 --scale notification-service=2
```

Only the gateway (`http://localhost:8098`) and Eureka (`http://localhost:8761`)
are published to the host. The replicas use random internal ports and are
discovered and load-balanced through Eureka. Do not add `container_name` to a
service that you intend to scale.

For a single instance of each service, omit the `--scale` options. The default
property values still support running each application directly from the IDE on
its original port.

## Capacity notes

- Inventory no longer contains the 10-second demonstration sleep, which had
  been consuming one request thread per inventory lookup.
- The API gateway has bounded connect and response timeouts. Tune these from
  observed p95/p99 latency rather than increasing them blindly.
- Keep database schemas and Kafka partitions aligned with expected write and
  consumer parallelism; replicas alone cannot exceed either of those limits.
