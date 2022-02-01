package de.synyx.cl.oauth.examples.service.b;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ServiceBController {

    @GetMapping("/api")
    public String api() {
        return "{\"answer\": 42}";
    }

    @GetMapping("/info")
    public Map<String, Object> getUserInfo(Principal principal) {

        Map<String, String> map = new HashMap<>();
        if (principal instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) principal;
            map.put("clientId", jwtToken.getTokenAttributes().get("clientId").toString());
        }

        map.put("name", principal.getName());

        return Collections.unmodifiableMap(map);
    }
}
