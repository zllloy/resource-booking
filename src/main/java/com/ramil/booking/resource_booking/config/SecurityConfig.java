package com.ramil.booking.resource_booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/graphiql", "/graphiql/**").permitAll()
            .requestMatchers("/webjars/**", "/favicon.ico", "/error").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/graphql").permitAll()
            .requestMatchers("/graphql").authenticated()
            .anyRequest().denyAll())
        .httpBasic(Customizer.withDefaults())
        .build();
  }

  @Bean
  InMemoryUserDetailsManager users(PasswordEncoder encoder) {
    UserDetails admin = User.withUsername("admin")
        .password(encoder.encode("admin"))
        .roles("ADMIN")
        .build();

    UserDetails user = User.withUsername("user")
        .password(encoder.encode("user"))
        .roles("USER")
        .build();

    return new InMemoryUserDetailsManager(admin, user);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
