package com.mongodb.starter.database.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
public class Audit implements Serializable {
    @CreatedDate
    @Field("created_date")
    @JsonIgnore
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Field("modified_date")
    @JsonIgnore
    private Instant modifiedDate = Instant.now();
}
