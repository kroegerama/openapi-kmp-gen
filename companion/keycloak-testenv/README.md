# Keycloak integration test environment

A throwaway Keycloak instance for `KeycloakIntegrationTest` (in `src/jvmTest`), pre-provisioned via realm import with everything the `keycloak`
package's API surface needs:

| Item                     | Value                                                                                                                                            |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Realm                    | `test` (access token lifespan: 30 s)                                                                                                             |
| Public client            | `test-public` - Direct Access Grants + Authorization Code, PKCE forced to `S256`, redirect URIs `http://localhost:8123/*` and `myapp://callback` |
| Confidential client      | `test-confidential` / secret `test-secret` - service accounts (client credentials)                                                               |
| User                     | `tester` / `password`                                                                                                                            |
| Admin console (`/admin`) | `admin` / `admin`                                                                                                                                |

## Usage

```powershell
docker compose up -d          # in this directory; ready once /realms/test responds
```

The integration tests are gated on `KEYCLOAK_BASE_URL` and silently skip when it is not set:

```powershell
$env:KEYCLOAK_BASE_URL = "http://localhost:8081"
./gradlew :companion:jvmTest --tests "*KeycloakIntegrationTest"
```

The tests only read and create sessions - the instance does not need to be reset between runs. Stop and discard it with `docker compose down`.
