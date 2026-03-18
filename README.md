# AWS Serverless Web Portal

A production-grade serverless web portal built with AWS Lambda (Java 21), DynamoDB, HTTP API Gateway, and a React frontend. Designed for minimal cost using the AWS Free Tier.

## Architecture

- **Compute**: 3 AWS Lambda functions (Java 21, ARM64, 512MB) deployed as ZIP packages
- **Database**: DynamoDB single-table design with GSI for email lookups
- **API**: HTTP API Gateway (cheaper than REST API)
- **Frontend**: React + TypeScript + Vite SPA

## Key Optimizations

1. **ZIP deployment** for faster cold starts vs container images
2. **Static initialization** pattern reuses DynamoDB clients across invocations
3. **ARM64 (Graviton)** architecture for 20% better price-performance
4. **URL Connection HTTP client** for smaller deployment package
5. **Single-table DynamoDB** design minimizes round-trips

## Project Structure

```
serverless-portal/
  backend/
    auth-service/        # Login + Register Lambda handlers
    content-service/     # Content list/create Lambda handler
  frontend/              # React + TypeScript + Vite
  infrastructure/
    template.yaml        # SAM template (DynamoDB + Lambda + HTTP API)
    docker-compose.yml   # LocalStack for local development
  scripts/
    create-local-table.sh
    seed-local.sh
    benchmark.sh
  docs/
    performance.md       # Cold start and cost analysis
```

## Run Locally (LocalStack)

```bash
# Start LocalStack
cd infrastructure && docker compose up -d

# Create table and seed demo user
cd .. && ./scripts/create-local-table.sh && ./scripts/seed-local.sh

# Build backend JARs
cd backend/auth-service && ./gradlew shadowJar
cd ../content-service && ../auth-service/gradlew -p . shadowJar

# Run frontend
cd ../../frontend && npm install && npm run dev
```

## Deploy to AWS

```bash
# Build JARs first (see above), then:
cd infrastructure
sam build
sam deploy --guided --profile your-aws-profile
```

After deploy, update `frontend/.env.local` with the `ApiEndpoint` from SAM output.

## Teardown

```bash
sam delete --stack-name serverless-portal --profile your-aws-profile --region us-east-1 --no-prompts
```

## Performance

See [docs/performance.md](docs/performance.md) for cold start measurements and cost analysis.

## References

- Duessmann & Fiorese (ICEIS 2025) — ZIP vs Container deployment benchmarks
- ElGazzar, Aziz & Soliman (IEEE 2024) — Serverless vs EC2 cost comparison
- Shehzadi (IEEE 2024) — Serverless architecture patterns survey
