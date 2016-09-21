package fr.ortolang.discovery;

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
        NAME_ID_POLICY_FORMAT ("name.id.policy.format");
        
        private final String key;

        private Property(String name) {
            this.key = name;
        }

        public String key() {
            return key;
        }

    }

}
