package com.mongodb.starter.database.versioning;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import com.mongodb.client.model.*;
import com.mongodb.starter.StarterConfiguration;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperationException;
import com.mongodb.starter.database.versioning.exception.UnknownCommandException;
import com.mongodb.starter.database.versioning.models.Param;
import com.mongodb.starter.database.versioning.models.QueryModel;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

import static com.mongodb.starter.database.versioning.Defines.GET_COLLECTION;
import static com.mongodb.starter.utils.StringUtils.countOccurences;
import static com.mongodb.starter.utils.StringUtils.subStringWithDelimiter;

class VersioningUtils {

    static String evaluateQueryValidity(String filePayload) {
        if(filePayload.contains(GET_COLLECTION)) {
            String collectionName = filePayload.substring(filePayload.indexOf("(") + 2, filePayload.indexOf(")") - 1);
            filePayload = filePayload.replace(GET_COLLECTION + "(\"" + collectionName + "\")", collectionName);
        }
        return filePayload;
    }

    static QueryModel queryAnalyze(Object[] objects, ObjectMapper objectMapper, StarterConfiguration starterConfiguration) throws UnknownCommandException, UnknownCollectionOperationException {
        String[] queryParts = Arrays.copyOf(objects, objects.length, String[].class);
        String operation = "";
        QueryModel queryModel;
        int selector = 0;
        Defines.CollectionOperations collectionOperations;
        if(queryParts.length == 4) {
            operation = queryParts[3].trim();
            queryModel = new QueryModel(queryParts[1], queryParts[2]);
            selector = countOccurences(operation, '{', '}');
            try {
                collectionOperations = Defines.CollectionOperations.valueOf(queryParts[2]);
            }
            catch (Exception e) {
                throw new UnknownCollectionOperationException("Unknown collection operation: " + queryParts[2]);
            }
        }
        else {
            queryModel = new QueryModel(true);
            try {
                collectionOperations = Defines.CollectionOperations.valueOf(queryParts[1]);
            }
            catch (Exception e) {
                throw new UnknownCollectionOperationException("Unknown collection operation: " + queryParts[2]);
            }
        }
        try {
            switch (collectionOperations) {
                case createCollection: {
                    queryParts[2] = queryParts[2].replace("\"", "").trim();
                    queryModel.setCollectionName(queryParts[2]);
                    queryModel.setCollectionOperation(queryParts[1].trim());
                    queryModel.addParam(new Param(queryParts[2]));
                    queryModel.setGlobal(true);
                    break;
                }
                case createIndex: {
                    String indexes = subStringWithDelimiter(operation, '{', '}');
                    queryModel.addParam(new Param(BasicDBObject.parse(indexes)));

                    if (selector > 1) {
                        ObjectNode objectNode = (ObjectNode) objectMapper.readTree(operation.substring(indexes.length() + 1));
                        queryModel.addParam(new Param(getIndexOptions(objectMapper, objectNode)));
                    }
                    break;
                }
                case createIndexes: {
                    List<IndexModel> indexModels = new ArrayList<>();
                    String indexes = subStringWithDelimiter(operation, '[', ']');
                    ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(indexes);

                    IndexOptions indexOptions = new IndexOptions();
                    boolean option = StringUtils.isNoneBlank(operation.substring(indexes.length()));
                    if (option) {
                        ObjectNode objectNodeOptions = (ObjectNode) objectMapper.readTree(operation.substring(indexes.length() + 1));
                        indexOptions = getIndexOptions(objectMapper, objectNodeOptions);
                    }

                    Iterator<JsonNode> elements = arrayNode.elements();
                    while (elements.hasNext()) {
                        Bson dbObject = BasicDBObject.parse(elements.next().toString());
                        if (option) {
                            indexModels.add(new IndexModel(dbObject, indexOptions));
                        } else {
                            indexModels.add(new IndexModel(dbObject));
                        }
                    }
                    queryModel.addParam(new Param(indexModels));
                    break;
                }
                case deleteOne:
                case deleteMany:
                case remove: {
                    String indexes = subStringWithDelimiter(operation, '{', '}');
                    queryModel.addParam(new Param(BasicDBObject.parse(indexes)));

                    if (selector > 1) {
                        ObjectNode objectNode = (ObjectNode) objectMapper.readTree(operation.substring(indexes.length() + 1));
                        if (objectNode.get("collation") != null) {
                            DeleteOptions deleteOptions = new DeleteOptions().collation(collationParser(objectNode.get("collation")));
                            queryModel.addParam(new Param(deleteOptions));
                        }
                        queryModel.setCollectionOperation(
                                objectNode.get("justOne") != null ?
                                        objectNode.get("justOne").asBoolean() ? "deleteOne" : "deleteMany"
                                        : "deleteOne"
                        );
                    }
                    break;
                }
                case drop:
                case dropIndexes: {
                    break;
                }
                case dropIndex:
                case insertOne: {
                    if (selector == 0) {
                        queryModel.addParam(new Param(operation.replace("\"", "")));
                    }
                    else {
                        String query = subStringWithDelimiter(operation, '{', '}');
                        queryModel.addParam(new Param(Document.parse(query)));
                    }
                    break;
                }
                case insertMany: {
                    List<Document> documents = new ArrayList<>();
                    String query = subStringWithDelimiter(operation, '[', ']');
                    ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(query);

                    Iterator<JsonNode> elements = arrayNode.elements();
                    while (elements.hasNext()) {
                        documents.add(Document.parse(elements.next().toString()));
                    }
                    queryModel.addParam(new Param(documents));
                    break;
                }
                case renameCollection: {
                    String[] operations = operation.replace("\"", "").trim().split(",");
                    queryModel.addParam(new Param(new MongoNamespace(starterConfiguration.getDatabaseName(), operations[0])));
                    queryModel.addParam(new Param(new RenameCollectionOptions().dropTarget(Boolean.parseBoolean(operations[1]))));
                    break;
                }
                case save: {
                    queryModel.addParam(new Param(Document.parse(operation)));
                    queryModel.addParam(new Param(queryModel.getCollectionName()));
                    queryModel.setGlobal(true);
                    break;
                }
                case update:
                case updateOne:
                case updateMany: {
                    String query = subStringWithDelimiter(operation, '{', '}').trim();
                    operation = operation.substring(query.length() + 1).trim();
                    String update = subStringWithDelimiter(operation, '{', '}').trim();
                    operation = operation.substring(update.length()).trim();

                    queryModel.addParam(new Param(BasicDBObject.parse(query)));
                    queryModel.addParam(new Param(BasicDBObject.parse(update)));

                    if (StringUtils.isNoneBlank(operation)) {
                        String options = subStringWithDelimiter(operation, '{', '}').trim();
                        JsonNode objectNode = objectMapper.readTree(options);
                        queryModel.addParam(new Param(updateOptionsParser(objectNode, queryModel)));
                    }
                    else {
                        queryModel.setCollectionOperation("updateOne");
                    }
                    break;
                }
                default:
                    throw new UnknownCollectionOperationException("Unknown collection operation: " + collectionOperations);
            }
        }
        catch (UnknownCommandException | UnknownCollectionOperationException u) {
            throw u;
        }
        catch (Exception e) {
            return new QueryModel(false);
        }
        return queryModel;
    }

