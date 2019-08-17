package com.mongodb.starter.database.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Address {
    private String address;
    private String province;
    private int number;

    public Address(String address, String province, int number) {
        this.address = address;
        this.province = province;
        this.number = number;
    }
}
