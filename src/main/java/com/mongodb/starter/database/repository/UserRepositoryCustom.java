package com.mongodb.starter.database.repository;

import com.fasterxml.jackson.databind.JsonNode;

public interface UserRepositoryCustom {
    void updateAddress(String userId, JsonNode update);
}