    private static IndexOptions getIndexOptions(ObjectMapper objectMapper, ObjectNode objectNode) throws UnknownCommandException {
        IndexOptions indexOptions;
        Collation collation = null;
        Bson storageEngine = null;
        Bson partialFilterExpression = null;

        if (objectNode.get("collation") != null) {
            collation = collationParser(objectNode.get("collation"));
            objectNode.remove("collation");
        }
        if (objectNode.get("storageEngine") != null) {
            storageEngine = BsonDocument.parse(objectNode.get("storageEngine").toString());
            objectNode.remove("storageEngine");
        }
        if (objectNode.get("partialFilterExpression") != null) {
            partialFilterExpression = BsonDocument.parse(objectNode.get("partialFilterExpression").toString());
            objectNode.remove("partialFilterExpression");
        }

        indexOptions = objectMapper.convertValue(objectNode, IndexOptions.class);
        indexOptions.collation(collation);
        indexOptions.storageEngine(storageEngine);
        indexOptions.partialFilterExpression(partialFilterExpression);
        return indexOptions;
    }

    private static UpdateOptions updateOptionsParser(JsonNode jsonNode, QueryModel queryModel) throws UnknownCommandException {
        ObjectMapper objectMapper = new ObjectMapper();
        UpdateOptions updateOptions = new UpdateOptions();
        Iterator<Map.Entry<String, JsonNode>> keyPairs = jsonNode.fields();
        queryModel.setCollectionOperation(jsonNode.get("multi") != null ? jsonNode.get("multi").asBoolean() ? "updateMany" : "updateOne" : "updateOne");
        while(keyPairs.hasNext()) {
            Map.Entry<String, JsonNode> nestedNode = keyPairs.next();
            Defines.UpdateOptionsField updateOptionsField;
            try {
                updateOptionsField = Defines.UpdateOptionsField.valueOf(nestedNode.getKey());
            } catch (Exception e) {
                throw new UnknownCommandException("Unknown field [" + nestedNode.getKey() + "] for UpdateOperations type.");
            }
            switch (updateOptionsField) {
                case upsert:
                    updateOptions.upsert(nestedNode.getValue().asBoolean());
                    break;
                case multi:
                    queryModel.setCollectionOperation(nestedNode.getValue().asBoolean() ? "updateMany" : "updateOne");
                    break;
                case collation:
                    updateOptions.collation(collationParser(nestedNode.getValue()));
                    break;
                case arrayFilters:
                    ObjectReader reader = objectMapper.readerFor(new TypeReference<List<String>>(){});
                    try {
                        updateOptions.arrayFilters(reader.readValue(nestedNode.getValue()));
                    } catch (IOException e) {
                        updateOptions.arrayFilters(null);
                    }
                    break;
                default:
                    throw new UnknownCommandException("Unknown field [" + updateOptionsField.name() + "] for UpdateOperations type.");
            }
        }
        return updateOptions;
    }

