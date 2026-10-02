package org.zayed.unirollweb;

import org.springframework.boot.SpringApplication;

public class TestUnirollWebApplication {

    public static void main(String[] args) {
        SpringApplication.from(UnirollWebApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
