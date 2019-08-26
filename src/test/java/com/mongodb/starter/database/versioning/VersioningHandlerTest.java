package com.mongodb.starter.database.versioning;

import com.mongodb.starter.database.versioning.exception.InvalidParameterException;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

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

    @Test(expected = UnknownCommand.class)
    public void executeQuery_exception_unknownCommand() {

    }

    @Test(expected = UnknownCollectionOperation.class)
    public void executeQuery_exception_unknownCollectionOperation() {

    }

    @Test(expected = IOException.class)
    public void executeQuery_exception_wrong_query_file() {

    }
}
