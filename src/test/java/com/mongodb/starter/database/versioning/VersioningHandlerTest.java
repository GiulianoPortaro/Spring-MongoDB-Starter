package com.mongodb.starter.database.versioning;

import com.mongodb.starter.database.versioning.exception.InvalidParameterException;
import org.junit.After;
import org.junit.Assert;
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

    @After
    public void cleanUp() {
        mongoTemplate.getDb().drop();
    }

    @Test
    public void databaseBuild() {
        versioningHandler.databaseBuild();
    }

    @Test
    public void migration() throws InvalidParameterException {
        versioningHandler.migration("v1.1", "1");
    }

    @Test
    public void migration_exception_version() {
        try {
            versioningHandler.migration("v1.1vfde", "1");
        }
        catch (InvalidParameterException e) {
            //ignore
        }
        catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void migration_exception_subversion() {
        try {
            versioningHandler.migration("v1.1", "1e3d");
        }
        catch (InvalidParameterException e) {
            //ignore
        }
        catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void restore_dumps() {
        versioningHandler.restore_dumps();
    }
}
