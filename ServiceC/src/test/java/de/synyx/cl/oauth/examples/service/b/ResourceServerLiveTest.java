package de.synyx.cl.oauth.examples.service.b;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceServerLiveTest {

    public static final String baseUrl = "http://localhost:8082";
    private final String redirectUrl = "http://localhost:8084/";
    private final String authorizeUrlPattern = "http://localhost:8080/auth/realms/example/protocol/openid-connect/auth?response_type=code&client_id=service_a&scope=%s&redirect_uri=" + redirectUrl;
    private final String tokenUrl = "http://localhost:8080/auth/realms/example/protocol/openid-connect/token";


    public OAuth2ProtectedResourceDetails details() {
        AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
        details.setId("client_id");
        details.setClientId("service_a");
        details.setClientSecret("7f7367d1-f394-4a98-af5b-6c11886ff26f");
        details.setAccessTokenUri(tokenUrl);
        details.setUserAuthorizationUri(tokenUrl);
        return details;
    }

    @Test
    void testOAuthRestTemplate() {
        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setAccessTokenUri(tokenUrl);
        resource.setId("service_a");
        resource.setClientId("service_a");
        resource.setClientSecret("7f7367d1-f394-4a98-af5b-6c11886ff26f");

        resource.setGrantType("client_credentials");

        resource.setScope(Arrays.asList("openid"));

        OAuth2RestTemplate template = new OAuth2RestTemplate(resource);
        System.out.println(" CALLING: " + baseUrl+"/api");

        String result = template.getForObject(baseUrl+"/api", String.class);

        System.err.println(result);
        assertEquals("Hello, Trusted User marissa", result);
    }

    @Test
    void testRestAssured() {

        Response response = RestAssured.given()
                .redirects()
                .follow(false)
                .formParams("client_id", "service_a", "client_secret", "7f7367d1-f394-4a98-af5b-6c11886ff26f", "grant_type", "client_credentials")
                .post("http://localhost:8080/auth/realms/example/protocol/openid-connect/token");

        assertThat(HttpStatus.OK.value()).isEqualTo(response.getStatusCode());

        String[] parts = response.getBody().asString().split(":");
        String accessToken = parts[1].split("\"")[1];
        System.out.println("parts:" + accessToken);

        Response api = RestAssured.given()
                .redirects().follow(false)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get(baseUrl + "/api");

        api.then()
                .statusCode(200)
                .body("answer", equalTo(42));
    }



    String obtainAccesToken(String scopes) {
        Response response = RestAssured.given()
                .redirects()
                .follow(false)
                .get(String.format(authorizeUrlPattern, scopes));
        String authSessionId = response.getCookie("AUTH_SESSION_ID");
        String kcPostAuthenticationUrl = response.asString()
                .split("action=\"")[1].split("\"")[0].replace("&amp;", "&");

        // obtain authentication code and state
        response = RestAssured.given()
                .redirects()
                .follow(false)
                .cookie("AUTH_SESSION_ID", authSessionId)
                .formParams("client_id", "service_a", "client_secret", "7f7367d1-f394-4a98-af5b-6c11886ff26f", "grant_type", "client_credentials")
                .post(kcPostAuthenticationUrl);
        assertThat(HttpStatus.FOUND.value()).isEqualTo(response.getStatusCode());

        // extract authorization code
        String location = response.getHeader(HttpHeaders.LOCATION);
        String code = location.split("code=")[1].split("&")[0];

        // get access token
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("client_id", "jwtClient");
        params.put("redirect_uri", redirectUrl);
        params.put("client_secret", "jwtClientSecret");
        response = RestAssured.given()
                .formParams(params)
                .post(tokenUrl);
        return response.jsonPath()
                .getString("access_token");
    }
}
