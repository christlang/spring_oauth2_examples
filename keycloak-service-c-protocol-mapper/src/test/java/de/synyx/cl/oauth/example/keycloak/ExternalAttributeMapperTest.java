package de.synyx.cl.oauth.example.keycloak;

import org.junit.jupiter.api.Test;

import javax.json.*;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static de.synyx.cl.oauth.example.keycloak.ExternalAttributeMapper.convert;
import static org.assertj.core.api.BDDAssertions.then;

class ExternalAttributeMapperTest {

    @Test
    void convertJsonStructure() {
        String body = "{ " +
                "\"rightA\" : true," +
                "\"rightB\" : false " +
                "}";
        JsonReader reader = Json.createReader(new StringReader(body));

        final JsonObject jsonObject = reader.readObject();

        Map<String, Boolean> result = convert(jsonObject);

        Map<String, Boolean> rights = new HashMap<>();
        rights.put("rightA", true);
        rights.put("rightB", false);

        then(result).isEqualTo(rights);
    }
}