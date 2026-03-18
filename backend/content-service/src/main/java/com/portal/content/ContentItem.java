package com.portal.content;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class ContentItem {

    private String PK;
    private String SK;
    private String GSI1PK;
    private String GSI1SK;

    private String contentId;
    private String userId;
    private String title;
    private String body;
    private String createdAt;

    @DynamoDbPartitionKey
    public String getPK() {
        return PK;
    }

    public void setPK(String PK) {
        this.PK = PK;
    }

    @DynamoDbSortKey
    public String getSK() {
        return SK;
    }

    public void setSK(String SK) {
        this.SK = SK;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "GSI1")
    public String getGSI1PK() {
        return GSI1PK;
    }

    public void setGSI1PK(String GSI1PK) {
        this.GSI1PK = GSI1PK;
    }

    @DynamoDbSecondarySortKey(indexNames = "GSI1")
    public String getGSI1SK() {
        return GSI1SK;
    }

    public void setGSI1SK(String GSI1SK) {
        this.GSI1SK = GSI1SK;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

