package de.synyx.cl.oauth.examples.service.b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ServiceBController {

    private final Logger log = LoggerFactory.getLogger(ServiceBController.class);

    @GetMapping("/api")
    public String api(Principal principal) {
        log.info(principal.toString());
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
