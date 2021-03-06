package fr.ortolang.discovery.keycloak;

/*-
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2017 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

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
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.util.JsonSerialization;

import fr.ortolang.discovery.entity.EntityDescriptor;

public class  KeycloakSynchronizer {
    
    private static final Logger LOGGER = Logger.getLogger(KeycloakSynchronizer.class.getName());

    private String username;
    private String password;
    private String realm;
    private String clientId;
    private String baseUrl;

    public KeycloakSynchronizer(String username, String password, String realm, String clientId, String baseUrl) {
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.clientId = clientId;
        this.baseUrl = baseUrl;
    }
    
    @SuppressWarnings("serial")
    static class IdPTypedList extends ArrayList<IdentityProviderRepresentation> {
    }
    
    @SuppressWarnings("serial")
    static class IdPMapperTypedList extends ArrayList<IdentityProviderMapperRepresentation> {
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
            HttpGet get = new HttpGet(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances").build());
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
                    return JsonSerialization.readValue(is, IdPTypedList.class);
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
            HttpPost post = new HttpPost(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances").build());
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

    public IdentityProviderRepresentation getIDP(AccessTokenResponse res, String alias) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            try {
                HttpGet get = new HttpGet(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias).build());
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

    public void updateIDP(AccessTokenResponse res, String alias, IdentityProviderRepresentation idp) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPut put = new HttpPut(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias).build());
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
    
    public void deleteIDP(AccessTokenResponse res, String alias) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpDelete delete = new HttpDelete(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias).build());
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
    
    public List<IdentityProviderMapperRepresentation> listIdpMappers(AccessTokenResponse res, String alias) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpGet get = new HttpGet(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias + "/mappers").build());
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
                    return JsonSerialization.readValue(is, IdPMapperTypedList.class);
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
    
    public void createIDPMapper(AccessTokenResponse res, String alias, IdentityProviderMapperRepresentation idp) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPost post = new HttpPost(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias + "/mappers").build());
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

    public IdentityProviderMapperRepresentation getIDPMapper(AccessTokenResponse res, String alias, String id) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            try {
                HttpGet get = new HttpGet(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias + "/mappers/" + id).build());
                get.addHeader("Authorization", "Bearer " + res.getToken());
                get.addHeader("Content-type", "application/json");
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, IdentityProviderMapperRepresentation.class);
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

    public void updateIDPMapper(AccessTokenResponse res, String alias, IdentityProviderMapperRepresentation idp) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPut put = new HttpPut(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias + "/mappers/" + idp.getId()).build());
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
    
    public void deleteIDPMapper(AccessTokenResponse res, String alias, String id) throws Failure, UnsupportedEncodingException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpDelete delete = new HttpDelete(UriBuilder.fromUri(baseUrl + "/admin/realms/" + realm + "/identity-provider/instances/" + alias + "/mappers/" + id).build());
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
    
    public boolean synchronize(Map<String, EntityDescriptor> entities) throws IOException {
        AccessTokenResponse res = getToken();
        boolean ok = true;
        try {
            List<IdentityProviderRepresentation> keycloakIdps = listIdps(res);
            List<String> keycloakAliases = new ArrayList<String>();
            IdentityProviderMapperRepresentation esrMapper = new IdentityProviderMapperRepresentation();
            esrMapper.setName("ESR role enforced for renater users");
            esrMapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
            Map<String, String> esrMapperConfig = new HashMap<String, String>();
            esrMapperConfig.put("role", "esr");
            esrMapper.setConfig(esrMapperConfig);
            IdentityProviderMapperRepresentation principalMapper = new IdentityProviderMapperRepresentation();
            principalMapper.setName("eduPersonPrincipalName as Username");
            principalMapper.setIdentityProviderMapper("saml-username-idp-mapper");
            Map<String, String> principalMapperConfig = new HashMap<String, String>();
            principalMapperConfig.put("template", "${ATTRIBUTE.eduPersonPrincipalName}");
            principalMapper.setConfig(principalMapperConfig);
            for (IdentityProviderRepresentation idp : keycloakIdps) {
                keycloakAliases.add(idp.getAlias());
                if (entities.containsKey(idp.getAlias())) {
                    EntityDescriptor entity = entities.get(idp.getAlias());
                    if ((entity.getSsoURL() != null && !entity.getSsoURL().equals(idp.getConfig().get("singleSignOnServiceUrl")))
                            || (entity.getCertificate() != null && !entity.getCertificate().equals(idp.getConfig().get("signingCertificate")))) {
                        LOGGER.log(Level.FINE, "idp need update in keycloak for alias: " + idp.getAlias());
                        try {
                            idp.getConfig().put("singleSignOnServiceUrl", entity.getSsoURL());
                            idp.getConfig().put("signingCertificate", entity.getCertificate());
                            updateIDP(res, idp.getAlias(), idp);
                        } catch (Failure e) {
                            LOGGER.log(Level.SEVERE, "error while updating keycloak idp with alias: " + idp.getAlias(), e);
                            ok = false;
                        }
                    }
                    try {
                        List<IdentityProviderMapperRepresentation> mappers = listIdpMappers(res, idp.getAlias());
                        if (mappers.size() == 0) {
                            LOGGER.log(Level.FINE, "creating mappers for idp with alias: " + idp.getAlias());
                            esrMapper.setIdentityProviderAlias(idp.getAlias());
                            principalMapper.setIdentityProviderAlias(idp.getAlias());
                            createIDPMapper(res, idp.getAlias(), esrMapper);
                            createIDPMapper(res, idp.getAlias(), principalMapper);
                        }
                    } catch (Failure e) {
                        LOGGER.log(Level.SEVERE, "error while updating keycloak idp mappers with alias: " + idp.getAlias(), e);
                        ok = false;
                    }

                } else {
                    LOGGER.log(Level.FINE, "idp to delete in keycloak for alias: " + idp.getAlias());
                    try {
                        deleteIDP(res, idp.getAlias());
                    } catch (Failure e) {
                        LOGGER.log(Level.SEVERE, "error while deleting keycloak idp with alias: " + idp.getAlias(), e);
                        ok = false;
                    }
                }
            }
            for (EntityDescriptor entity : entities.values()) {
                if (!keycloakAliases.contains(entity.getAlias())) {
                    LOGGER.log(Level.FINE, "idp to create in keycloak for alias: " + entity.getAlias());
                    try {
                        createIDP(res, entity.toIdentityProviderRepresentation());
                        esrMapper.setIdentityProviderAlias(entity.getAlias());
                        principalMapper.setIdentityProviderAlias(entity.getAlias());
                        createIDPMapper(res, entity.getAlias(), esrMapper);
                        createIDPMapper(res, entity.getAlias(), principalMapper);
                    } catch (Failure e) {
                        LOGGER.log(Level.SEVERE, "error while creating keycloak idp with alias: " + entity.getAlias(), e);
                        ok = false;
                    }
                }
            }
            logout(res);
            LOGGER.log(Level.FINE, "keycloak idps synchronized");
        } catch (Failure e) {
            LOGGER.log(Level.SEVERE, "error while loading keycloak idps", e);
            ok = false;
        }
        return ok;
    }

}