package com.mongodb.starter.database.versioning;

import com.mongodb.starter.database.versioning.exception.InvalidParameterException;
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
        mongoTemplate.getDb().drop();
    }

    @Test
    public void databaseBuild() {
        versioningHandler.databaseBuild();
        cleanUp();
    }

    @Test
    public void migration() throws InvalidParameterException {
        versioningHandler.migration("v1.1", "1");
        cleanUp();
    }

    @Test(expected = InvalidParameterException.class)
    public void migration_exception_version() throws InvalidParameterException {
        versioningHandler.migration("v1.1vfde", "1");
        cleanUp();
    }

    @Test(expected = InvalidParameterException.class)
    public void migration_exception_subversion() throws InvalidParameterException {
        versioningHandler.migration("v1.1", "1e3d");
        cleanUp();
    }
}
