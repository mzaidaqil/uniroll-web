package org.zayed.unirollweb.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Swagger UI at /swagger-ui.html. "Authorize" takes the accessToken from /api/auth/login
// and sends it as "Authorization: Bearer <token>" on every request.
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "UniRoll API", version = "v1",
                description = "Course enrollment: lecturers manage subjects, students enroll in them"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
