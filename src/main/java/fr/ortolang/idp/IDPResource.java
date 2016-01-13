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
        xml.append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" ID=\"_666acb6d4439006afef16dfd1a338b77\" entityID=\"https://demo-auth.ortolang.fr/auth/realms/ortolang\">");
        xml.append("<md:SPSSODescriptor AuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">");
        int cpt = 0;
        for ( IDPRepresentation idp : service.listIDPs() ) {
            cpt++;
            xml.append("<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://demo-auth.ortolang.fr/auth/realms/ortolang/broker/").append(idp.getAlias()).append("/endpoint\" index=\"").append(cpt).append("\"/>");
        }
        xml.append("<md:KeyDescriptor use=\"signing\">");
        xml.append("<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">");
        xml.append("<ds:KeyName>auth.ortolang.fr</ds:KeyName>");
        xml.append("<ds:X509Data>");
        xml.append("<ds:X509Certificate>MIICnzCCAYcCBgFRxPn3xzANBgkqhkiG9w0BAQsFADATMREwDwYDVQQDDAhvcnRvbGFuZzAeFw0xNTEyMjExNDM2MzRaFw0yNTEyMjExNDM4MTRaMBMxETAPBgNVBAMMCG9ydG9sYW5nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj3hDFa3b77uDgalDKwLrvAlVSUxLwwL5pgl2d/0UDiaiRUjtqdrbhCQNEPA+ijDh2LzBDdC7FhabY3/gIj5hqI6zkWVWQoG98ONdn52jKCpQO5qjK1wcFnUqz4bfESxbc5vESfQ+MPzkqLC7gm8p5QrjJQgxXbyEEQxBCI0jWb+tUHsQAis3IzoNAsuXr5a9qm9wF+mP/VzB3WGCbJOP+7WwwngN3iN4Ykq96nqR1A/3c/CrtygeVtalYxEFzK9g1qk+q6/q5vyF1PSaThmbW4kgXktvXICDgFEr2/tC+JXQt6pH65tqjc2Inqw/N6QwOdjkmF8BHjFPyxudRMRPZwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQABq2zafrSRBzyTpPEoYwPPxzobYfIavIoLbKhpJq+7dHWJMvU2PhJlmE3HnBi05wr2Xhf3LNQJ1N2Si3wHUAGK4tSxw+wXQOLvCuo/5mjRG0CTzyj0rrWZR0/5Ogn0+f1wheUOtC6U0X2woWfdsbgronX3CqGh2B73m5SbyD4Ul8EoxMlpnNmh9hd54HH2YNvKS2JrMhPaHSibVJyHuQND3CxlhpobozWzcng4J4QvMGxgrEBUDdYg8jY86lS4JZUUiuVoVTlV0yKSkbsUSgb/ZeBP+bUARUAnrAHptCxQRW0FVHxkCFj7wmPdQsIe2eNO8lvEQbABKsvG8xGQTboO</ds:X509Certificate>");
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
