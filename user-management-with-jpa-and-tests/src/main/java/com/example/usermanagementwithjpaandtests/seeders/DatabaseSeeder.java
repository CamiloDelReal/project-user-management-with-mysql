package com.example.usermanagementwithjpaandtests.seeders;

import com.example.usermanagementwithjpaandtests.entities.Role;
import com.example.usermanagementwithjpaandtests.entities.User;
import com.example.usermanagementwithjpaandtests.repositories.RoleRepository;
import com.example.usermanagementwithjpaandtests.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DatabaseSeeder {

    private final BCryptPasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Autowired
    public DatabaseSeeder(BCryptPasswordEncoder passwordEncoder, RoleRepository roleRepository, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @EventListener
    public void seed(ContextRefreshedEvent event) {
        seedRoles();
        seedUsers();
    }

    private void seedRoles() {
        if(roleRepository.count() == 0) {
            Role administrator = new Role(Role.ADMINISTRATOR);
            roleRepository.save(administrator);
            Role guest = new Role(Role.GUEST);
            roleRepository.save(guest);
        }
    }

    private void seedUsers() {
        if(userRepository.count() == 0) {
            Role administratorRole = roleRepository.findByName(Role.ADMINISTRATOR).orElse(null);
            User administratorUser = new User("Administrator", "Root", "root@gmail.com", passwordEncoder.encode("root"), Set.of(administratorRole));
            userRepository.save(administratorUser);
        }
    }

}
