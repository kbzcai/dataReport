package com.sjtb.reporting.security;

import com.sjtb.reporting.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception { return configuration.getAuthenticationManager(); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username).map(user -> org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername()).password(user.getPassword()).disabled(!user.isEnabled())
                .authorities(java.util.stream.Stream.concat(user.getRoles().stream().map(role -> "ROLE_" + role.name()), user.getPermissions().stream().map(permission -> "PERM_" + permission.name())).toArray(String[]::new)).build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    @Bean AuthenticationProvider authenticationProvider(UserDetailsService service, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(); provider.setUserDetailsService(service); provider.setPasswordEncoder(encoder); return provider;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter filter, AuthenticationProvider provider) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/login", "/actuator/health").permitAll().anyRequest().authenticated())
                .authenticationProvider(provider).addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
