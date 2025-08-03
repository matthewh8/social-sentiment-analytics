# Social Media Sentiment Analytics System

A distributed microservices platform for social media sentiment analysis.

## Prerequisites

- **Java 17+** - [Download OpenJDK](https://adoptium.net/temurin/releases/)
- **Maven 3.6+** - [Installation Guide](https://maven.apache.org/install.html)

Verify installation:
```bash
java -version
mvn -version
```

## Running the Application

```bash
# Navigate to the service
cd services/data.ingestion.service

# Start the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## Testing

### Health Check
```bash
curl http://localhost:8080/actuator/health
```
Expected response: `{"status":"UP"}`

### Available Endpoints
- `GET /actuator/health` - Application health status
- `GET /actuator/info` - Application information

## Development

### Hot Reload
The application includes Spring Boot DevTools for automatic restart during development.

### Running Tests
```bash
cd services/data.ingestion.service
./mvnw test
```

### Building
```bash
./mvnw clean package
```