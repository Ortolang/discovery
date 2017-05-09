package fr.ortolang.discovery;

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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opensaml.DefaultBootstrap;

public class DiscoveryConfig {
    
    private static final Logger LOGGER = Logger.getLogger(DiscoveryConfig.class.getName());
    private static DiscoveryConfig config;
    private Properties props;
    private Path home;
    
    private DiscoveryConfig() throws Exception {
        if ( System.getenv("ORTOLANG_HOME") != null ) {
            home = Paths.get(System.getenv("ORTOLANG_HOME"));
        } else {
            home = Paths.get(System.getProperty("user.home"), ".ortolang");
        }
        if ( !Files.exists(home) ) {
            Files.createDirectories(home);
        }
        LOGGER.log(Level.INFO, "DISCOVERY_HOME set to : " + home);

        props = new Properties();
        Path configFilePath = Paths.get(home.toString(), "discovery.properties");
        if ( !Files.exists(configFilePath) ) {
            Files.copy(DiscoveryConfig.class.getClassLoader().getResourceAsStream("discovery.properties.sample"), configFilePath);
        }
        try (InputStream in = Files.newInputStream(configFilePath) ) {
            props.load(in);
        }
        
        DefaultBootstrap.bootstrap();
    }

    public static synchronized DiscoveryConfig getInstance() {
        if (config == null) {
            try {
                config = new DiscoveryConfig();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "unable to load configuration", e);
                throw new RuntimeException("unable to load configuration", e);
            }
        }
        return config;
    }

    public Path getHomePath() {
        return home;
    }

    public String getProperty(DiscoveryConfig.Property property) {
        return props.getProperty(property.key());
    }

    public enum Property {

        HOSTNAME ("hostname"),
        KEY_NAME ("key.name"),
        KEY_CERTIFICATE ("key.certificate"),
        KEYCLOAK_USER ("keycloak.username"),
        KEYCLOAK_PASS ("keycloak.password"),
        KEYCLOAK_REALM ("keycloak.realm"),
        KEYCLOAK_CLIENT ("keycloak.client.name"),
        KEYCLOAK_URL ("keycloak.server.url"),
        WAYF_URL ("wayf.url"),
        NAME_ID_POLICY_FORMAT ("name.id.policy.format"),
        MODE ("mode");
        
        private final String key;

        private Property(String name) {
            this.key = name;
        }

        public String key() {
            return key;
        }

    }

}
