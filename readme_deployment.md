# Concert Ticketing Backend

A highly concurrent concert ticketing system built with Spring Boot that prevents overselling, supports time-boxed reservations, and manages watchlists.

## 🚀 Features

- **Concurrent-Safe Reservations**: Reserve 1-6 tickets with 120-second hold
- **Oversell Prevention**: Atomic operations prevent ticket overselling under high load
- **Watchlist Notifications**: Get notified when sold-out concerts become available
- **Purchase Management**: Buy reserved tickets and cancel purchases with refunds
- **Admin Controls**: Create and update concerts with capacity management
- **Auto-Expiry**: Automatic reservation cleanup with background jobs

## 🏗️ Architecture

- **Framework**: Spring Boot 3.5.4 with Java 21
- **Database**: PostgreSQL with Flyway migrations
- **Concurrency**: Pessimistic locking and atomic operations
- **Observability**: Structured JSON logging for domain events
- **Infrastructure**: AWS ECS Fargate with RDS PostgreSQL
- **CI/CD**: GitHub Actions with automated testing and deployment

## 🔧 Quick Start

### Prerequisites

- Java 21
- PostgreSQL 15+
- Docker & Docker Compose
- AWS CLI (for deployment)
- Node.js 18+ (for CDK)

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd concert_backend
   ```

2. **Start local infrastructure**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d postgres
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the API**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health: http://localhost:8080/actuator/health

### Using Docker Compose

```bash
# Start everything
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f app

# Stop
docker-compose -f docker-compose.dev.yml down
```

## 🚀 AWS Deployment

### 1. Setup AWS Credentials

```bash
aws configure
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_REGION=us-east-1
```

### 2. Deploy Infrastructure

```bash
cd cdk
npm install
npm run build
npx cdk bootstrap
npx cdk deploy
```

### 3. Build and Push Container

```bash
# Create ECR repository
aws ecr create-repository --repository-name concert-backend

# Build and push
docker build -t concert-backend .
aws ecr get-login-password | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
docker tag concert-backend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/concert-backend:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/concert-backend:latest
```

### 4. Update ECS Service

The GitHub Actions pipeline automatically builds and deploys on push to `main`.

## 🧪 Testing

### Unit and Integration Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Load Testing

```bash
# Install k6
brew install k6  # macOS
# or download from https://k6.io/docs/getting-started/installation/

# Run load tests
k6 run load-test.js

# Run with custom target
BASE_URL=https://your-api-url.com k6 run load-test.js
```

### Manual API Testing

```powershell
# PowerShell examples
$base = "http://localhost:8080"

# List concerts
curl.exe -s "$base/concerts" | jq

# Reserve tickets
$body = @{ userId = "u-test"; quantity = 2 } | ConvertTo-Json -Compress
$res = Invoke-RestMethod "$base/concerts/1/reserve" -Method POST -ContentType "application/json" -Body $body

# Buy tickets
Invoke-RestMethod "$base/reservations/$($res.reservationId)/buy" -Method POST
```

## 📊 Monitoring

### Structured Logging

The application emits structured JSON logs for all domain events:

```json
{
  "@timestamp": "2025-08-20T10:30:00.000Z",
  "level": "INFO",
  "logger_name": "domain",
  "message": "reservation_placed",
  "concert_id": 1,
  "reservation_id": 123,
  "qty": 2
}
```

### Key Metrics

- `reservation_placed`: New ticket reservations
- `reservation_expired`: Automatic reservation cleanup
- `purchase_completed`: Successful ticket purchases
- `purchase_cancelled`: Refunds and cancellations
- `watchlist_notified`: Restock notifications sent

### Health Checks

- Application: `/actuator/health`
- Database connectivity included in health check
- Kubernetes/ECS health probes configured

## 🔐 Security Considerations

- **Input Validation**: All requests validated with Bean Validation
- **SQL Injection Protection**: JPA/Hibernate with parameterized queries
- **Concurrency Safety**: Pessimistic locking prevents race conditions
- **Idempotency**: Safe retry mechanisms for critical operations

## 🏢 Production Deployment

### Environment Variables

```bash
# Database
DB_URL=jdbc:postgresql://host:5432/concertdb
DB_USERNAME=concertapp
DB_PASSWORD=<secret>

# Application
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS="-Xmx1g -XX:+UseG1GC"

# Background Jobs
APP_EXPIRY_SWEEP_MS=5000
APP_OUTBOX_SWEEP_MS=5000
```

### Scaling Configuration

- **ECS Service**: 2-10 tasks with auto-scaling
- **Database**: RDS with read replicas for high load
- **Load Balancer**: Application Load Balancer with health checks

### Backup Strategy

- **RDS Automated Backups**: 7-day retention
- **Point-in-Time Recovery**: Available
- **Cross-Region Replication**: For disaster recovery

## 📝 API Documentation

Full OpenAPI 3.0 specification available:
- Local: http://localhost:8080/swagger-ui.html
- Spec: [openapi.yml](./openapi.yml)

### Key Endpoints

- `GET /concerts` - List all concerts
- `POST /concerts/{id}/reserve` - Reserve tickets
- `POST /reservations/{id}/buy` - Purchase reserved tickets
- `DELETE /sales/{id}` - Cancel purchase (refund)
- `POST /concerts/{id}/watchlist` - Join watchlist
- `POST /admin/concerts` - Create concert (admin)
- `PUT /admin/concerts/{id}` - Update concert (admin)

## 🐛 Troubleshooting

### Common Issues

1. **Reservation Expired**: Reservations auto-expire after 120 seconds
2. **Not Enough Tickets**: High concurrency may cause temporary conflicts
3. **Database Connection**: Check PostgreSQL connectivity and credentials
4. **Memory Issues**: Increase JVM heap size with `JAVA_OPTS`

### Debug Mode

```bash
# Enable debug logging
export LOGGING_LEVEL_COM_YOURTICKETING=DEBUG
./gradlew bootRun
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Run tests (`./gradlew test`)
4. Commit changes (`git commit -m 'Add amazing feature'`)
5. Push to branch (`git push origin feature/amazing-feature`)
6. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.