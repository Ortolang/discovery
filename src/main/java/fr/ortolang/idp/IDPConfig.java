package fr.ortolang.idp;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IDPConfig {
    
    private static final Logger LOGGER = Logger.getLogger(IDPConfig.class.getName());
    private static IDPConfig config;
    private Properties props;
    private Path home;
    
    private IDPConfig() throws Exception {
        if ( System.getenv("ORTOLANG_HOME") != null ) {
            home = Paths.get(System.getenv("ORTOLANG_HOME"));
        } else {
            home = Paths.get(System.getProperty("user.home"), ".ortolang");
        }
        if ( !Files.exists(home) ) {
            Files.createDirectories(home);
        }
        LOGGER.log(Level.INFO, "IDPS_HOME set to : " + home);

        props = new Properties();
        Path configFilePath = Paths.get(home.toString(), "idps.properties");
        if ( !Files.exists(configFilePath) ) {
            Files.copy(IDPConfig.class.getClassLoader().getResourceAsStream("idps.properties.sample"), configFilePath);
        }
        try (InputStream in = Files.newInputStream(configFilePath) ) {
            props.load(in);
        }
    }

    public static synchronized IDPConfig getInstance() {
        if (config == null) {
            try {
                config = new IDPConfig();
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

    public String getProperty(IDPConfig.Property property) {
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
        IDPS_URL ("idps.url"),
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
