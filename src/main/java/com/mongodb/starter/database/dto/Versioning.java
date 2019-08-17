package com.mongodb.starter.database.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "versioning")
@Data
@NoArgsConstructor
public class Versioning {
    private String version;
    @CreatedDate
    @JsonIgnore
    private Instant installedOn = Instant.now();
    private String fileName;
    private String description;
    private String success;

    public Versioning(String version, String fileName, String description, String success) {
        this.version = version;
        this.fileName = fileName;
        this.description = description;
        this.success = success;
    }
}
