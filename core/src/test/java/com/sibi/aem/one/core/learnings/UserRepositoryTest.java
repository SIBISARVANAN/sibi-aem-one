package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserRepositoryTest {

    UserRepository userRepository;

    @BeforeEach
    public void setUp(){
        userRepository = new UserRepository();
    }

    @Test
    void getUserName(){
        assertEquals("Real User", userRepository.getUserName());
    }
}
