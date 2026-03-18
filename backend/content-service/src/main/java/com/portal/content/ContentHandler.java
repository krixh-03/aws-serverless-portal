package com.portal.content;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ContentHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DynamoDbEnhancedClient ENHANCED_CLIENT;
    private static final DynamoDbTable<ContentItem> CONTENT_TABLE;

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

        CONTENT_TABLE = ENHANCED_CLIENT.table(tableName, TableSchema.fromBean(ContentItem.class));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            // For now, use a fixed user id that matches the seeded demo user.
            String userId = Optional.ofNullable(input.getRequestContext())
                    .map(APIGatewayProxyRequestEvent.ProxyRequestContext::getAuthorizer)
                    .map(a -> (String) a.get("userId"))
                    .orElse("demo-user-1");

            String path = Optional.ofNullable(input.getPath()).orElse("");
            String method = Optional.ofNullable(input.getHttpMethod()).orElse("GET").toUpperCase();

            if ("GET".equals(method)) {
                return listUserContent(userId);
            } else if ("POST".equals(method)) {
                return createContent(userId, input.getBody());
            }

            return response(404, Map.of("error", "Not found"));
        } catch (Exception e) {
            e.printStackTrace();
            return response(500, Map.of("error", e.getMessage()));
        }
    }

    private APIGatewayProxyResponseEvent listUserContent(String userId) {
        QueryConditional query = QueryConditional.keyEqualTo(k ->
                k.partitionValue("USER#" + userId + "#CONTENT")
        );

        PageIterable<ContentItem> pages = CONTENT_TABLE.query(r -> r.queryConditional(query));
        List<ContentItem> items = pages.items().stream().collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("count", items.size());

        return response(200, body);
    }

    private APIGatewayProxyResponseEvent createContent(String userId, String requestBody) throws Exception {
        ContentRequest request = MAPPER.readValue(
                Optional.ofNullable(requestBody).orElse("{}"),
                new TypeReference<ContentRequest>() {}
        );

        if (request.title == null || request.body == null) {
            return response(400, Map.of("error", "title and body are required"));
        }

        String contentId = UUID.randomUUID().toString();

        ContentItem item = new ContentItem();
        item.setPK("USER#" + userId + "#CONTENT");
        item.setSK("CONTENT#" + contentId);
        item.setGSI1PK("CONTENT");
        item.setGSI1SK(Instant.now().toString());
        item.setContentId(contentId);
        item.setUserId(userId);
        item.setTitle(request.title);
        item.setBody(request.body);
        item.setCreatedAt(Instant.now().toString());

        CONTENT_TABLE.putItem(item);

        Map<String, Object> body = new HashMap<>();
        body.put("contentId", contentId);
        body.put("message", "Created");

        return response(201, body);
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

    public static class ContentRequest {
        public String title;
        public String body;
    }
}

