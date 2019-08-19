package com.mongodb.starter.database.versioning;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.starter.StarterConfiguration;
import com.mongodb.starter.database.dto.Versioning;
import com.mongodb.starter.database.repository.VersioningRepository;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;

import static com.mongodb.starter.database.versioning.Defines.SUB_VERSION_DELIMITER;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

@Service
public class VersioningHandler {

    private MongoTemplate mongoTemplate;
    private StarterConfiguration starterConfiguration;
    private VersioningRepository versioningRepository;

    @Autowired
    public VersioningHandler(MongoTemplate mongoTemplate, StarterConfiguration starterConfiguration, VersioningRepository versioningRepository) {
        this.mongoTemplate = mongoTemplate;
        this.starterConfiguration = starterConfiguration;
        this.versioningRepository = versioningRepository;
    }

    public void databaseBuild() {

        File file = new File(starterConfiguration.getBasePath() + "database/dto");
        File[] files = file.listFiles();

        if(files == null) {
            return;
        }

        for(File dtoFile : files) {
            String fileName = dtoFile.getName().split("\\.")[0];
            Class<?> classDto;
            try {
                classDto = Class.forName(starterConfiguration.getBasePackage() + "database.dto." + fileName);
            } catch (ClassNotFoundException e) {
                return;
            }
            Document annotation = AnnotationUtils.findAnnotation(classDto, Document.class);
            if(annotation != null) {
                String collectionName = annotation.value();
                mongoTemplate.dropCollection(collectionName);
                mongoTemplate.createCollection(collectionName);
            }
        }

        versioningRepository.insert(new Versioning("v0", "", "Database Building", "100%"));
    }

    public void migration(@NotNull String version, @NotNull String subVersion) {
        if(isFalse(version.startsWith("v"))) {
            version = "v" + version;
        }

        File treeResources = new File(VersioningHandler.class.getResource("migration").getPath());

        analyzeResourceType(treeResources.listFiles(), version, subVersion);
    }

    private void analyzeResourceType(File[] resources, String version, String subVersion) {
        if(resources == null) {
            return;
        }

        for(File resource : resources) {
            if(resource.isDirectory() && resource.getName().compareTo(version) <= 0) {
                analyzeResourceType(resource.listFiles(), version, subVersion);
            }
            else if(resource.isFile()) {
                if(resource.getName().contains(SUB_VERSION_DELIMITER) &&
                        resource.getName().split(SUB_VERSION_DELIMITER)[1].compareTo(subVersion) <= 0) {
                    migrationBuild(resource, subVersion);
                }
                else if(resource.getName().compareTo(version) <= 0) {
                    migrationBuild(resource, subVersion);
                }
            }
        }
    }

    private void migrationBuild(File migrationFile, String subVersion) {
        String filePayload;
        List<?> queryParts;
        try {
            filePayload = Files.readString(migrationFile.toPath());
            queryParts = VersioningUtils.queryAnalyze(filePayload.split("\\."));
        } catch (IOException e) {
            return;
        } catch (UnknownCommand unknownCommand) {
            return;
        } catch (UnknownCollectionOperation unknownCollectionOperation) {
            return;
        }

        //mongoTemplate.getCollection("").createIndexes();

        if(isFalse(queryParts.isEmpty())) {
            Class<?> classZ = (queryParts.get(2) instanceof DBObject) ? Bson.class : queryParts.get(2).getClass();
            MongoCollection<org.bson.Document> document = mongoTemplate.getCollection(queryParts.get(0).toString());
            try {
                Method method = document.getClass().getMethod(queryParts.get(1).toString(), classZ, queryParts.get(3).getClass());
                method.setAccessible(true);
                method.invoke(document, queryParts.get(2), queryParts.get(3));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                //ignore
            }
        }
    }
}
