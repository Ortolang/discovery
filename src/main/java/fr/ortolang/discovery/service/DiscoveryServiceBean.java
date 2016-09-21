package fr.ortolang.discovery.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import fr.ortolang.discovery.DiscoveryConfig;
import fr.ortolang.discovery.entity.EntityDescriptor;
import fr.ortolang.discovery.entity.EntityDescriptorsHandler;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DiscoveryServiceBean implements DiscoveryService {

    private static final Logger LOGGER = Logger.getLogger(DiscoveryServiceBean.class.getName());

    private Map<String, EntityDescriptor> entities;

    public DiscoveryServiceBean() {
        entities = Collections.emptyMap();
    }
    
    @PostConstruct
    public void setup() {
         this.update();   
    }

    @Override
    public Collection<EntityDescriptor> listAll() {
        return entities.values();
    }

    @Override
    @Schedule(hour = "*", persistent = false)
    public void update() {
        LOGGER.log(Level.INFO, "Updating entities");
        boolean ok = true;
        try {
            String idpsurl = DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.WAYF_URL);
            URL url = new URL(idpsurl);
            try (InputStream input = url.openStream()) {
                LOGGER.log(Level.FINE, "loading entities from wayf URL: " + url);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                EntityDescriptorsHandler handler = new EntityDescriptorsHandler();
                saxParser.parse(input, handler);
                LOGGER.log(Level.FINE, "entities loaded and parsed.");
                this.entities = handler.getIdps();
            } catch (SAXException | ParserConfigurationException | IOException e) {
                LOGGER.log(Level.SEVERE, "unable to update local entities", e);
                ok = false;
            }
        } catch (MalformedURLException e1) {
            LOGGER.log(Level.SEVERE, "unable to update local entities", e1);
            ok = false;
        }
        if (!ok) {
            // TODO send a mail to admin
            LOGGER.log(Level.SEVERE, "entities NOT updated correctly, check previous logs");
        } else {
            LOGGER.log(Level.INFO, "entities updated");
        }
    }

}
