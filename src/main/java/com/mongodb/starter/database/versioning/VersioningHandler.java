package com.mongodb.starter.database.versioning;

import com.mongodb.starter.StarterConfiguration;
import com.mongodb.starter.database.dto.Versioning;
import com.mongodb.starter.database.repository.VersioningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.File;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

@Service
public class VersioningHandler {

    private MongoTemplate mongoTemplate;
    private StarterConfiguration starterConfiguration;
    private VersioningRepository versioningRepository;
    private final static String SUB_VERSION_DELIMITER = "_";

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

    }
}
