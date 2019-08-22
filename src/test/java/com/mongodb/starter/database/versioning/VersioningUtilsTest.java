package com.mongodb.starter.database.versioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.starter.database.dto.Address;
import com.mongodb.starter.database.dto.User;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations= "classpath:test.properties")
public class VersioningUtilsTest {

    @Autowired MongoTemplate mongoTemplate;
    @Autowired ObjectMapper objectMapper;

    private void cleanUp() {
        mongoTemplate.getDb().drop();
    }

    @Test
    public void queryAnalyzeCreateIndex() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/createIndex").toURI())).trim();
        QueryModel queryModel = VersioningUtils.queryAnalyze(
                VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray(), objectMapper);
        executeQuery(queryModel);
        cleanUp();
    }

    @Test
    public void queryAnalyzeCreateIndexes() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/createIndexes").toURI())).trim();
        QueryModel queryModel = VersioningUtils.queryAnalyze(
                VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray(), objectMapper);
        executeQuery(queryModel);
        cleanUp();
    }

    @Test
    public void queryAnalyzeDeleteOne() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        addRandomUsers(1);
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/deleteOne").toURI())).trim();
        QueryModel queryModel = VersioningUtils.queryAnalyze(
                VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray(), objectMapper);
        executeQuery(queryModel);
        cleanUp();
    }

    @Test
    public void queryAnalyzeDeleteMany() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        addRandomUsers(2);
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/deleteMany").toURI())).trim();
        QueryModel queryModel = VersioningUtils.queryAnalyze(
                VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray(), objectMapper);
        executeQuery(queryModel);
        cleanUp();
    }

    @Test
    public void queryAnalyzeDrop() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/drop").toURI())).trim();
        QueryModel queryModel = VersioningUtils.queryAnalyze(
                VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray(), objectMapper);
        executeQuery(queryModel);
        cleanUp();
    }

    @Test
    public void queryAnalyzeDropIndex() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/dropIndex").toURI())).trim();
        QueryModel queryModel = VersioningUtils.queryAnalyze(
                VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray(), objectMapper);
        executeQuery(queryModel);
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

    private void executeQuery(QueryModel query) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MongoCollection<Document> document = mongoTemplate.getCollection(query.getCollectionName());
        Method method;

        if(isFalse(query.isUseQuery())) {
            method = document.getClass().getMethod(query.getCollectionOperation());
            method.setAccessible(true);
            method.invoke(document);
        }
        else {
            List<Class<?>> classes = new ArrayList<>(Arrays.asList(query.getQuery().getClass().getInterfaces()));
            classes.add(query.getQuery().getClass());

            int interfaces = classes.size();

            if(query.isUseOptions()) {
                while(true) {
                    try {
                        method = document.getClass().getMethod(query.getCollectionOperation(), classes.get(interfaces - 1),
                                query.getOptions().getClass());
                        break;
                    } catch (NoSuchMethodException e) {
                        if(--interfaces == 0) {
                            throw e;
                        }
                    }
                }
                method.setAccessible(true);
                method.invoke(document, query.getQuery(), query.getOptions());
            }
            else {
                while(true) {
                    try {
                        method = document.getClass().getMethod(query.getCollectionOperation(), classes.get(interfaces - 1));
                        break;
                    } catch (NoSuchMethodException e) {
                        if(--interfaces == 0) {
                            throw e;
                        }
                    }
                }
                method.setAccessible(true);
                method.invoke(document, query.getQuery());
            }
        }
    }
}
