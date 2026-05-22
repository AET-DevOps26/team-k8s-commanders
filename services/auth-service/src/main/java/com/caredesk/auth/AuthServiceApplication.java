package com.caredesk.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages ensures Spring only picks up our beans, not the generated
// org.openapitools controllers (AuthApiController etc.) from the stubs JAR
@SpringBootApplication(scanBasePackages = "com.caredesk.auth")
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
