package fr.ortolang.discovery.service;

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
import fr.ortolang.discovery.keycloak.KeycloakSynchronizer;

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
        boolean changed = false;
        try {
            String idpsurl = DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.WAYF_URL);
            URL url = new URL(idpsurl);
            try (InputStream input = url.openStream()) {
                LOGGER.log(Level.FINE, "loading entities from wayf URL: " + url);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                EntityDescriptorsHandler handler = new EntityDescriptorsHandler();
                saxParser.parse(input, handler);
                LOGGER.log(Level.FINE, "entities loaded and parsed, calculating diff");
                for (EntityDescriptor entity : handler.getIdps().values()) {
                    if (entities.containsKey(entity.getAlias())) {
                        if (!entities.get(entity.getAlias()).equals(entity)) {
                            changed = true;
                            break;
                        }
                    } else {
                        changed = true;
                        break;
                    }
                }
                this.entities = handler.getIdps();
                if (changed) {
                    LOGGER.log(Level.INFO, "changes detected with previous update, synchronizing keycloak idps...");
                    KeycloakSynchronizer keycloak = new KeycloakSynchronizer(DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.KEYCLOAK_USER), DiscoveryConfig.getInstance()
                            .getProperty(DiscoveryConfig.Property.KEYCLOAK_PASS), DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.KEYCLOAK_REALM), DiscoveryConfig.getInstance()
                            .getProperty(DiscoveryConfig.Property.KEYCLOAK_CLIENT), DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.KEYCLOAK_URL));
                    ok = keycloak.synchronize(entities);
                } else {
                    LOGGER.log(Level.FINE, "no changes detected with previous update, nothing to do.");
                }
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
