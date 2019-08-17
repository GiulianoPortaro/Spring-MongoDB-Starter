package com.mongodb.starter.database.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
public class Audit {
    @CreatedDate
    @Field("created_date")
    @JsonIgnore
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Field("modified_date")
    @JsonIgnore
    private Instant modifiedDate = Instant.now();
}
