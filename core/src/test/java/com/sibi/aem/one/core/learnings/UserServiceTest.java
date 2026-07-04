package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    UserRepository userRepository;
    Logger logger;
    UserService userservice;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        logger = mock(Logger.class);
        userservice = new UserService(userRepository, logger);
    }

    @Test
    void fetchUser(){
        when(userRepository.getUserName()).thenReturn("Real User");
        assertEquals("Real User", userservice.fetchUser());
        verify(userRepository, times(1)).getUserName();
        verify(logger, times(1)).debug(anyString());
    }

}
