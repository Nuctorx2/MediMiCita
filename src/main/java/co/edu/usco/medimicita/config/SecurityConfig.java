package co.edu.usco.medimicita.config;// En SecurityConfig.java (ejemplo)
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
// @EnableWebSecurity // (Necesario si no usas Spring Boot auto-configuration)
public class SecurityConfig { // O el nombre que le des

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Usamos BCrypt
    }

    // ... resto de la configuración de HttpSecurity, UserDetailsService, etc.
}