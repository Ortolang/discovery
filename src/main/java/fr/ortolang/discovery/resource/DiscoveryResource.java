package fr.ortolang.discovery.resource;

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
