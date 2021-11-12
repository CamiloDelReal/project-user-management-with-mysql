package com.example.usermanagementwithjpaandtests.security;

import com.example.usermanagementwithjpaandtests.entities.User;
import com.example.usermanagementwithjpaandtests.services.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

//@Component
public class AuthorizationFilterReplacingAuth extends BasicAuthenticationFilter {

    private final Logger logger = LoggerFactory.getLogger(AuthorizationFilterBeforeAuth.class);
    private final UserService userService;

    @Value("${security.token-type}")
    private String tokenType;
    @Value("${security.token-key}")
    private String tokenKey;
    @Value("${security.separator}")
    private String separator;

    public AuthorizationFilterReplacingAuth(AuthenticationManager authenticationManager, UserService userService) {
        super(authenticationManager);
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String headerAuthorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(headerAuthorization != null && headerAuthorization.startsWith(tokenType)) {
            UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
            if(authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Unauthorize wrong credentials, doesn't matter what
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } // We will allow anonymous authentication, prefilter will handle authorization
        super.doFilterInternal(request, response, chain);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = null;
        String headerAuthorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = headerAuthorization.replace(tokenType, "");
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(tokenKey)
                    .parseClaimsJws(token)
                    .getBody();
            String[] subjectData = claims.getSubject().split(separator);
            if(subjectData != null && subjectData[0] != null && subjectData[1] != null) {
                Long id = Long.parseLong(subjectData[0]);
                String email = subjectData[1];
                User user = userService.getUserByEmail(email);
                if(user != null) {
                    Set<GrantedAuthority> authorities = user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).collect(Collectors.toSet());
                    authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                }
            }
        } catch (Exception ex) {
            logger.error("Exception captured", ex);
        }
        return authentication;
    }
}
