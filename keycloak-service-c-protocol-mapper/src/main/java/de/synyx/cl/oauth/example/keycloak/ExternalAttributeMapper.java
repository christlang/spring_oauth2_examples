package de.synyx.cl.oauth.example.keycloak;

import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.services.Urls;

import javax.json.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * inspired by https://github.com/mschwartau/keycloak-custom-protocol-mapper-example
 */
public class ExternalAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    /*
     * A config which keycloak uses to display a generic dialog to configure the token.
     */
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /*
     * The ID of the token mapper. Is public, because we need this id in our data-setup project to
     * configure the protocol mapper in keycloak.
     */
    public static final String PROVIDER_ID = "oidc-service-c-protocol-mapper";

    private static final String URL_NAME = PROVIDER_ID + ".url";

    static {
        // The builtin protocol mapper let the user define under which claim name (key)
        // the protocol mapper writes its value. To display this option in the generic dialog
        // in keycloak, execute the following method.
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

        ProviderConfigProperty propertyUrl = new ProviderConfigProperty();
        propertyUrl.setName(URL_NAME);
        propertyUrl.setLabel("URL");
        propertyUrl.setType(ProviderConfigProperty.STRING_TYPE);
        propertyUrl.setHelpText("URL of the service to retrive information from");
        configProperties.add(propertyUrl);

        // The builtin protocol mapper let the user define for which tokens the protocol mapper
        // is executed (access token, id token, user info). To add the config options for the different types
        // to the dialog execute the following method. Note that the following method uses the interfaces
        // this token mapper implements to decide which options to add to the config. So if this token
        // mapper should never be available for some sort of options, e.g. like the id token, just don't
        // implement the corresponding interface.
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ExternalAttributeMapper.class);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Service C Attribute Mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds information from service c to claim";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        // adds our data to the token. Uses the parameters like the claim name which were set by the user
        // when this protocol mapper was configured in keycloak. Note that the parameters which can
        // be configured in keycloak for this protocol mapper were set in the static initializer of this class.
        //
        // Sets a static "Hello world" string, but we could write a dynamic value like a group attribute here too.
        String accessToken = getAccessToken(clientSessionCtx.getClientSession().getClient().getId(), keycloakSession);


        System.out.println("***************************************");
        System.out.println("1:");
        System.out.println(accessToken);
        System.out.println("***************************************");

        KeyWrapper key = keycloakSession.keys().getActiveKey(keycloakSession.getContext().getRealm(), KeyUse.SIG, "RS256");
        String bearerToken = new JWSBuilder().kid(key.getKid()).type("JWT").jsonContent(token).sign(new AsymmetricSignatureSignerContext(key));
        System.out.println("2:");
        System.out.println(bearerToken);
        System.out.println("***************************************");

        String url = mappingModel.getConfig().get(URL_NAME);

        JsonObject attributes = getExternalAttribute(url, bearerToken);
        final Map<String, Boolean> list = convert(attributes);

        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, list);
    }

    private JsonObject getExternalAttribute(String url, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .setHeader("User-Agent", ExternalAttributeMapper.class.getSimpleName())
                .setHeader("Authorization", "Bearer " + token)
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("request not successful: " + response.statusCode());
            }
            String body = response.body();

            System.out.println("response body: " + body);
            JsonReader reader = Json.createReader(new StringReader(body));
            return reader.readObject();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return Json.createReader(new StringReader("{\"error\": \"unknown error\"}")).readObject();
        }
    }

    // https://stackoverflow.com/questions/63742225/how-to-obtain-an-access-token-within-a-keycloak-spi
    public String getAccessToken(String userId, KeycloakSession keycloakSession) {
        KeycloakContext keycloakContext = keycloakSession.getContext();

        AccessToken token = new AccessToken();
        token.subject(userId);
        token.issuer(Urls.realmIssuer(keycloakContext.getUri().getBaseUri(), keycloakContext.getRealm().getName()));
        token.issuedNow();
        token.type("Bearer");
        token.expiration((int) (token.getIat() + 60L)); //Lifetime of 60 seconds

        KeyWrapper key = keycloakSession.keys().getActiveKey(keycloakContext.getRealm(), KeyUse.SIG, "RS256");

        return new JWSBuilder().kid(key.getKid()).type("JWT").jsonContent(token).sign(new AsymmetricSignatureSignerContext(key));
    }

    public static Map<String, Boolean> convert(JsonObject jsonObject) {
        Map<String, Boolean> result = new HashMap<>();
        for(String key: jsonObject.keySet()) {
            final JsonValue jsonValue = jsonObject.get(key);
            result.put(key, jsonValue.toString().equals("true") ? true : false);
        };

        return result;
    }
}
