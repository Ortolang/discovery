package fr.ortolang.discovery.resource;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fr.ortolang.discovery.DiscoveryConfig;
import fr.ortolang.discovery.entity.EntityDescriptor;
import fr.ortolang.discovery.service.DiscoveryService;

@Path("/")
@RequestScoped
public class DiscoveryResource {

    private static final Logger LOGGER = Logger.getLogger(DiscoveryResource.class.getName());

    @EJB
    private DiscoveryService service;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection<EntityDescriptor> listIDPs() {
        LOGGER.log(Level.INFO, "GET /");
        return service.listAll();
    }

    @POST
    public void update() {
        LOGGER.log(Level.INFO, "POST /");
        service.update();
    }
    
    @GET
    @Path("/config")
    public String config() {
        LOGGER.log(Level.INFO, "GET /config");
        StringBuffer xml = new StringBuffer();
        xml.append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" ID=\"_666acb6d4439006afef16dfd1a338b77\" entityID=\"" + DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.HOSTNAME) + "/auth/realms/ortolang\">");
        xml.append("<md:SPSSODescriptor AuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">");
        if ( DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.MODE).equals("keycloak-sync") ) {
            int cpt = 0;
            for ( EntityDescriptor descriptor : service.listAll() ) {
                cpt++;
                xml.append("<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"").append(DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.HOSTNAME)).append("/auth/realms/ortolang/broker/").append(descriptor.getAlias()).append("/endpoint\" index=\"").append(cpt).append("\"/>");
            }
        } else {
            xml.append("<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"").append(DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.HOSTNAME)).append("/idps/proxy/endpoint\" index=\"1\"/>");
        }
        xml.append("<md:KeyDescriptor use=\"signing\">");
        xml.append("<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">");
        xml.append("<ds:KeyName>" + DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.KEY_NAME) + "</ds:KeyName>");
        xml.append("<ds:X509Data>");
        xml.append("<ds:X509Certificate>" + DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.KEY_CERTIFICATE) + "</ds:X509Certificate>");
        xml.append("</ds:X509Data>");
        xml.append("</ds:KeyInfo>");
        xml.append("</md:KeyDescriptor>");
        xml.append("</md:SPSSODescriptor>");
        xml.append("</md:EntityDescriptor>");
        return xml.toString();
    }

}
