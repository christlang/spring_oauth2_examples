package de.synyx.cl.oauth.examples.service.b;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt principal) {
        Map<String, String> map = new HashMap<>();
        map.put("user_name", principal.getClaimAsString("preferred_username"));
        map.put("organization", principal.getClaimAsString("organization"));
        return Collections.unmodifiableMap(map);
    }
}
