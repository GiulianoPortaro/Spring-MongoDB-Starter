package com.mongodb.starter.database.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.database.dto.User;
import com.mongodb.starter.database.repository.UserRepositoryCustom;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Map;

import static com.mongodb.starter.database.repository.impl.Defines.ADDRESS;
import static com.mongodb.starter.database.repository.impl.Defines.ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
@Log4j
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private MongoTemplate mongoTemplate;
    private ObjectMapper objectMapper;

    @Autowired
    public UserRepositoryCustomImpl(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updateAddress(String userId, JsonNode jsonNode) {
        Query query = new Query().addCriteria(where(ID).is(userId));
        Update update = new Update();

        Map<String, Object> elements = objectMapper.convertValue(jsonNode, new TypeReference<Map<String, Object>>(){});

        for (Map.Entry<String, Object> pair : elements.entrySet()) {
            update.set(ADDRESS + pair.getKey(), pair.getValue());
        }

        try {
            mongoTemplate.findAndModify(query, update, User.class);
        }
        catch (Exception e) {
            //ignore;
        }
    }
}
