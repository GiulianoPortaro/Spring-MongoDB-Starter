package com.mongodb.starter.database.versioning;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations= "classpath:test.properties")
public class VersioningHandlerTest {

    @Autowired VersioningHandler versioningHandler;
    @Autowired MongoTemplate mongoTemplate;

    private void cleanUp() {
        for(String collectionName : mongoTemplate.getCollectionNames()) {
            mongoTemplate.dropCollection(collectionName);
        }
    }

    @Test
    public void databaseBuild() {
        versioningHandler.databaseBuild();
        cleanUp();
    }
}
