

```shell
mvn clean verify

docker cp target/keycloak-service-c-protocol-mapper.jar keycloak:/opt/jboss/keycloak/standalone/deployments/

curl -s http://localhost:8080/auth/realms/example/protocol/openid-connect/token -d grant_type=client_credentials -d client_id=ProtocolMapperTestClient -d client_secret=hWOVZkbeBx8Vl1oFp80UIBcdaEXsRGbA
```