package com.mongodb.starter.database.versioning;

import com.mongodb.client.MongoCollection;
import com.mongodb.starter.StarterConfiguration;
import com.mongodb.starter.database.dto.Versioning;
import com.mongodb.starter.database.repository.VersioningRepository;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
import com.mongodb.starter.database.versioning.models.QueryModel;
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
import java.util.ArrayList;
import java.util.Arrays;
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
        QueryModel queryModel;
        try {
            filePayload = Files.readString(migrationFile.toPath()).trim();
            queryModel = VersioningUtils.queryAnalyze(splitWithDelimiter(filePayload, "(", "\\.").toArray());
        } catch (IOException e) {
            return;
        } catch (UnknownCommand unknownCommand) {
            return;
        } catch (UnknownCollectionOperation unknownCollectionOperation) {
            return;
        }

        //mongoTemplate.getCollection("").deleteMany();

        if(queryModel.isValidQuery()) {
            Class<?>[] classes = queryModel.getQuery().getClass().getInterfaces();

            int interfaces = classes.length;

            MongoCollection<org.bson.Document> document = mongoTemplate.getCollection(queryModel.getCollectionName());
            try {
                Method method;
                if(queryModel.isUseOptions()) {
                    while(true) {
                        try {
                            method = document.getClass().getMethod(queryModel.getCollectionOperation(), classes[interfaces - 1],
                                    queryModel.getOptions().getClass());
                            break;
                        } catch (NoSuchMethodException e) {
                            if(--interfaces == 0) {
                                return;
                            }
                        }
                    }
                    method.setAccessible(true);
                    method.invoke(document, queryModel.getQuery(), queryModel.getOptions());

                }
                else {
                    while(true) {
                        try {
                            method = document.getClass().getMethod(queryModel.getCollectionOperation(), classes[interfaces - 1]);
                            break;
                        } catch (NoSuchMethodException e) {
                            if(--interfaces == 0) {
                                return;
                            }
                        }
                    }
                    method.setAccessible(true);
                    method.invoke(document, queryModel.getQuery());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                //ignore
            }
        }
    }

    public static List<String> splitWithDelimiter(String stringToSplit, String delimiter, String regex) {
        String firstPart = stringToSplit.substring(0, stringToSplit.indexOf(delimiter));
        String secondPart = stringToSplit.substring(stringToSplit.indexOf(delimiter) + 1, stringToSplit.length() - 1);
        String[] splitted = firstPart.split(regex);
        List<String> ret =  new ArrayList<>(Arrays.asList(splitted));
        ret.add(secondPart);
        return ret;
    }
}
