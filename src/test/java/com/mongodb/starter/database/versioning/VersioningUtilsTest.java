package com.mongodb.starter.database.versioning;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
import org.bson.conversions.Bson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    public void queryAnalyze() {
        String[] queryParts = "db.user.createIndex( { \"category\": 1 }, { \"name\": \"category_fr\", \"collation\": { \"normalization\": true, \"locale\": \"fr\", \"strength\": 2 } } )".split("\\.");
        List<?> q;
        try {
            q = VersioningUtils.queryAnalyze(queryParts);
        } catch (UnknownCommand unknownCommand) {
            return;
        } catch (UnknownCollectionOperation unknownCollectionOperation) {
            return;
        }

        if(q.isEmpty()) {
            return;
        }

        Class<?> classZ = q.get(2).getClass();

        if(q.get(2) instanceof DBObject) {
            classZ = Bson.class;
        }
        MongoCollection<org.bson.Document> document = mongoTemplate.getCollection(q.get(0).toString());
        try {
            Method method = document.getClass().getMethod(q.get(1).toString(), classZ, q.get(3).getClass());
            method.setAccessible(true);
            method.invoke(document, q.get(2), q.get(3));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return;
        }
        cleanUp();
    }
}
