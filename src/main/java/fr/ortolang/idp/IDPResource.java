package fr.ortolang.idp;

import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/")
@RequestScoped
public class IDPResource {

    private static final Logger LOGGER = Logger.getLogger(IDPResource.class.getName());

    @EJB
    private IDPService service;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection<IDPRepresentation> listIDPs() {
        return service.listIDPs();
    }

    @POST
    public void updateIDPs() {
        service.updateIDPs();
    }

    @GET
    @Path("config")
    public String getConfig() {
        StringBuffer xml = new StringBuffer();
        xml.append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" ID=\"_666acb6d4439006afef16dfd1a338b77\" entityID=\"" + IDPConfig.getInstance().getProperty(IDPConfig.Property.HOSTNAME) + "/auth/realms/ortolang\">");
        xml.append("<md:SPSSODescriptor AuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">");
        int cpt = 0;
        for ( IDPRepresentation idp : service.listIDPs() ) {
            cpt++;
            xml.append("<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"" + IDPConfig.getInstance().getProperty(IDPConfig.Property.HOSTNAME) + "/auth/realms/ortolang/broker/").append(idp.getAlias()).append("/endpoint\" index=\"").append(cpt).append("\"/>");
        }
        xml.append("<md:KeyDescriptor use=\"signing\">");
        xml.append("<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">");
        xml.append("<ds:KeyName>" + IDPConfig.getInstance().getProperty(IDPConfig.Property.KEY_NAME) + "</ds:KeyName>");
        xml.append("<ds:X509Data>");
        xml.append("<ds:X509Certificate>" + IDPConfig.getInstance().getProperty(IDPConfig.Property.KEY_CERTIFICATE) + "</ds:X509Certificate>");
        xml.append("</ds:X509Data>");
        xml.append("</ds:KeyInfo>");
        xml.append("</md:KeyDescriptor>");
        xml.append("</md:SPSSODescriptor>");
        xml.append("</md:EntityDescriptor>");
        return xml.toString();
    }

    @GET
    @Path("/endpoint")
    public void getCallback(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        LOGGER.log(Level.INFO, "GET /idps/endpoint");
        dumpRequest(request);
    }

    @POST
    @Path("/endpoint")
    public void postCallback(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        LOGGER.log(Level.INFO, "POST /idps/endpoint");
        dumpRequest(request);
    }

    private void dumpRequest(HttpServletRequest request) {
        LOGGER.log(Level.FINE, "REQUEST URI       =" + request.getRequestURI());
        LOGGER.log(Level.FINE, "          authType=" + request.getAuthType());
        LOGGER.log(Level.FINE, " characterEncoding=" + request.getCharacterEncoding());
        LOGGER.log(Level.FINE, "     contentLength=" + request.getContentLength());
        LOGGER.log(Level.FINE, "       contentType=" + request.getContentType());
        LOGGER.log(Level.FINE, "       contextPath=" + request.getContextPath());
        Cookie cookies[] = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++)
                LOGGER.log(Level.FINE, "            cookie=" + cookies[i].getName() + "=" + cookies[i].getValue());
        }
        Enumeration<String> hnames = request.getHeaderNames();
        while (hnames.hasMoreElements()) {
            String hname = (String) hnames.nextElement();
            Enumeration<String> hvalues = request.getHeaders(hname);
            while (hvalues.hasMoreElements()) {
                String hvalue = (String) hvalues.nextElement();
                LOGGER.log(Level.FINE, "            header=" + hname + "=" + hvalue);
            }
        }
        LOGGER.log(Level.FINE, "            locale=" + request.getLocale());
        LOGGER.log(Level.FINE, "            method=" + request.getMethod());
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
            LOGGER.log(Level.FINE, "         parameter=" + result.toString());
        }
        LOGGER.log(Level.FINE, "          pathInfo=" + request.getPathInfo());
        LOGGER.log(Level.FINE, "          protocol=" + request.getProtocol());
        LOGGER.log(Level.FINE, "       queryString=" + request.getQueryString());
        LOGGER.log(Level.FINE, "        remoteAddr=" + request.getRemoteAddr());
        LOGGER.log(Level.FINE, "        remoteHost=" + request.getRemoteHost());
        LOGGER.log(Level.FINE, "        remoteUser=" + request.getRemoteUser());
        LOGGER.log(Level.FINE, "requestedSessionId=" + request.getRequestedSessionId());
        LOGGER.log(Level.FINE, "            scheme=" + request.getScheme());
        LOGGER.log(Level.FINE, "        serverName=" + request.getServerName());
        LOGGER.log(Level.FINE, "        serverPort=" + request.getServerPort());
        LOGGER.log(Level.FINE, "       servletPath=" + request.getServletPath());
        LOGGER.log(Level.FINE, "          isSecure=" + request.isSecure());
        LOGGER.log(Level.FINE, "---------------------------------------------------------------");
    }

}
