package com.mongodb.starter.database.versioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.starter.StarterConfiguration;
import com.mongodb.starter.database.dto.Address;
import com.mongodb.starter.database.dto.User;
import com.mongodb.starter.database.repository.VersioningRepository;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
import com.mongodb.starter.database.versioning.models.QueryModel;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations= "classpath:test.properties")
public class VersioningUtilsTest {

    @Autowired MongoTemplate mongoTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired StarterConfiguration starterConfiguration;
    @Autowired VersioningRepository versioningRepository;
    private VersioningHandler versioningHandler;

    private void tearsUp() {
        try {
            versioningHandler = new VersioningHandler(mongoTemplate, starterConfiguration, versioningRepository, objectMapper);
            mongoTemplate.getDb().drop();
            mongoTemplate.getDb().createCollection("user");
            mongoTemplate.getDb().getCollection("user").createIndex(BasicDBObject.parse("{mail:1}"), new IndexOptions().name("mail"));
        }
        catch (Exception e) {
            //ignore
        }
    }

    private void cleanUp() {
        mongoTemplate.getDb().drop();
    }

    @Test
    public void queryAnalyzeCreateIndex() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/createIndex").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeCreateIndexes() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/createIndexes").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeDeleteOne() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        addRandomUsers(1);
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/deleteOne").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeDeleteMany() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        addRandomUsers(2);
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/deleteMany").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeDrop() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/drop").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeDropIndex() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/dropIndex").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeDropIndexes() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/dropIndexes").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeInsertOne() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/insertOne").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeInsertMany() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/insertMany").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeRemove() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/remove").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeRenameCollection() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/renameCollection").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeSave() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/save").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeUpdate() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/update").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeUpdateOne() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/updateOne").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeUpdateMany() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/updateMany").toURI())).trim();
        migrationOperation(query);
    }

    @Test
    public void queryAnalyzeCreateCollection() throws UnknownCommand, UnknownCollectionOperation, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/createCollection").toURI())).trim();
        migrationOperation(query);
    }

    private void migrationOperation(String query) throws UnknownCommand, UnknownCollectionOperation {
        while(query.contains(")")) {
            tearsUp();
            QueryModel queryModel = VersioningUtils.queryAnalyze(
                    VersioningUtils.splitWithDelimiter(query, "(", ")", "\\.").toArray(), objectMapper, starterConfiguration);
            versioningHandler.executeQuery(queryModel);
            query = query.substring(query.indexOf(")") + 1).trim();
        }
        cleanUp();
    }

    private void addRandomUsers(int number) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        for(int i = 0; i < number; i++) {
            User user = new User("User", "Surname" + i, "Mail" + i, i,
                    new Address("A" + i, "B" + i, i));
            mongoTemplate.getCollection("user").insertOne(Document.parse(objectMapper.writeValueAsString(user)));
        }
    }
}
