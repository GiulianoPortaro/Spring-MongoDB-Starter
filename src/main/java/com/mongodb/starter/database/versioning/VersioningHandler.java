package com.mongodb.starter.database.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.StarterConfiguration;
import com.mongodb.starter.database.dto.Versioning;
import com.mongodb.starter.database.repository.VersioningRepository;
import com.mongodb.starter.database.versioning.exception.InvalidParameterException;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
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
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
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
                if(resource.getName().contains(version)) {
                    if(analyzeSubVersion(version, subVersion, resource.getName())) {
                        log.info(MessageFormat.format("Analyze file[{0}].", resource.getName()));
                        migrationBuild(resource);
                    }
                }
                else {
                    log.info(MessageFormat.format("Analyze file[{0}].", resource.getName()));
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
        try {
            String filePayload = Files.readString(migrationFile.toPath()).trim();
            if(StringUtils.isBlank(filePayload)) {
                log.info(MessageFormat.format("Read empty file[{0}]", migrationFile.getName()));
                return;
            }
            while(filePayload.contains(")")) {
                QueryModel queryModel = VersioningUtils.queryAnalyze(VersioningUtils.splitWithDelimiter(
                        filePayload, "(", ")", "\\.").toArray(), objectMapper, starterConfiguration
                );
                executeQuery(queryModel);
                filePayload = filePayload.substring(filePayload.indexOf(")") + 1).trim();
            }
        } catch (IOException e) {
            log.error(MessageFormat.format("Parse error: file[{}], error[{0}]", migrationFile.getName(), e.getMessage()));
        } catch (UnknownCommand | UnknownCollectionOperation u) {
            log.error(MessageFormat.format("Error during analyze file: type[{0}] file[{1}], error[{2}]", u.getClass(),
                    migrationFile.getName(), u.getMessage()));
        }
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

    private List<Class<?>> getClasses(@NotNull Param param) {
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(param.getObjectClass().getInterfaces()));
        classes.add(param.getObjectClass());
        classes.add(param.getObjectClass().getSuperclass());
        return classes;
    }
}
