package com.mongodb.starter.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

public class StringUtils {
    public static List<String> splitWithDelimiter(String stringToSplit, String startDelimiter, String endDelimiter, String regex) {
        stringToSplit = stringToSplit.replace(";", "").trim();
        String firstPart = stringToSplit.substring(0, stringToSplit.indexOf(startDelimiter)).trim();
        String secondPart = stringToSplit.substring(stringToSplit.indexOf(startDelimiter) + 1, stringToSplit.indexOf(endDelimiter)).trim();
        List<String> ret =  new ArrayList<>(Arrays.asList(firstPart.split(regex)));
        ret.add(secondPart);
        return ret;
    }

    public static String subStringWithDelimiter(String someString, char startChar, char endChar) {
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

    public static int countOccurences(String someString, char startChar, char endChar) {
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
}
