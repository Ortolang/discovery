package fr.ortolang.idp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

@Singleton
@Startup
public class IDPServiceBean implements IDPService {
    
    private static final Logger LOGGER = Logger.getLogger(IDPServiceBean.class.getName());
    private static final String renaterIdps = "http://federation.renater.fr/renater/idps-renater-metadata.xml";
    
    private Collection<IDPRepresentation> idps;
    
    public IDPServiceBean() {
        updateIDPs();
    }

    @Override
    public Collection<IDPRepresentation> listIDPs() {
        return idps;
    }
    
    private void updateIDPs() {
        LOGGER.log(Level.INFO, "Starting IDPs update");
        try {
            URL renater = new URL(renaterIdps);
            try (InputStream input = renater.openStream()) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                IDPHandler handler = new IDPHandler();
                saxParser.parse(input, handler);
                this.idps = handler.getIdps();
            } catch ( SAXException | ParserConfigurationException | IOException e ) {
                LOGGER.log(Level.SEVERE, "unable to update local IDPs", e);
            }
        } catch (MalformedURLException e1) {
            LOGGER.log(Level.SEVERE, "unable to update local IDPs", e1);
        }
        LOGGER.log(Level.INFO, "IDPs updated");
    }

}

    