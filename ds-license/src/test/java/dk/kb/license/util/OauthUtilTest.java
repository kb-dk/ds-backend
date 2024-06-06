package dk.kb.license.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.util.Base64;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OauthUtilTest {

    private static final Logger log = LoggerFactory.getLogger(OauthUtilTest.class);

    //this is a access token from keycloak. It is expired though.
    //It is not possible to create a token that is not expired, so this is used for testing
    private static String JWT_ACCESS_TOKEN="{\"access_token\":\"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1a1c4c1ZMajNlVVAzaDJiZXk4UTROUHZQM3EzWXNZdThRMjY3elVfMFBjIn0.eyJleHAiOjE3MTc2NjIyNjAsImlhdCI6MTcxNzY2MjIwMCwiYXV0aF90aW1lIjoxNzE3NjYyMDQ4LCJqdGkiOiIyNTI5MDFkNC0wNGFiLTQzOTgtYjg0OS05NGI3ZTZjNWMxMGEiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLWRldmVsLTAxLmtiLmRrL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZjpmOTE5NjhlZC1hNDJhLTQ1MzYtOWFmNC1lZWU2NWU1ZDhmMjU6dGVnQGtiLmRrIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoia2ItYWQiLCJzZXNzaW9uX3N0YXRlIjoiY2RmNjExMTQtMTMxOC00OTQzLTkxNTctMDZlYjEyYzhlZmI1IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1tYXN0ZXIiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiY2RmNjExMTQtMTMxOC00OTQzLTkxNTctMDZlYjEyYzhlZmI1IiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiVGhvbWFzIEVnZW5zZSIsInByZWZlcnJlZF91c2VybmFtZSI6InRob21hcyBlZ2Vuc2UiLCJnaXZlbl9uYW1lIjoiVGhvbWFzIiwiZmFtaWx5X25hbWUiOiJFZ2Vuc2UiLCJlbWFpbCI6InRlZ0BrYi5kayJ9.M32zTn40Pj73jwhAicevDcElZxfhZK4Exkf-tX2NY3hmrG_7HNIP7AH0j_6BD_H1qhpbLK1vL_akwd9xcNUaupNoRsFypT_2w0xj3HWnF75qX78g8O2qGawIvOAs6b3I0q8ov9e8XYGiKa2uTfvvRLmJbVVFafhQErUgQdqYH05SYdqxdHzuK8PtAZMUjGjlJYwzhEhaumMKYAf3A588WMa7c-SoQR2t27tdqNf_2c_V7T3O7WiEycXpS96N2klLTYu8Rz-wvNgauXOU2H5YizKtbTHeC3xEi2-B1s_N0R0s4g9x9kN2Y0tdBrvs1c6TSJ9FfnNLXRejTpl0ZtPb7w\",\"expires_in\":60,\"refresh_expires_in\":1800,\"refresh_token\":\"eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIyN2NiNjBiMy0yZTcxLTQzZjktOWI3ZS03MDE1NzAwM2U2OWIifQ.eyJleHAiOjE3MTc2NjQwMDAsImlhdCI6MTcxNzY2MjIwMCwianRpIjoiYTYzNDY2NTgtNzEzNC00ZWI5LTk4YjctODFiYjliMjI2NTY5IiwiaXNzIjoiaHR0cHM6Ly9rZXljbG9hay1kZXZlbC0wMS5rYi5kay9yZWFsbXMvbWFzdGVyIiwiYXVkIjoiaHR0cHM6Ly9rZXljbG9hay1kZXZlbC0wMS5rYi5kay9yZWFsbXMvbWFzdGVyIiwic3ViIjoiZjpmOTE5NjhlZC1hNDJhLTQ1MzYtOWFmNC1lZWU2NWU1ZDhmMjU6dGVnQGtiLmRrIiwidHlwIjoiUmVmcmVzaCIsImF6cCI6ImtiLWFkIiwic2Vzc2lvbl9zdGF0ZSI6ImNkZjYxMTE0LTEzMTgtNDk0My05MTU3LTA2ZWIxMmM4ZWZiNSIsInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsInNpZCI6ImNkZjYxMTE0LTEzMTgtNDk0My05MTU3LTA2ZWIxMmM4ZWZiNSJ9.6oW5e0OTgtw2UcqNa3tMX42awPU1lL3IKZuPIOx2tsY\",\"token_type\":\"Bearer\",\"not-before-policy\":0,\"session_state\":\"cdf61114-1318-4943-9157-06eb12c8efb5\",\"scope\":\"profile email\"}";

    @Test
    public void testJWTBase64Decode() {
        JSONObject jwtJson = new JSONObject(JWT_ACCESS_TOKEN);
        String accessToken= jwtJson.getString("access_token");        

        String[] base64DecodeToken = OauthUtil.base64DecodeToken(accessToken);
        String header=base64DecodeToken[0];  //Example: {"alg":"RS256","typ" : "JWT","kid" : "5kW8sVLj3eUP3h2bey8Q4NPvP3q3YsYu8Q267zU_0Pc"}
        assertTrue(header.indexOf("\"RS256\"")>0);  //Just test algorithm is RS256
        String payload=base64DecodeToken[1]; // second entry. First is header
        String combinedUserEmail=OauthUtil.extractNameAndEmail(payload);
        assertEquals("Thomas Egense(teg@kb.dk)",combinedUserEmail);

        //Testing token is expired will require public RsaKey. Not secret but not worth including. Also token has expired
        //boolean valid=OauthUtil.isValidAccessToken(accessToken, "rsa public key") <-- Can be tried using RSA public key. Can be seen on KeyCloak server admin page    
    }


}
