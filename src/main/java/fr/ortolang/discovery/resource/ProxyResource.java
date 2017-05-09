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

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.opensaml.Configuration;
import org.opensaml.saml2.binding.artifact.AbstractSAML2Artifact;

@Path("/proxy")
@RequestScoped
public class ProxyResource {
    
    private static final Logger LOGGER = Logger.getLogger(ProxyResource.class.getName());
    
    @GET
    @Path("/sso")
    public void authRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @QueryParam("SAMLRequest") String authnRequest, @QueryParam("RelayState") String relayState) {
        LOGGER.log(Level.INFO, "GET /proxy/sso");
        
        AbstractSAML2Artifact artefact = Configuration.getSAML2ArtifactBuilderFactory().buildArtifact(authnRequest);
        
        dumpRequest(request);
    }
    
    @POST
    @Path("/endpoint")
    public void forwardCallback(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        LOGGER.log(Level.INFO, "POST /proxy/endpoint");
        //TODO forward the authn response to keycloak
        dumpRequest(request);
    }
    
    private void dumpRequest(HttpServletRequest request) {
        LOGGER.log(Level.INFO, "REQUEST URI       =" + request.getRequestURI());
        LOGGER.log(Level.INFO, "          authType=" + request.getAuthType());
        LOGGER.log(Level.INFO, " characterEncoding=" + request.getCharacterEncoding());
        LOGGER.log(Level.INFO, "     contentLength=" + request.getContentLength());
        LOGGER.log(Level.INFO, "       contentType=" + request.getContentType());
        LOGGER.log(Level.INFO, "       contextPath=" + request.getContextPath());
        Cookie cookies[] = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++)
                LOGGER.log(Level.INFO, "            cookie=" + cookies[i].getName() + "=" + cookies[i].getValue());
        }
        Enumeration<String> hnames = request.getHeaderNames();
        while (hnames.hasMoreElements()) {
            String hname = (String) hnames.nextElement();
            Enumeration<String> hvalues = request.getHeaders(hname);
            while (hvalues.hasMoreElements()) {
                String hvalue = (String) hvalues.nextElement();
                LOGGER.log(Level.INFO, "            header=" + hname + "=" + hvalue);
            }
        }
        LOGGER.log(Level.INFO, "            locale=" + request.getLocale());
        LOGGER.log(Level.INFO, "            method=" + request.getMethod());
        Enumeration<String> pnames = request.getParameterNames();
        while (pnames.hasMoreElements()) {
            String pname = (String) pnames.nextElement();
            String pvalues[] = request.getParameterValues(pname);
            StringBuffer result = new StringBuffer(pname);
            result.append('=');
            for (int i = 0; i < pvalues.length; i++) {
                if (i > 0)
                    result.append(", ");
                result.append(pvalues[i]);
            }
            LOGGER.log(Level.INFO, "         parameter=" + result.toString());
        }
        LOGGER.log(Level.INFO, "          pathInfo=" + request.getPathInfo());
        LOGGER.log(Level.INFO, "          protocol=" + request.getProtocol());
        LOGGER.log(Level.INFO, "       queryString=" + request.getQueryString());
        LOGGER.log(Level.INFO, "        remoteAddr=" + request.getRemoteAddr());
        LOGGER.log(Level.INFO, "        remoteHost=" + request.getRemoteHost());
        LOGGER.log(Level.INFO, "        remoteUser=" + request.getRemoteUser());
        LOGGER.log(Level.INFO, "requestedSessionId=" + request.getRequestedSessionId());
        LOGGER.log(Level.INFO, "            scheme=" + request.getScheme());
        LOGGER.log(Level.INFO, "        serverName=" + request.getServerName());
        LOGGER.log(Level.INFO, "        serverPort=" + request.getServerPort());
        LOGGER.log(Level.INFO, "       servletPath=" + request.getServletPath());
        LOGGER.log(Level.INFO, "          isSecure=" + request.isSecure());
        LOGGER.log(Level.INFO, "---------------------------------------------------------------");
    }


}
