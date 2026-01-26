package com.yuxian.backend.component;

import com.yuxian.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataFixer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataFixer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        userRepository.fixNullVersions();
        System.out.println("DataFixer: Fixed null versions for users.");
    }
}
