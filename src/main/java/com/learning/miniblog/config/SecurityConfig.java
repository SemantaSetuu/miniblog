package com.learning.miniblog.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;


@Configuration
public class SecurityConfig{
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.authorizeHttpRequests(auth->auth.requestMatchers("/api/posts").permitAll()  //Anyone can access: /api/posts //AuthorizeHttpRequestsConfigurer auth
                                            .anyRequest().authenticated()) //Everything else, requires login
                                            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}