    private static Collation collationParser(@NotNull JsonNode jsonNode) throws UnknownCommandException {
        Iterator<Map.Entry<String, JsonNode>> keyPairs = jsonNode.fields();
        Collation.Builder builder = Collation.builder();
        while(keyPairs.hasNext()) {
            Map.Entry<String, JsonNode> nestedNode = keyPairs.next();
            Defines.CollationField collationField;
            try {
                collationField = Defines.CollationField.valueOf(nestedNode.getKey());
            }
            catch (Exception e) {
                throw new UnknownCommandException("Unknown field [" + nestedNode.getKey() + "] for Collation type.");
            }
            switch(collationField) {
                case locale:
                    builder.locale(nestedNode.getValue().textValue());
                    break;
                case strength:
                    builder.collationStrength(CollationStrength.fromInt(nestedNode.getValue().asInt()));
                    break;
                case alternate:
                    builder.collationAlternate(CollationAlternate.fromString(nestedNode.getValue().textValue()));
                    break;
                case backwards:
                    builder.backwards(nestedNode.getValue().booleanValue());
                    break;
                case caseFirst:
                    builder.collationCaseFirst(CollationCaseFirst.fromString(nestedNode.getValue().textValue()));
                    break;
                case caseLevel:
                    builder.caseLevel(nestedNode.getValue().booleanValue());
                    break;
                case maxVariable:
                    builder.collationMaxVariable(CollationMaxVariable.fromString(nestedNode.getValue().textValue()));
                    break;
                case normalization:
                    builder.normalization(nestedNode.getValue().booleanValue());
                    break;
                case numericOrdering:
                    builder.numericOrdering(nestedNode.getValue().booleanValue());
                    break;
                default:
                    throw new UnknownCommandException("Unknown field [" + collationField.name() + "] for Collation type.");
            }
        }
        return builder.build();
    }
}
