# AWS Serverless Web Portal

A performance-optimized serverless web portal built with AWS Lambda (Java 21, ARM64), DynamoDB, HTTP API Gateway, and a React frontend. Achieves 89% cold-to-warm latency reduction and near-zero monthly cost using the AWS Free Tier.

## Architecture

```
Browser (React SPA on Vite)
        |
        v
HTTP API Gateway  -->  /auth/login     -->  Lambda (Java 21, ARM64)  -->  DynamoDB
                  -->  /auth/register  -->  Lambda (Java 21, ARM64)  -->  (Single-Table
                  -->  /content        -->  Lambda (Java 21, ARM64)  -->   Design + GSI)
```

| Layer | Technology |
|-------|-----------|
| Compute | 3 AWS Lambda functions (Java 21, ARM64, 512 MB) — ZIP deployment |
| Database | DynamoDB single-table design with GSI for email lookups |
| API | HTTP API Gateway (71% cheaper than REST API) |
| Frontend | React 18 + TypeScript + Vite |
| Auth | BCrypt password hashing + JWT tokens |
| IaC | AWS SAM (CloudFormation) |
| Local Dev | LocalStack + Docker |

## Key Optimizations

1. **Static initialization pattern** — DynamoDB client, JWT key, and BCrypt config created once in a `static {}` block; warm requests skip ~900 ms of re-initialization
2. **ZIP deployment** (~16 MB shadow JAR) — avoids the 2–5 s container image pull penalty
3. **ARM64 (Graviton)** architecture — 20% cheaper per millisecond than x86_64
4. **HTTP API Gateway** — 71% cheaper per million requests ($1 vs $3.50) with lower latency
5. **Single-table DynamoDB** — one table, one GSI, one round-trip per query

## Project Structure

```
aws-serverless-portal/
  backend/
    auth-service/           # Login + Register Lambda handlers (Java 21)
    content-service/        # Content list/create Lambda handler (Java 21)
  frontend/                 # React + TypeScript + Vite SPA
  infrastructure/
    template.yaml           # SAM template (DynamoDB, Lambda, HTTP API)
    docker-compose.yml      # LocalStack for local development
    samconfig.toml          # SAM deployment configuration
  scripts/
    create-local-table.sh   # Create DynamoDB table in LocalStack
    seed-local.sh           # Seed demo user locally
    seed-aws.sh             # Seed demo user on AWS
    benchmark.sh            # Cold/warm start performance benchmark
  docs/
    performance.md          # Performance analysis and evaluation report
    appendices.txt          # Project appendices (architecture, setup, API docs)
  setup.sh                  # One-command local setup (LocalStack)
  setup-aws.sh              # One-command AWS deployment
  PRESENTATION.md           # Presentation guide with talking points
```

## Quick Start (Local Development)

**Prerequisites:** Java 21, Node.js 18+, Docker Desktop, AWS CLI v2

```bash
git clone git@github.com:krixh-03/aws-serverless-portal.git
cd aws-serverless-portal
bash setup.sh
```

This script installs dependencies, builds the backend JARs, starts LocalStack, creates the DynamoDB table, and seeds a demo user. When it finishes:

```bash
cd frontend && npm run dev
```

Open http://localhost:5173 and log in with `demo@test.com` / `password`.

## Deploy to AWS

**Prerequisites:** Everything above, plus AWS SAM CLI and configured AWS credentials (`aws configure`)

```bash
bash setup-aws.sh
```

This single script:
1. Checks all prerequisites (Java, Node, AWS CLI, SAM CLI)
2. Verifies AWS credentials
3. Installs frontend dependencies
4. Builds both backend JARs (`shadowJar`)
5. Validates and deploys the SAM template to AWS
6. Seeds the demo user into the live DynamoDB table
7. Writes the API endpoint to `frontend/.env.local`

After it completes, start the frontend:

```bash
cd frontend && npm run dev
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `STACK_NAME` | `serverless-portal` | CloudFormation stack name |
| `AWS_REGION` | `us-east-1` | AWS region to deploy to |
| `AWS_PROFILE` | (default credentials) | AWS CLI profile name |

Example with a custom profile:

```bash
AWS_PROFILE=my-profile bash setup-aws.sh
```

## Run Locally (Manual Steps)

If you prefer to run each step manually instead of using `setup.sh`:

```bash
# Install frontend dependencies
cd frontend && npm install

# Build backend JARs
cd ../backend/auth-service && ./gradlew shadowJar
cd ../content-service && ../auth-service/gradlew -p . shadowJar

# Start LocalStack
cd ../../infrastructure && docker compose up -d

# Create table and seed demo user
cd .. && bash scripts/create-local-table.sh && bash scripts/seed-local.sh

# Run frontend
cd frontend && npm run dev
```

## Performance Benchmarks

Run against a live AWS deployment:

```bash
bash scripts/benchmark.sh https://<your-api-id>.execute-api.us-east-1.amazonaws.com
```

### Results Summary

| Metric | Cold Start | Warm Start |
|--------|-----------|------------|
| Request latency | 4,248 ms | 467 ms (avg) |
| End-to-end (incl. network) | 6.95 s | 1.30 s (avg) |
| Improvement | — | **89% reduction** |

- 70%+ of cold start time is outside developer control (container provisioning + JVM startup)
- Warm latency is dominated by BCrypt password hashing (~450 ms) — a security feature, not a performance issue
- Non-cryptographic endpoints (content listing) achieve **< 50 ms** warm latency

See [docs/performance.md](docs/performance.md) for the full analysis.

## Cost

| Service | Monthly Cost |
|---------|-------------|
| Lambda (1K requests) | $0.00 |
| DynamoDB (on-demand) | $0.00 |
| HTTP API Gateway | $0.00 |
| **Total** | **< $0.05** |

Compared to an EC2 t3.micro running 24/7: **~99% savings**.

## Teardown

Remove all AWS resources:

```bash
aws cloudformation delete-stack --stack-name serverless-portal --region us-east-1
```

Stop LocalStack:

```bash
cd infrastructure && docker compose down
```

## References

1. M. L. El Bechir, C. Sad Bouh, and A. Shuwail, "Comprehensive Review of Performance Optimization Strategies for Serverless Applications on AWS Lambda," arXiv:2407.10397v1, Jul. 2024.
2. N. Mahmoudi and H. Khazaei, "Performance Modeling of Metric-Based Serverless Computing Platforms," IEEE Transactions on Cloud Computing, vol. 11, no. 2, pp. 1899–1910, Apr.–Jun. 2023.
3. T. Shehzadi, "Serverless Computing: Architecture, Best Practices, and Case Studies," IEEE, 2024.
