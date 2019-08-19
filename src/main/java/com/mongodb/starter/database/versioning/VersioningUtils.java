package com.mongodb.starter.database.versioning;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.*;
import com.mongodb.starter.database.versioning.exception.UnknownCollectionOperation;
import com.mongodb.starter.database.versioning.exception.UnknownCommand;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VersioningUtils {

    public static List<?> queryAnalyze(String[] queryParts) throws UnknownCommand, UnknownCollectionOperation {
        String operation = queryParts[2].substring(queryParts[2].indexOf("(") + 1, queryParts[2].length() - 1);
        String collectionOperation = queryParts[2].substring(0, queryParts[2].indexOf("("));
        switch (Defines.CollectionOperations.valueOf(collectionOperation)) {
            /* Example
            db.collection.createIndex(
                { category: 1 },
                { name: "category_fr", collation: { locale: "fr", strength: 2 } }
            )
            */
            case createIndex:
                int selector = countOccurences(operation, '{', 0);
                DBObject dbObject;
                IndexOptions indexOptions = new IndexOptions();
                ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                if(selector > 1) {
                    String indexes = operation.substring(operation.indexOf("{"), operation.indexOf("}") + 1);
                    dbObject = BasicDBObject.parse(indexes);
                    String options = operation.substring(indexes.length() + 2).trim();

                    Collation collation = null;
                    Bson storageEngine = null;
                    Bson partialFilterExpression = null;

                    ObjectNode objectNode;
                    try {
                        objectNode = (ObjectNode) objectMapper.readTree(options);
                    } catch (IOException e) {
                        return new ArrayList<>();
                    }

                    if(objectNode.get("collation") != null) {
                        collation = collationParser(objectNode.get("collation"));
                        objectNode.remove("collation");
                    }
                    if(objectNode.get("storageEngine") != null) {
                        storageEngine = BsonDocument.parse(objectNode.get("storageEngine").toString());
                        objectNode.remove("storageEngine");
                    }
                    if(objectNode.get("partialFilterExpression") != null) {
                        partialFilterExpression = BsonDocument.parse(objectNode.get("partialFilterExpression").toString());
                        objectNode.remove("partialFilterExpression");
                    }

                    indexOptions = objectMapper.convertValue(objectNode, IndexOptions.class);
                    indexOptions.collation(collation);
                    indexOptions.storageEngine(storageEngine);
                    indexOptions.partialFilterExpression(partialFilterExpression);
                }
                else {
                    dbObject = BasicDBObject.parse(operation);
                }
                IndexOptions finalIndexOptions = indexOptions;
                return new ArrayList<>(){{
                    add(queryParts[1]);
                    add(Defines.COL_CREATE_INDEX);
                    add(dbObject);
                    add(finalIndexOptions);
                }};
            case createIndexes:

                return new ArrayList<>();
            default:
                throw new UnknownCollectionOperation("Unknown collection operation.");
        }
    }

    private static int countOccurences(String someString, char searchedChar, int index) {
        if (index >= someString.length()) {
            return 0;
        }
        int count = someString.charAt(index) == searchedChar ? 1 : 0;
        return count + countOccurences(someString, searchedChar, index + 1);
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
