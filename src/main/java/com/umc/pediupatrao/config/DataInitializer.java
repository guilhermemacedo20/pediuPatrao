package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo) {
        return args -> {

            if (repo.findByUsername("admin").isEmpty()) {

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                Usuario user = new Usuario();
                user.setUsername("admin");
                user.setPassword(encoder.encode("teste123"));
                user.setRole("ADMIN");

                repo.save(user);

                System.out.println("Usuário admin criado!");
            }

            if (repo.findByUsername("gerente").isEmpty()) {

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                Usuario user = new Usuario();
                user.setUsername("gerente");
                user.setPassword(encoder.encode("teste123"));
                user.setRole("GERENTE");

                repo.save(user);

                System.out.println("Usuário gerente criado!");
            }

            if (repo.findByUsername("atendente").isEmpty()) {

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                Usuario user = new Usuario();
                user.setUsername("atendente");
                user.setPassword(encoder.encode("teste123"));
                user.setRole("ATENDENTE");

                repo.save(user);

                System.out.println("Usuário atendente criado!");
            }

            repo.findByUsername("admin").ifPresent(admin -> {
                String role = admin.getRole();
                if (role == null || role.isBlank() || role.contains("USER")) {
                    admin.setRole("ADMIN");
                    repo.save(admin);
                    System.out.println("Role do usuário admin alterada para ADMIN");
                }
            });
        };
    }
}