package fr.ortolang.idp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.xml.sax.SAXException;

import fr.ortolang.idp.KeycloakAdminClient.Failure;

@Singleton
@Startup
public class IDPServiceBean implements IDPService {

    private static final Logger LOGGER = Logger.getLogger(IDPServiceBean.class.getName());
    
    private Map<String, IDPRepresentation> idps;

    public IDPServiceBean() {
        idps = Collections.emptyMap();
    }

    @Override
    public Collection<IDPRepresentation> listIDPs() {
        return idps.values();
    }

    @Override
    @Schedule(hour = "*", persistent = false)
    public void updateIDPs() {
        LOGGER.log(Level.INFO, "Starting IDPs update");
        boolean ok = true;
        boolean changed = false;
        try {
            String idpsurl = IDPConfig.getInstance().getProperty(IDPConfig.Property.IDPS_URL);
            URL url = new URL(idpsurl);
            try (InputStream input = url.openStream()) {
                LOGGER.log(Level.FINE, "loading idps from wayf URL: " + url);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                IDPHandler handler = new IDPHandler();
                saxParser.parse(input, handler);
                LOGGER.log(Level.FINE, "idps loaded and parsed, calculating diff");
                for (IDPRepresentation idp : handler.getIdps().values()) {
                    if (idps.containsKey(idp.getAlias())) {
                        if (!idps.get(idp.getAlias()).equals(idp)) {
                            changed = true;
                        }
                    } else {
                        changed = true;
                    }
                }
                this.idps = handler.getIdps();
                if (changed) {
                    LOGGER.log(Level.INFO, "changes detected with previous update, synchronizing keycloak idps...");
                    KeycloakAdminClient keycloak = new KeycloakAdminClient(IDPConfig.getInstance().getProperty(IDPConfig.Property.KEYCLOAK_USER), 
                            IDPConfig.getInstance().getProperty(IDPConfig.Property.KEYCLOAK_PASS), 
                            IDPConfig.getInstance().getProperty(IDPConfig.Property.KEYCLOAK_REALM), 
                            IDPConfig.getInstance().getProperty(IDPConfig.Property.KEYCLOAK_CLIENT), 
                            IDPConfig.getInstance().getProperty(IDPConfig.Property.KEYCLOAK_URL));
                    AccessTokenResponse res = keycloak.getToken();
                    try {
                        List<IdentityProviderRepresentation> keycloakIdps = keycloak.listIdps(res);
                        List<String> keycloakAliases = new ArrayList<String>();
                        for (IdentityProviderRepresentation idp : keycloakIdps) {
                            keycloakAliases.add(idp.getAlias());
                            if (idps.containsKey(idp.getAlias())) {
                                IDPRepresentation idpr = idps.get(idp.getAlias());
                                if ((idpr.getSsoURL() != null && !idpr.getSsoURL().equals(idp.getConfig().get("singleSignOnServiceUrl")))
                                        || (idpr.getCertificate() != null && !idpr.getCertificate().equals(idp.getConfig().get("signingCertificate")))) {
                                    LOGGER.log(Level.FINE, "idp need update in keycloak for alias: " + idp.getAlias());
                                    try {
                                        idp.getConfig().put("singleSignOnServiceUrl", idpr.getSsoURL());
                                        idp.getConfig().put("signingCertificate", idpr.getCertificate());
                                        keycloak.updateIDP(res, idp.getAlias(), idp);
                                    } catch (Failure e) {
                                        LOGGER.log(Level.SEVERE, "error while updating keycloak idp with alias: " + idp.getAlias());
                                        ok = false;
                                    }
                                }
                            } else {
                                LOGGER.log(Level.FINE, "idp to delete in keycloak for alias: " + idp.getAlias());
                                try {
                                    keycloak.deleteIDP(res, idp.getAlias());
                                } catch (Failure e) {
                                    LOGGER.log(Level.SEVERE, "error while deleting keycloak idp with alias: " + idp.getAlias());
                                    ok = false;
                                } 
                            }
                        }
                        for (IDPRepresentation idp : idps.values()) {
                            if (!keycloakAliases.contains(idp.getAlias())) {
                                LOGGER.log(Level.FINE, "idp to create in keycloak for alias: " + idp.getAlias());
                                try {
                                    keycloak.createIDP(res, idp.toIdentityProviderRepresentation());
                                    IdentityProviderMapperRepresentation esrMapper = new IdentityProviderMapperRepresentation();
                                    esrMapper.setName("ESR role enforced for renater users");
                                    esrMapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
                                    Map<String, String> esrMapperConfig = new HashMap<String, String>();
                                    esrMapperConfig.put("role", "esr");
                                    esrMapper.setConfig(esrMapperConfig);
                                    keycloak.createIDPMapper(res, idp.getAlias(), esrMapper);
                                    IdentityProviderMapperRepresentation principalMapper = new IdentityProviderMapperRepresentation();
                                    principalMapper.setName("eduPersonPrincipalName as Username");
                                    principalMapper.setIdentityProviderMapper("saml-username-idp-mapper");
                                    Map<String, String> principalMapperConfig = new HashMap<String, String>();
                                    principalMapperConfig.put("template", "${ATTRIBUTE.eduPersonPrincipalName}");
                                    principalMapper.setConfig(principalMapperConfig);
                                    keycloak.createIDPMapper(res, idp.getAlias(), principalMapper);
                                } catch (Failure e) {
                                    LOGGER.log(Level.SEVERE, "error while creating keycloak idp with alias: " + idp.getAlias());
                                    ok = false;
                                }
                            }
                        }
                        keycloak.logout(res);
                        LOGGER.log(Level.FINE, "keycloak idps synchronized");
                    } catch (Failure e) {
                        LOGGER.log(Level.SEVERE, "error while loading keycloak idps");
                        ok = false;
                    }
                } else {
                    LOGGER.log(Level.FINE, "no changes detected with previous update, nothing to do.");
                }
            } catch (SAXException | ParserConfigurationException | IOException e) {
                LOGGER.log(Level.SEVERE, "unable to update local IDPs", e);
                ok = false;
            }
        } catch (MalformedURLException e1) {
            LOGGER.log(Level.SEVERE, "unable to update local IDPs", e1);
            ok = false;
        }
        if (!ok) {
            // TODO send a mail to admin
            LOGGER.log(Level.SEVERE, "IDPs NOT updated correctly, check previous logs");
        } else {
            LOGGER.log(Level.INFO, "IDPs updated");
        }
    }

}
