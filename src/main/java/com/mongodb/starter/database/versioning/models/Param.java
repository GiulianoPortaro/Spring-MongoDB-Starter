package com.mongodb.starter.database.versioning.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@NoArgsConstructor
@Getter
public class Param {
    private Object queryParameter;
    private Class<?> objectClass;

    public Param(Object queryParameter) {
        this.queryParameter = queryParameter;
        this.objectClass = queryParameter.getClass();
    }
}
