package com.mongodb.starter.database.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.StarterConfiguration;
import com.mongodb.starter.database.dto.Versioning;
import com.mongodb.starter.database.repository.VersioningRepository;
import com.mongodb.starter.database.versioning.exception.InvalidParameterException;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperationException;
import com.mongodb.starter.database.versioning.exception.UnknownCommandException;
import com.mongodb.starter.database.versioning.models.Param;
import com.mongodb.starter.database.versioning.models.QueryModel;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.starter.database.versioning.Defines.SUB_VERSION_DELIMITER;
import static com.mongodb.starter.database.versioning.Defines.VERSION_REGEX;
import static com.mongodb.starter.utils.StringUtils.splitWithDelimiter;
import static com.mongodb.starter.utils.StringUtils.subStringWithDelimiter;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;

@Service
@Log4j
public class VersioningHandler {

    private MongoTemplate mongoTemplate;
    private StarterConfiguration starterConfiguration;
    private VersioningRepository versioningRepository;
    private ObjectMapper objectMapper;

    @Autowired
    public VersioningHandler(MongoTemplate mongoTemplate, StarterConfiguration starterConfiguration,
                             VersioningRepository versioningRepository, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.starterConfiguration = starterConfiguration;
        this.versioningRepository = versioningRepository;
        this.objectMapper = objectMapper;
    }

