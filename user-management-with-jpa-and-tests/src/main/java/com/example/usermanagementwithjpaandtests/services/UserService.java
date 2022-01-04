package com.example.usermanagementwithjpaandtests.services;

import com.example.usermanagementwithjpaandtests.dtos.LoginRequest;
import com.example.usermanagementwithjpaandtests.dtos.LoginResponse;
import com.example.usermanagementwithjpaandtests.dtos.UserRequest;
import com.example.usermanagementwithjpaandtests.dtos.UserResponse;
import com.example.usermanagementwithjpaandtests.entities.Role;
import com.example.usermanagementwithjpaandtests.entities.User;
import com.example.usermanagementwithjpaandtests.entities.UserRole;
import com.example.usermanagementwithjpaandtests.repositories.RoleRepository;
import com.example.usermanagementwithjpaandtests.repositories.UserRepository;
import com.example.usermanagementwithjpaandtests.repositories.UserRoleRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final AuthenticationManager authenticationManager;
    private final ModelMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${security.token-type}")
    private String tokenType;
    @Value("${security.token-key}")
    private String tokenKey;
    @Value("${security.separator}")
    private String separator;
    @Value("${security.authorities-key}")
    private String authoritiesKey;
    @Value("${security.validity}")
    private Long validity;

    @Autowired
    public UserService(@Lazy AuthenticationManager authenticationManager, ModelMapper mapper, BCryptPasswordEncoder passwordEncoder, UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.authenticationManager = authenticationManager;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails userDetails = null;
        User user = userRepository.findByEmail(username).orElse(null);
        if (user != null) {
            user.getRoles().forEach(System.out::println);
            List<GrantedAuthority> authorities = user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).collect(Collectors.toList());
            userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), user.getProtectedPassword(), true, true, true, true, authorities);
        }
        return userDetails;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public LoginResponse login(LoginRequest loginRequest) {
        LoginResponse response = null;
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
            User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
            if (user != null) {
                String rolesClaim = user.getRoles().stream().map(Role::getName).collect(Collectors.joining(separator));
                String subject = String.join(separator, String.valueOf(user.getId()), user.getEmail());
                long timestamp = System.currentTimeMillis();
                Claims claims = Jwts.claims();
                claims.put(authoritiesKey, rolesClaim);
                String token = Jwts.builder()
                        .setClaims(claims)
                        .setSubject(subject)
                        .setIssuedAt(new Date(timestamp))
                        .setExpiration(new Date(timestamp + validity))
                        .signWith(SignatureAlgorithm.HS256, tokenKey)
                        .compact();
                response = new LoginResponse(user.getEmail(), tokenType, token);
            }
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
        }
        return response;
    }

    public List<UserResponse> getAllUsers() {
        List<UserResponse> response = null;
        List<User> users = userRepository.findAll();
        if (users != null && !users.isEmpty()) {
            response = users.stream().map(u -> mapper.map(u, UserResponse.class)).collect(Collectors.toList());
        } else {
            response = new ArrayList<>();
        }
        return response;
    }

    public UserResponse getUserById(Long id) {
        UserResponse response = null;
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            response = mapper.map(user, UserResponse.class);
        }
        return response;
    }

    public UserResponse createUser(UserRequest userRequest) {
        User user = mapper.map(userRequest, User.class);
        user.setProtectedPassword(passwordEncoder.encode(userRequest.getPassword()));
        List<Role> roles = null;
        if (userRequest.getRoles() != null) {
            roles = userRequest.getRoles().stream().map(it -> roleRepository.findByName(it).orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
        }

        if (userRequest.getRoles() == null || (roles != null && roles.isEmpty())) {
            Role guestRole = roleRepository.findByName(Role.GUEST).orElse(null);
            roles = List.of(guestRole);
        }
        user.setRoles(roles);
        userRepository.save(user);
        return mapper.map(user, UserResponse.class);
    }

    public UserResponse editUser(Long id, UserRequest userRequest) {
        UserResponse response = null;
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            System.out.println(user);
            user.setFirstName(userRequest.getFirstName());
            user.setLastName(userRequest.getLastName());
            user.setEmail(userRequest.getEmail());
            user.setProtectedPassword(passwordEncoder.encode(userRequest.getPassword()));
            userRoleRepository.deleteRolesForUser(user.getId());
            List<Role> roles = null;
            if (userRequest.getRoles() != null) {
                roles = userRequest.getRoles().stream().map(it -> roleRepository.findByName(it).orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
            }

            if (userRequest.getRoles() == null || (roles != null && roles.isEmpty())) {
                Role guestRole = roleRepository.findByName(Role.GUEST).orElse(null);
                roles = List.of(guestRole);
            }

            user.setRoles(null);
            userRepository.save(user);

            roles.stream().forEach(r -> userRoleRepository.save(new UserRole(new UserRole.UserRoleId(user.getId(), r.getId()))));
            user.setRoles(roles);

            response = mapper.map(user, UserResponse.class);
        }
        return response;
    }

    public boolean isEmailAvailable(String email) {
        return userRepository.findByEmail(email).orElse(null) == null;
    }

    public boolean hasAdminRole(UserRequest userRequest) {
        boolean has = false;
        if (userRequest.getRoles() != null) {
            Role adminRole = roleRepository.findByName(Role.ADMINISTRATOR).orElse(null);
            has = (adminRole != null && userRequest.getRoles().stream().anyMatch(it -> Objects.equals(adminRole.getName(), it)));
        }
        return has;
    }

    public boolean hasAdminRole(User user) {
        boolean has = false;
        if (user.getRoles() != null) {
            Role adminRole = roleRepository.findByName(Role.ADMINISTRATOR).orElse(null);
            has = (adminRole != null && user.getRoles().stream().anyMatch(it -> Objects.equals(adminRole.getId(), it.getId())));
        }
        return has;
    }

    public boolean deleteUser(Long id) {
        boolean success = false;
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            userRoleRepository.deleteRolesForUser(user.getId());
            userRepository.delete(user);
            success = true;
        }
        return success;
    }
}
