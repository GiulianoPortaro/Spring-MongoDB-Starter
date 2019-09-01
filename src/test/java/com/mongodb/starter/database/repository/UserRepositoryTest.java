package com.mongodb.starter.database.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.database.dto.Address;
import com.mongodb.starter.database.dto.User;
import com.mongodb.starter.database.repository.impl.UserRepositoryCustomImpl;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations= "classpath:test.properties")
public class UserRepositoryTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Autowired UserRepository userRepository;
    @Autowired UserRepositoryCustomImpl userRepositoryCustom;
    @Autowired ObjectMapper objectMapper;

    @Before
    public void loadContext() throws IOException, URISyntaxException {
        String[] users = Files.readString(Paths.get(UserRepositoryTest.class.getResource("/users.json").toURI())).split("\r\n");
        ObjectMapper objectMapper = new ObjectMapper();
        for(String user : users) {
            userRepository.save(objectMapper.readValue(user, User.class));
        }
    }

    @After
    public void clearContext() {
        userRepository.deleteAll();
    }

    @Test
    public void insert() {
        User user = createUser("paolo.depini@mail.com");
        Assert.assertEquals(user, userRepository.insert(user));
    }

    @Test
    public void find() {
        Optional<User> user = userRepository.findByMail("antonio.rossi@mail.com");
        user.orElseThrow();

        insert();

        List<User> users = userRepository.findByName("Paolo");
        Assert.assertEquals(2, users.size());

        users = userRepository.findByNameAndSurname("Paolo", "De Pini");
        Assert.assertEquals(1, users.size());

        users = userRepository.findByNameContaining("Paolo");
        Assert.assertEquals(3, users.size());
    }

    @Test
    public void not_find() {
        thrown.expect(NoSuchElementException.class);
        Optional<User> user = userRepository.findByMail("noUserPresent@mail.com");
        user.orElseThrow();
    }

    @Test
    public void duplicate_mail() {
        thrown.expect(DuplicateKeyException.class);
        User user = createUser("antonio.rossi@mail.com");
        userRepository.insert(user);
    }

    @Test
    public void update() {
        User user = userRepository.findByMail("antonio.rossi@mail.com").orElseThrow();
        user.setName("Gino");
        userRepository.save(user);
        Assert.assertEquals("Gino", userRepository.findByMail("antonio.rossi@mail.com").orElseThrow().getName());
    }

    @Test
    public void updateAddress() {
        User user = userRepository.findByMail("antonio.rossi@mail.com").orElseThrow();
        Address address = new Address("Via delle Città", "PA", 56);
        userRepositoryCustom.updateAddress(user.getId(), objectMapper.valueToTree(address));
        User userNew = userRepository.findByMail("antonio.rossi@mail.com").orElseThrow();
        user.setAddress(address);
        Assert.assertEquals(user, userNew);
    }

    private User createUser(String mail) {
        Address address = new Address("Via Delle Picche", "NO", 124);
        return new User("Paolo", "De Pini", mail, 22, address);
    }
}
