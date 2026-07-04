package com.sibi.aem.one.core.learnings;

import org.slf4j.Logger;

public class UserService {

    private UserRepository repository;
    private Logger logger;

    public UserService(UserRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    public String fetchUser() {
        logger.debug("Fetching user");
        return repository.getUserName();
    }
}