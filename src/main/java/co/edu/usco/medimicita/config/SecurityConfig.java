package co.edu.usco.medimicita.config;

import co.edu.usco.medimicita.util.DataInitializer;
import co.edu.usco.medimicita.service.UserService; // Tu UserDetailsService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity // (1) Habilita la configuración de seguridad web de Spring Security
public class SecurityConfig {

    @Autowired
    private UserService userService; // (2) Inyecta tu UserDetailsService personalizado

    // (3) Bean para el PasswordEncoder que usaremos en toda la aplicación
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // (4) Configuración principal de HttpSecurity para definir reglas de acceso, login, logout, etc.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // (5) Rutas públicas: CSS, JS, imágenes, página de inicio, login, registro, etc.
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/vendors/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password", "/terms").permitAll()
                        // (6) Rutas específicas para roles (¡ASEGÚRATE QUE LOS NOMBRES DE ROL COINCIDAN CON LOS DE TU BD SIN "ROLE_")
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR") // Spring automáticamente añade "ROLE_"
                        .requestMatchers("/doctor/**").hasRole("MEDICO")
                        .requestMatchers("/patient/**").hasRole("PACIENTE")
                        // (7) Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // (8) Especifica la URL de la página de login y permite acceso a todos
                        .loginPage("/login")
                        // (9) URL a la que se envía el formulario de login (Spring Security la maneja)
                        .loginProcessingUrl("/login") // No necesitas un controlador para este POST
                        // (10) URL a la que redirigir tras un login exitoso
                        .defaultSuccessUrl("/default-success", true) // Un handler para redirigir según rol
                        // (11) URL a la que redirigir tras un login fallido
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        // (12) URL para procesar el logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        // (13) URL a la que redirigir tras un logout exitoso
                        .logoutSuccessUrl("/login?logout=true")
                        // (14) Invalidar la sesión HTTP
                        .invalidateHttpSession(true)
                        // (15) Limpiar cualquier autenticación de seguridad configurada
                        .clearAuthentication(true)
                        // (16) Eliminar cookies (opcional, pero bueno para logout completo)
                        .deleteCookies("JSESSIONID") // Añade otras cookies si las usas
                        .permitAll()
                )
                // (17) Configuración de "Remember Me" (opcional)
                .rememberMe(rememberMe -> rememberMe
                        .key("uniqueAndSecret") // Clave secreta para remember-me
                        .tokenValiditySeconds(86400) // 1 día de validez
                        .userDetailsService(userService) // Necesita el UserDetailsService
                )
                // (18) CSRF está habilitado por defecto, lo cual es bueno para aplicaciones web con sesiones y formularios.
                // Si tuvieras una API RESTful stateless con JWT, podrías deshabilitarlo: .csrf(csrf -> csrf.disable())
                // pero para Thymeleaf + Sesiones, MANTENLO HABILITADO.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")); // Ejemplo si tienes APIs REST que no usan CSRF tokens

        return http.build();
    }

    // (19) Configura el AuthenticationManagerBuilder para usar tu UserDetailsService y PasswordEncoder
    // Este metodo es más común en configuraciones más antiguas o si necesitas un bean AuthenticationManager explícito.
    // Con la configuración de SecurityFilterChain, Spring Boot a menudo puede inferirlo.
    // Si lo necesitas para @Autowired AuthenticationManager en otro lugar:
    /*
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService)
                                    .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
    */

    // Alternativamente, para configurar el AuthenticationProvider directamente:
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userService) // (20) Le dice a Spring Security cómo cargar usuarios
                .passwordEncoder(passwordEncoder()); // (20) Le dice cómo verificar contraseñas
    }
}