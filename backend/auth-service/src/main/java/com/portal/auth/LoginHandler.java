package com.portal.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.mindrot.jbcrypt.BCrypt;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final DynamoDbEnhancedClient ENHANCED_CLIENT;
    private static final DynamoDbTable<UserEntity> USER_TABLE;
    private static final DynamoDbIndex<UserEntity> EMAIL_INDEX;
    private static final String JWT_SECRET;
    private static final long INIT_TIME_MS;
    private static boolean isColdStart = true;

    static {
        long startNanos = System.nanoTime();

        String regionName = Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-east-1");
        Region region = Region.of(regionName);

        String tableName = Optional.ofNullable(System.getenv("TABLE_NAME"))
                .orElse("PortalData");

        String localstackEndpoint = System.getenv("LOCALSTACK_ENDPOINT");

        DynamoDbClientBuilder clientBuilder = DynamoDbClient.builder()
                .region(region)
                .httpClientBuilder(software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder());

        if (localstackEndpoint != null && !localstackEndpoint.isBlank()) {
            clientBuilder = clientBuilder.endpointOverride(URI.create(localstackEndpoint));
        }

        DynamoDbClient dynamoClient = clientBuilder.build();

        ENHANCED_CLIENT = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoClient)
                .build();

        USER_TABLE = ENHANCED_CLIENT.table(tableName, TableSchema.fromBean(UserEntity.class));
        EMAIL_INDEX = USER_TABLE.index("GSI1");

        String secretFromEnv = System.getenv("JWT_SECRET");
        if (secretFromEnv == null || secretFromEnv.length() < 32) {
            secretFromEnv = "demo-secret-key-at-least-32-characters-long!!";
        }
        JWT_SECRET = secretFromEnv;

        INIT_TIME_MS = (System.nanoTime() - startNanos) / 1_000_000;
        System.out.println("COLD_START_INIT_MS:" + INIT_TIME_MS);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        long requestStart = System.currentTimeMillis();
        String requestId = context != null ? context.getAwsRequestId() : "local-request";

        try {
            LoginRequest request = MAPPER.readValue(
                    Optional.ofNullable(input.getBody()).orElse("{}"),
                    LoginRequest.class
            );

            if (request.email == null || request.password == null) {
                return response(400, Map.of("error", "email and password are required"), requestId);
            }

            UserEntity user = findUserByEmail(request.email);
            if (user == null || user.getPasswordHash() == null ||
                    !BCrypt.checkpw(request.password, user.getPasswordHash())) {
                return response(401, Map.of("error", "Invalid credentials"), requestId);
            }

            String token = Jwts.builder()
                    .subject(user.getUserId())
                    .claim("email", user.getEmail())
                    .claim("role", Optional.ofNullable(user.getRole()).orElse("user"))
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                    .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                    .compact();

            long durationMs = System.currentTimeMillis() - requestStart;
            System.out.println("REQUEST_MS:" + durationMs + ",REQUEST_ID:" + requestId);

            boolean wasColdStart = isColdStart;
            isColdStart = false;

            Map<String, Object> body = new HashMap<>();
            body.put("token", token);
            body.put("user", Map.of(
                    "id", user.getUserId(),
                    "email", user.getEmail(),
                    "role", Optional.ofNullable(user.getRole()).orElse("user")
            ));
            body.put("isColdStart", wasColdStart);
            body.put("initMs", INIT_TIME_MS);
            body.put("requestMs", durationMs);

            return response(200, body, requestId);
        } catch (Exception e) {
            e.printStackTrace();
            return response(500, Map.of("error", "Internal error: " + e.getMessage()), requestId);
        }
    }

    private UserEntity findUserByEmail(String email) {
        QueryConditional query = QueryConditional.keyEqualTo(k -> k.partitionValue("EMAIL#" + email));
        Iterable<Page<UserEntity>> pages = EMAIL_INDEX.query(r -> r.queryConditional(query).limit(1));

        for (Page<UserEntity> page : pages) {
            if (!page.items().isEmpty()) {
                return page.items().get(0);
            }
        }
        return null;
    }

    private APIGatewayProxyResponseEvent response(int status,
                                                  Map<String, Object> body,
                                                  String requestId) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("X-Request-Id", requestId);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(headers)
                    .withBody(MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }
}

