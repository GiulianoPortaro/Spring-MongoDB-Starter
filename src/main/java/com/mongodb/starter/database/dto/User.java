package com.mongodb.starter.database.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "user")
@Getter
@Setter
@NoArgsConstructor
public class User extends Audit{
    @Id
    private String id;

    private String name;
    private String surname;

    @Indexed(unique=true)
    private String mail;

    private int age;
    private Address address;

    public User(String name, String surname, String mail, int age, Address address) {
        this.name = name;
        this.surname = surname;
        this.mail = mail;
        this.age = age;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return age == user.age &&
                Objects.equals(name, user.name) &&
                Objects.equals(surname, user.surname) &&
                Objects.equals(mail, user.mail) &&
                Objects.equals(address, user.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, surname, mail, age, address);
    }
}
