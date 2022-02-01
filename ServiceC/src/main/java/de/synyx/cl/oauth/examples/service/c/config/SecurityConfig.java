package de.synyx.cl.oauth.examples.service.b.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private String audience = "test";
//    private String issuer = "http://localhost:8080/auth/realms/example/protocol/openid-connect/certs";
    private String issuer = "http://localhost:8080/auth/realms/example";

    String jwkSetUri = "https://localhost:8080/auth/realms/example/protocol/openid-connect/certs";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().authenticated()
                .and().oauth2ResourceServer().jwt();
    }
}
