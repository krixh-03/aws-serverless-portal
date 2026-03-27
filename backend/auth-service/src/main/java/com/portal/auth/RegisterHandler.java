package com.portal.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RegisterHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DynamoDbEnhancedClient ENHANCED_CLIENT;
    private static final DynamoDbTable<UserEntity> USER_TABLE;
    private static final DynamoDbIndex<UserEntity> EMAIL_INDEX;

    static {
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
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            RegisterRequest request = MAPPER.readValue(
                    Optional.ofNullable(input.getBody()).orElse("{}"),
                    RegisterRequest.class
            );

            if (request.email == null || request.password == null) {
                return response(400, Map.of("error", "email and password are required"));
            }

            boolean exists = emailExists(request.email);
            if (exists) {
                return response(409, Map.of("error", "Email already registered"));
            }

            String userId = UUID.randomUUID().toString();
            String passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(10));

            UserEntity user = new UserEntity();
            user.setPK("USER#" + userId);
            user.setSK("PROFILE#" + userId);
            user.setGSI1PK("EMAIL#" + request.email);
            user.setGSI1SK(request.email);
            user.setUserId(userId);
            user.setEmail(request.email);
            user.setPasswordHash(passwordHash);
            user.setRole(Optional.ofNullable(request.role).orElse("user"));
            user.setCreatedAt(Instant.now().toString());

            USER_TABLE.putItem(user);

            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("email", request.email);
            body.put("role", user.getRole());
            body.put("message", "User created successfully");

            return response(201, body);
        } catch (Exception e) {
            e.printStackTrace();
            return response(500, Map.of("error", "Internal error: " + e.getMessage()));
        }
    }

    private boolean emailExists(String email) {
        QueryConditional query = QueryConditional.keyEqualTo(k -> k.partitionValue("EMAIL#" + email));
        Iterable<Page<UserEntity>> pages = EMAIL_INDEX.query(r -> r.queryConditional(query).limit(1));

        for (Page<UserEntity> page : pages) {
            if (!page.items().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private APIGatewayProxyResponseEvent response(int status, Map<String, Object> body) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(headers)
                    .withBody(MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class RegisterRequest {
        public String email;
        public String password;
        public String role;
    }
}