    public void databaseBuild() {
        File[] files;
        try {
            File file = new File(starterConfiguration.getBasePath() + "database/dto");
            files = file.listFiles();
        }
        catch (Exception e) {
            log.error(MessageFormat.format("No directories structure found: error[{0}]", e.getMessage()));
            return;
        }

        if (files == null) {
            log.error("No dto found.");
            return;
        }

        for(File dtoFile : files) {
            String fileName = dtoFile.getName().split("\\.")[0];
            Class<?> classDto;
            try {
                classDto = Class.forName(starterConfiguration.getBasePackage() + "database.dto." + fileName);
            } catch (ClassNotFoundException e) {
                log.error(MessageFormat.format("Error class research: error[{0}]", e.getMessage()));
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

    public void restore_dumps() {
        File[] dumpFiles;
        try {
            File dumpTree = new File(VersioningHandler.class.getResource("/dump").getPath());
            dumpFiles = dumpTree.listFiles();
        }
        catch (Exception e) {
            log.error(MessageFormat.format("No directories structure found: error[{0}]", e.getMessage()));
            return;
        }

        if (dumpFiles == null) {
            log.error("No dump files found.");
            return;
        }

        for(File dumpFile : dumpFiles) {
            String collectionName = dumpFile.getName().split("\\.")[0];
            try {
                String collection = Files.readString(dumpFile.toPath()).replaceAll("\\s", "");
                while(isNotBlank(collection) && collection.length() != 1) {
                    String document = subStringWithDelimiter(collection, '{', '}');
                    collection = collection.replace(document, "");
                    mongoTemplate.getCollection(collectionName).insertOne(org.bson.Document.parse(document));
                }
            }
            catch (IOException e) {
                log.error(MessageFormat.format("Error during reading file: fileName[{0}], error[{1}].",
                        dumpFile.getName(), e.getMessage()));
            }
            catch (Exception e) {
                log.error(MessageFormat.format("Error during execute query: collectionName[{0}], error[{1}]",
                        collectionName, e.getMessage()));
            }
        }
    }

    public void migration(@NotNull String version, @NotNull String subVersion) throws InvalidParameterException {
        if(isFalse(version.startsWith("v"))) {
            version = "v" + version;
        }

        if(isFalse(version.substring(1).matches(VERSION_REGEX))) {
            log.error(MessageFormat.format("Invalid parameter version[{0}]", version));
            throw new InvalidParameterException("Invalid parameter version[" + version + "]");
        }

        if(isFalse(isNumeric(subVersion))) {
            log.error(MessageFormat.format("Invalid parameter subversion[{0}]", subVersion));
            throw new InvalidParameterException("Invalid parameter subversion[" + subVersion + "]");
        }

        File treeResources;

        try {
            treeResources = new File(VersioningHandler.class.getResource("/migration").getPath());
        }
        catch (Exception e) {
            log.error(MessageFormat.format("No directory structure found: error[{0}]", e.getMessage()));
            return;
        }

        analyzeResourceType(treeResources.listFiles(), version, subVersion);
    }

    public void executeQuery(@NotNull QueryModel queryModel) {
        Method method;
        List<Param> params = queryModel.getParams();
        int paramsSize = params.size();

        List<Class<?>> classesX = (paramsSize > 0) ? getClasses(params.get(0)) : new ArrayList<>();
        List<Class<?>> classesY = (paramsSize > 1) ? getClasses(params.get(1)) : new ArrayList<>();

        int interfacesX = classesX.size();
        int interfaces = (classesY.size() > 0) ? interfacesX * classesY.size() : interfacesX;

        Object object = queryModel.isGlobal() ? mongoTemplate :
                mongoTemplate.getCollection(queryModel.getCollectionName());
        Class<?> objectClass = object.getClass();

        if(queryModel.isValidQuery()) {
            int x = 0;
            int y = 0;
            while (true) {
                try {
                    if(paramsSize == 0) {
                        method = objectClass.getMethod(queryModel.getCollectionOperation());
                        method.setAccessible(true);
                        method.invoke(object);
                        break;
                    }
                    else if(paramsSize == 1) {
                        method = objectClass.getMethod(queryModel.getCollectionOperation(), classesX.get(x));
                        method.setAccessible(true);
                        method.invoke(object, params.get(0).getQueryParameter());
                        break;
                    }
                    else if(paramsSize == 2) {
                        method = objectClass.getMethod(queryModel.getCollectionOperation(), classesX.get(x), classesY.get(y));
                        method.setAccessible(true);
                        method.invoke(object, params.get(0).getQueryParameter(), params.get(1).getQueryParameter());
                        break;
                    }
                    else {
                        method = objectClass.getMethod(queryModel.getCollectionOperation(), classesX.get(x), classesY.get(y), params.get(2).getObjectClass());
                        method.setAccessible(true);
                        method.invoke(object, params.get(0).getQueryParameter(), params.get(1).getQueryParameter(), params.get(2).getQueryParameter());
                        break;
                    }
                }
                catch (NoSuchMethodException e) {
                    if(++x >= interfacesX) {
                        x = 0;
                        y++;
                    }
                    if (--interfaces == 0 || interfaces == -1) {
                        log.error(MessageFormat.format("No method found, with [{0}], try: error[{1}].",
                                (classesY.size() > 0) ? interfacesX * classesY.size() : interfacesX, e.getMessage()));
                        return;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error(MessageFormat.format("Error occurred during execution of query [{0}]: error[{1}].",
                            e.getMessage()));
                }
            }
        }
    }

    private void analyzeResourceType(File[] resources, @NotNull String version, @NotNull String subVersion) {
        if(isEmpty(resources)) {
            return;
        }

        for(File resource : resources) {
            if(resource.isDirectory() && resource.getName().compareTo(version) <= 0) {
                log.info(MessageFormat.format("Analyze directory[{0}].", resource.getName()));
                analyzeResourceType(resource.listFiles(), version, subVersion);
            }
            else if(resource.isFile()) {
                String resourceName = resource.getName();
                if(isFalse(resourceName.startsWith("v"))) {
                    resourceName = "v" + resourceName;
                }
                if(resourceName.contains(version)) {
                    if(analyzeSubVersion(version, subVersion, resourceName)) {
                        log.info(MessageFormat.format("Analyze file[{0}].", resourceName));
                        migrationBuild(resource);
                    }
                }
                else {
                    log.info(MessageFormat.format("Analyze file[{0}].", resourceName));
                    migrationBuild(resource);
                }

            }
        }
    }

    private boolean analyzeSubVersion(@NotNull String version, @NotNull String subVersion, @NotNull String filename) {
        return (filename.contains(SUB_VERSION_DELIMITER) && filename.split(SUB_VERSION_DELIMITER)[1].compareTo(subVersion) <= 0)
                || (filename.compareTo(version) <= 0);
    }

    private void migrationBuild(@NotNull File migrationFile) {
        String fileName = migrationFile.getName();

        if(isFalse(fileName.startsWith("v"))) {
            fileName = "v" + fileName;
        }

        int totalQuery = 0;
        int successQuery = 0;
        String filePayload;
        try {
            filePayload = Files.readString(migrationFile.toPath()).trim();
            if (StringUtils.isBlank(filePayload)) {
                log.info(MessageFormat.format("Read empty file[{0}]", fileName));
                return;
            }
        }
        catch (IOException e) {
            log.error(MessageFormat.format("Parse error: file[{}], error[{0}]",fileName, e.getMessage()));
            return;
        }

        StringBuilder description = new StringBuilder("The file contained the execution of the following queries: ");

        filePayload = VersioningUtils.evaluateQueryValidity(filePayload);

        while(filePayload.contains(")")) {
            try {
                QueryModel queryModel = VersioningUtils.queryAnalyze(splitWithDelimiter(
                        filePayload, "(", ")", "\\.").toArray(), objectMapper, starterConfiguration
                );
                executeQuery(queryModel);
                filePayload = filePayload.substring(filePayload.indexOf(")") + 1).trim();
                totalQuery++;
                successQuery++;
                description.append(", ")
                        .append(queryModel.getCollectionOperation())
                        .append(" (collection: ").
                        append(queryModel.getCollectionName()).append(")");
            } catch (UnknownCommandException | UnknownCollectionOperationException u) {
                log.error(MessageFormat.format("Error during analyze file: type[{0}] file[{1}], error[{2}]", u.getClass(),
                        fileName, u.getMessage()));
                totalQuery++;
            }
        }
        versioningRepository.insert(new Versioning(fileName.split("_")[0], fileName,
                description.toString(), (successQuery / totalQuery) * 100 + "%"));
    }

    private List<Class<?>> getClasses(@NotNull Param param) {
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(param.getObjectClass().getInterfaces()));
        classes.add(param.getObjectClass());
        classes.add(param.getObjectClass().getSuperclass());
        return classes;
    }
}
