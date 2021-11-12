package com.example.usermanagementwithjpaandtests.controllers;

import com.example.usermanagementwithjpaandtests.dtos.LoginRequest;
import com.example.usermanagementwithjpaandtests.dtos.LoginResponse;
import com.example.usermanagementwithjpaandtests.dtos.UserRequest;
import com.example.usermanagementwithjpaandtests.dtos.UserResponse;
import com.example.usermanagementwithjpaandtests.entities.User;
import com.example.usermanagementwithjpaandtests.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class UserManagementController {

    private final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    private final UserService userService;

    @Autowired
    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        ResponseEntity<LoginResponse> response = null;
        try {
            LoginResponse loginResponse = userService.login(loginRequest);
            if(loginResponse != null) {
                response = ResponseEntity.ok(loginResponse);
            } else {
                response = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        ResponseEntity<List<UserResponse>> response = null;
        try {
            List<UserResponse> users = userService.getAllUsers();
            response = ResponseEntity.ok(users);
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.id == #id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        ResponseEntity<UserResponse> response = null;
        try {
            UserResponse user = userService.getUserById(id);
            if(user != null) {
                response = ResponseEntity.ok(user);
            } else {
                response = ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        ResponseEntity<UserResponse> response = null;
        try {
            if(userService.isEmailAvailable(userRequest.getEmail())) {
                boolean wannaCreateAdmin = userService.hasAdminRole(userRequest);
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if(!wannaCreateAdmin || (principal != null && principal instanceof User && userService.hasAdminRole((User) principal))) {
                    UserResponse user = userService.createUser(userRequest);
                    response = new ResponseEntity<>(user, HttpStatus.CREATED);
                } else {
                    response = new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            } else {
                response = new ResponseEntity<>(HttpStatus.CONFLICT);
            }
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.id == #id")
    public ResponseEntity<UserResponse> editUser(@PathVariable("id") Long id, @Valid @RequestBody UserRequest userRequest) {
        ResponseEntity<UserResponse> response = null;
        try {
            boolean wannaCreateAdmin = userService.hasAdminRole(userRequest);
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(!wannaCreateAdmin || (principal != null && principal instanceof User && userService.hasAdminRole((User) principal))) {
                UserResponse user = userService.editUser(id, userRequest);
                if(user != null) {
                    response = ResponseEntity.ok(user);
                } else {
                    response = ResponseEntity.notFound().build();
                }
            } else {
                response = new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @DeleteMapping(path = "/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.id == #id")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        ResponseEntity<Void> response = null;
        try {
            boolean success = userService.deleteUser(id);
            if(success) {
                response = ResponseEntity.ok().build();
            } else {
                response = ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

}
