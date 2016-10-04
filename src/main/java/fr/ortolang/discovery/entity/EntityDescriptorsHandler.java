package fr.ortolang.discovery.entity;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EntityDescriptorsHandler extends DefaultHandler {

    private static final Logger LOGGER = Logger.getLogger(EntityDescriptorsHandler.class.getName());

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private Map<String, EntityDescriptor> idps;
    private EntityDescriptor current;
    private boolean read = false;
    private StringBuffer buffer = new StringBuffer();

    public EntityDescriptorsHandler() {
        idps = new HashMap<String, EntityDescriptor>();
    }

    public Map<String, EntityDescriptor> getIdps() {
        return idps;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals("md:EntityDescriptor") || qName.equals("EntityDescriptor")) {
            String entityId = atts.getValue("entityID");
            if (entityId != null) {
                LOGGER.log(Level.FINEST, "Found new entity descriptor with entity ID: " + entityId);
                current = new EntityDescriptor();
                current.setEntityId(entityId);
            }
        }
        if (current != null && qName.equals("mdrpi:RegistrationInfo")) {
            String registration = atts.getValue("registrationInstant");
            if (registration != null) {
                try {
                    current.setRegistrationDate(sdf.parse(registration));
                } catch (ParseException e) {
                    LOGGER.log(Level.SEVERE, "unable to parse registration date", e);
                }
            }
        }
        if (current != null && qName.equals("mdui:DisplayName")) {
            if (atts.getValue("xml:lang") != null && atts.getValue("xml:lang").equals("fr")) {
                read = true;
            }
        }
        if (current != null && qName.equals("mdui:Description")) {
            if (atts.getValue("xml:lang") != null && atts.getValue("xml:lang").equals("fr")) {
                read = true;
            }
        }
        if (current != null && qName.equals("mdui:Logo")) {
            read = true;
        }
        if (current != null && qName.equals("ds:X509Certificate") && current.getCertificate() == null) {
            read = true;
        }
        if (current != null
                && (qName.equals("md:SingleSignOnService") || qName.equals("SingleSignOnService") || qName.equals("md:AssertionConsumerService") || qName.equals("AssertionConsumerService"))) {
            String binding = atts.getValue("Binding");
            String location = atts.getValue("Location");
            if (binding != null && binding.equals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")) {
                try {
                    new URL(location);
                    current.setSsoURL(location);
                } catch ( MalformedURLException e ) {
                    LOGGER.log(Level.INFO, "unable to parse Single Sign On Service Location: " + location, e);
                }
                
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("md:EntityDescriptor") || qName.equals("EntityDescriptor")) {
            LOGGER.log(Level.FINEST, "EntityDescriptor added to list : \r\n" + current.toString());
            if (current.getName() != null && current.getName().length() > 0 && current.getSsoURL() != null && current.getSsoURL().length() > 0 && current.getAlias() != null
                    && current.getAlias().length() > 0) {
                idps.put(current.getAlias(), current);
            }
            current = null;
        }
        if (read) {
            if (qName.equals("mdui:DisplayName")) {
                current.setName(buffer.toString());
            }
            if (qName.equals("mdui:Description")) {
                current.setDescription(buffer.toString());
            }
            if (qName.equals("mdui:Logo")) {
                current.setLogo(buffer.toString());
            }
            if (qName.equals("ds:X509Certificate")) {
                current.setCertificate(buffer.toString());
            }
            read = false;
            buffer = new StringBuffer();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (read) {
            buffer.append(ch, start, length);
        }
    }

}