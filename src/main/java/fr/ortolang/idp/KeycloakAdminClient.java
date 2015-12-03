package fr.ortolang.idp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.util.JsonSerialization;

public class KeycloakAdminClient {

    private String username;
    private String password;
    private String realm;
    private String clientId;
    private String baseUrl;

    public KeycloakAdminClient(String username, String password, String realm, String clientId, String baseUrl) {
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.clientId = clientId;
        this.baseUrl = baseUrl;
    }
    
    @SuppressWarnings("serial")
    static class TypedList extends ArrayList<IdentityProviderRepresentation> {
    }

    @SuppressWarnings("serial")
    public static class Failure extends Exception {
        private int status;

        public Failure(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    public String getContent(HttpEntity entity) throws IOException {
        if (entity == null)
            return null;
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            return new String(bytes);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }
    }

    public AccessTokenResponse getToken() throws IOException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(baseUrl).path(ServiceUrlConstants.TOKEN_PATH).build(realm));
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("username", username));
            formparams.add(new BasicNameValuePair("password", password));
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                String json = getContent(entity);
                throw new IOException("Bad status: " + status + " response: " + json);
            }
            if (entity == null) {
                throw new IOException("No Entity");
            }
            String json = getContent(entity);
            return JsonSerialization.readValue(json, AccessTokenResponse.class);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void logout(AccessTokenResponse res) throws IOException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(baseUrl).path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(realm));
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, res.getRefreshToken()));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);
            HttpResponse response = client.execute(post);
            boolean status = response.getStatusLine().getStatusCode() != 204;
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return;
            }
            InputStream is = entity.getContent();
            if (is != null)
                is.close();
            if (status) {
                throw new RuntimeException("failed to logout");
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
    
    public List<IdentityProviderRepresentation> listIdps(AccessTokenResponse res) throws Failure {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpGet get = new HttpGet(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances");
            get.addHeader("Authorization", "Bearer " + res.getToken());
            get.addHeader("Content-type", "application/json");
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, TypedList.class);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void createIDP(AccessTokenResponse res, IdentityProviderRepresentation idp) throws Failure {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPost post = new HttpPost(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances");
            post.addHeader("Authorization", "Bearer " + res.getToken());
            post.addHeader("Content-type", "application/json");
            try {
                post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(idp), "UTF-8"));
                HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() != 201) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public IdentityProviderRepresentation getIDP(AccessTokenResponse res, String alias) throws Failure {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            try {
                HttpGet get = new HttpGet(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias);
                get.addHeader("Authorization", "Bearer " + res.getToken());
                get.addHeader("Content-type", "application/json");
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, IdentityProviderRepresentation.class);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void updateIDP(AccessTokenResponse res, String alias, IdentityProviderRepresentation idp) throws Failure {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPut put = new HttpPut(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias);
            put.addHeader("Authorization", "Bearer " + res.getToken());
            put.addHeader("Content-type", "application/json");
            try {
                put.setEntity(new StringEntity(JsonSerialization.writeValueAsString(idp), "UTF-8"));
                HttpResponse response = client.execute(put);
                if (response.getStatusLine().getStatusCode() != 204) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void deleteIDP(AccessTokenResponse res, String alias) throws Failure {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpDelete delete = new HttpDelete(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias);
            delete.addHeader("Authorization", "Bearer " + res.getToken());
            delete.addHeader("Content-type", "application/json");
            try {
                HttpResponse response = client.execute(delete);
                if (response.getStatusLine().getStatusCode() != 204) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public String getAuthUrlBase() {
        return baseUrl;
    }

}