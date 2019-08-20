package com.mongodb.starter.database.versioning;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.*;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;

import java.util.*;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

public class VersioningUtils {

    public static List<?> queryAnalyze(Object[] objects) throws UnknownCommand, UnknownCollectionOperation {
        String[] queryParts = Arrays.copyOf(objects, objects.length, String[].class);
        String operation = queryParts[3].trim();
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        switch (Defines.CollectionOperations.valueOf(queryParts[2])) {
            case createIndex: {
                int selector = countOccurences(operation, '{', '}');
                DBObject dbObject;
                IndexOptions indexOptions;
                if (selector > 1) {
                    String indexes = subStringWithDelimiter(operation, '{', '}').trim();
                    dbObject = BasicDBObject.parse(indexes);
                    String options = operation.substring(indexes.length() + 1).trim();

                    ObjectNode objectNode;
                    try {
                        objectNode = (ObjectNode) objectMapper.readTree(options);
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }

                    indexOptions = getIndexOptions(objectMapper, objectNode);
                } else {
                    dbObject = BasicDBObject.parse(operation);
                    indexOptions = new IndexOptions();
                }
                return new ArrayList<>() {{
                    add(queryParts[1]);
                    add(Defines.COL_CREATE_INDEX);
                    add(dbObject);
                    add(indexOptions);
                    add(true);
                }};
            }
            case createIndexes: {
                List<IndexModel> indexModels = new ArrayList<>();
                IndexOptions indexOptions = new IndexOptions();
                int selector = countOccurences(operation, '{', '}');
                if (selector > 1) {
                    String indexes = subStringWithDelimiter(operation, '[', ']').trim();
                    String options = operation.substring(indexes.length() + 1).trim();

                    ArrayNode arrayNode;
                    ObjectNode objectNodeOptions;
                    try {
                        arrayNode = (ArrayNode) objectMapper.readTree(indexes);
                        objectNodeOptions = (ObjectNode) objectMapper.readTree(options);
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }

                    indexOptions = getIndexOptions(objectMapper, objectNodeOptions);

                    Iterator<JsonNode> elements = arrayNode.elements();
                    while(elements.hasNext()) {
                        JsonNode element = elements.next();
                        Bson dbObject = BasicDBObject.parse(element.toString());
                        indexModels.add(new IndexModel(dbObject, indexOptions));
                    }
                } else {
                    indexModels.add(new IndexModel(BasicDBObject.parse(operation)));
                }
                return new ArrayList<>() {{
                    add(queryParts[1]);
                    add(Defines.COL_CREATE_INDEXES);
                    add(indexModels);
                    add("null");
                    add(false);
                }};
            }
            default:
                throw new UnknownCollectionOperation("Unknown collection operation.");
        }
    }

    private static IndexOptions getIndexOptions(ObjectMapper objectMapper, ObjectNode objectNode) throws UnknownCommand {
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

    private static String subStringWithDelimiter(String someString, char startChar, char endChar) {
        int index = 0;
        int endOccurrences = 0;
        int startIndex = 0;
        boolean found = false;
        int endIndex = 0;
        while(index < someString.length()) {
            if(someString.charAt(index) == startChar && isFalse(found)) {
                startIndex = index;
                found = true;
                endOccurrences++;
            }
            else if(someString.charAt(index) == startChar && found) {
                endOccurrences++;
            }
            else if(someString.charAt(index) == endChar) {
                endOccurrences--;
                if(someString.charAt(index) == endChar && endOccurrences == 0 && found) {
                    endIndex = index;
                }
            }
            if(endIndex != 0) {
                break;
            }
            index++;
        }
        return someString.substring(startIndex, endIndex + 1);
    }

    private static int countOccurences(String someString, char startChar, char endChar) {
        int index = 0;
        int occurences = 0;
        int endOccurrences = 0;
        while(index < someString.length()) {
            if(someString.charAt(index) == startChar && endOccurrences == 0) {
                occurences++;
                endOccurrences++;
            }
            else if(someString.charAt(index) == startChar && endOccurrences != 0) {
                endOccurrences++;
            }
            else if(someString.charAt(index) == endChar) {
                endOccurrences--;
            }
            index++;
        }
        return occurences;
    }

    private static Collation collationParser(JsonNode jsonNode) throws UnknownCommand {
        Iterator<Map.Entry<String, JsonNode>> keyPairs = jsonNode.fields();
        Collation.Builder builder = Collation.builder();
        while(keyPairs.hasNext()) {
            Map.Entry<String, JsonNode> nestedNode = keyPairs.next();
            switch(Defines.CollationField.valueOf(nestedNode.getKey())) {
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
                    throw new UnknownCommand("Unknown field for Collation type.");
            }
        }
        return builder.build();
    }
}
