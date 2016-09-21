package fr.ortolang.discovery.resource;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import fr.ortolang.discovery.DiscoveryConfig;

@Path("/config")
@RequestScoped
public class ConfigResource {
    
    private static final Logger LOGGER = Logger.getLogger(ConfigResource.class.getName());
    
    @GET
    public String config() {
        LOGGER.log(Level.INFO, "GET /config");
        StringBuffer xml = new StringBuffer();
        xml.append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" ID=\"_666acb6d4439006afef16dfd1a338b77\" entityID=\"" + DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.HOSTNAME) + "/idps\">");
        xml.append("<md:SPSSODescriptor AuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">");
        xml.append("<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"").append(DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.HOSTNAME)).append("/idps/proxy/endpoint\" index=\"1\"/>");
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
