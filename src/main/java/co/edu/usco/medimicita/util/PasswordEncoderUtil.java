//package co.edu.usco.medimicita.util; // O el paquete que elijas
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component // (1) Anota con @Component para que Spring lo ejecute al iniciar
//public class PasswordEncoderUtil implements CommandLineRunner {
//
//    @Override
//    public void run(String... args) throws Exception {
//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // (2) Crea una instancia de BCrypt
//
//        // (3) Define las contraseñas en texto plano que quieres hashear
//        String adminPassword = "admin123";
//        String medicoPassword = "medico123";
//        String pacientePassword = "paciente123";
//        // Añade más si necesitas
//
//        // (4) Hashea cada contraseña e imprímela en la consola
//        System.out.println("--- BCrypt Hashes Generados ---");
//        System.out.println("Contraseña: " + adminPassword + " -> Hash: " + passwordEncoder.encode(adminPassword));
//        System.out.println("Contraseña: " + medicoPassword + " -> Hash: " + passwordEncoder.encode(medicoPassword));
//        System.out.println("Contraseña: " + pacientePassword + " -> Hash: " + passwordEncoder.encode(pacientePassword));
//        System.out.println("-------------------------------");
//
//        // (5) Opcional: Para salir de la aplicación después de imprimir si solo quieres esto.
//        // System.exit(0);
//    }
//}