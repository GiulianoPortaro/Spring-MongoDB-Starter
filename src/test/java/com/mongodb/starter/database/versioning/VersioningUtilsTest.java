package com.mongodb.starter.database.versioning;

import com.mongodb.client.MongoCollection;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
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
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations= "classpath:test.properties")
public class VersioningUtilsTest {

    @Autowired MongoTemplate mongoTemplate;

    private void cleanUp() {
        mongoTemplate.getDb().drop();
    }

    @Test
    public void queryAnalyzeCreateIndex() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/createIndex").toURI()));
        List<?> q = VersioningUtils.queryAnalyze(VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray());
        executeQuery(q);
        cleanUp();
    }

    @Test
    public void queryAnalyzeCreateIndexes() throws UnknownCommand, UnknownCollectionOperation, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException, IOException {
        String query = Files.readString(Paths.get(VersioningUtilsTest.class.getResource("/versioning/createIndexes").toURI()));
        List<?> q = VersioningUtils.queryAnalyze(VersioningHandler.splitWithDelimiter(query, "(", "\\.").toArray());
        executeQuery(q);
        cleanUp();
    }

    private void executeQuery(List<?> query) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?>[] classes = query.get(2).getClass().getInterfaces();

        MongoCollection<Document> document = mongoTemplate.getCollection(query.get(0).toString());

        int interfaces = classes.length;

        Method method;
        if(query.get(4).equals(true)) {
            while(true) {
                try {
                    method = document.getClass().getMethod(query.get(1).toString(), classes[interfaces - 1], query.get(3).getClass());
                    break;
                } catch (NoSuchMethodException e) {
                    if(--interfaces == 0) {
                        throw e;
                    }
                }
            }
            method.setAccessible(true);
            method.invoke(document, query.get(2), query.get(3));
        }
        else {
            while(true) {
                try {
                    method = document.getClass().getMethod(query.get(1).toString(), classes[interfaces - 1]);
                    break;
                } catch (NoSuchMethodException e) {
                    if(--interfaces == 0) {
                        throw e;
                    }
                }
            }
            method.setAccessible(true);
            method.invoke(document, query.get(2));
        }
    }
}
