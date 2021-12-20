package de.synyx.cl.oauth.examples.service.a.config;

import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String SERVICE_NAME = "service_a";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .anyRequest().authenticated()
                .and().oauth2Login();
    }


    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapperForKeycloak() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            var authority = authorities.iterator().next();
            boolean isOidc = authority instanceof OidcUserAuthority;

            if (isOidc) {
                var oidcUserAuthority = (OidcUserAuthority) authority;
                var userInfo = oidcUserAuthority.getUserInfo();

                System.out.println("---------------------------------------------------------------------------------");
                System.out.println("---------------------------------------------------------------------------------");
                System.out.println("" + oidcUserAuthority.getIdToken().getClaim("realm_access"));
                System.out.println("" + oidcUserAuthority.getIdToken().getClaim("resource_access"));

                final JSONObject realmAccess = oidcUserAuthority.getIdToken().getClaim("realm_access");
                if (realmAccess != null) {
                    System.out.println(realmAccess.get("roles"));
                    JSONArray realmRoles = (JSONArray) realmAccess.get("roles");
                    System.out.println(realmRoles);

                    for (Object role : realmRoles) {
                        System.out.println("realm role: " + role);

                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }

                final JSONObject resourceAccess = oidcUserAuthority.getIdToken().getClaim("resource_access");
                if (resourceAccess != null && resourceAccess.get(SERVICE_NAME) != null) {
                    System.out.println(resourceAccess.get(SERVICE_NAME));
                    JSONObject clientRoles = (JSONObject) resourceAccess.get(SERVICE_NAME);
                    for (Object role : (JSONArray)clientRoles.get("roles")) {
                        System.out.println("client role: " + role);
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }

                    System.out.println("---------------------------------------------------------------------------------");
                    System.out.println("---------------------------------------------------------------------------------");

                    if (userInfo.hasClaim("realm_access")) {
                        var claimRealmAccess = userInfo.getClaimAsMap("realm_access");
                        var roles = (Collection<String>) claimRealmAccess.get("roles");
                        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                    }
                }

            } else {
                var oauth2UserAuthority = (OAuth2UserAuthority) authority;
                Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

                if (userAttributes.containsKey("realm_access")) {
                    var realmAccess =  (Map<String,Object>) userAttributes.get("realm_access");
                    var roles =  (Collection<String>) realmAccess.get("roles");
                    mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                }
            }

            return mappedAuthorities;
        };
    }

    Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
