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
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<md:EntitiesDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\" cacheDuration=\"PT1H\">");
        xml.append("<md:EntityDescriptor entityID=\"https://auth-int.ortolang.fr:8443/auth/realms/ortolang\">");
        xml.append("<md:SPSSODescriptor AuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">");
        int cpt = 0;
        for ( IDPRepresentation idp : service.listIDPs() ) {
            cpt++;
            xml.append("<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://auth-int.ortolang.fr:8443/auth/realms/ortolang/broker/").append(idp.getAlias()).append("/endpoint\" index=\"").append(cpt).append("\"/>");
        }
        xml.append("<md:KeyDescriptor use=\"signing\">");
        xml.append("<dsig:KeyInfo>");
        xml.append("<dsig:X509Data>");
        xml.append("<dsig:X509Certificate>MIICnzCCAYcCBgFO36Y8szANBgkqhkiG9w0BAQsFADATMREwDwYDVQQDDAhvcnRvbGFuZzAeFw0xNTA3MzAxNTQ2MzBaFw0yNTA3MzAxNTQ4MTBaMBMxETAPBgNVBAMMCG9ydG9sYW5nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg8Wj2XtSr/l8OREO2Ay/B6OLeqIBSFSjL92P1MANWLijpjHFHi99dS4j732yXqQbYQED4R2c2FNMV8Tkhrm+34/P0JGxJWCO9OYLprQ2u5A+0z3UB+ITM1EvymDYA5sGX8VtXj6yiniAQwGOwUefFNIS1BRoPr3BJ5av+ehO1lLgpbPRtkGISehtxoYFjM9UEvzk8YObeuz1EdaiOuZJnwwAGxqJ/c54Wii2IjWb2dtXWtvbsUJLxSjFKbHrpk45tT/JiWfcXc0P7EUEnRs8WmfgFL2M0fJGF8y2o++gHeHmRycpsr7bvlVtkE23Fp1k6aJSEAhBffmwz4gY9XX4FQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQA9NpF9N2I08YIuAvvT81Pih2Wg4qsFdTSH3E/ulYo/R8Du2eW/OnIuis6XqBz6MG5wI12TNJfxuBMEXlXZY1e69WblrqDbyGYnN5fm83lORd+xi+p5dYBad8KoxfgnIinRaxYNyuZPxwYpkntT0eHEKvt5jtXzDCXev9Asfnl0yxqTehTHovV2wNgp3xCQoD6N4aRXRwTpsfc/CQ7Mauyv0fci5T+ax4Ut5IFsNSgTWs64yYVIIrv+erztWkpGAixr7k5XIrQAJYMjTZvgmMogueKwqH0v2Lv0jMn+F5qjrBH8L1E0nY0KuEtSOhTq4TNn1bzZ2taNNv9daZuscX0h</dsig:X509Certificate>");
        xml.append("</dsig:X509Data>");
        xml.append("</dsig:KeyInfo>");
        xml.append("</md:KeyDescriptor>");
        xml.append("</md:SPSSODescriptor>");
        xml.append("</md:EntityDescriptor>");
        xml.append("</md:EntitiesDescriptor>");
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
        Enumeration hnames = request.getHeaderNames();
        while (hnames.hasMoreElements()) {
            String hname = (String) hnames.nextElement();
            Enumeration hvalues = request.getHeaders(hname);
            while (hvalues.hasMoreElements()) {
                String hvalue = (String) hvalues.nextElement();
                LOGGER.log(Level.INFO, "            header=" + hname + "=" + hvalue);
            }
        }
        LOGGER.log(Level.INFO, "            locale=" + request.getLocale());
        LOGGER.log(Level.INFO, "            method=" + request.getMethod());
        Enumeration pnames = request.getParameterNames();
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
