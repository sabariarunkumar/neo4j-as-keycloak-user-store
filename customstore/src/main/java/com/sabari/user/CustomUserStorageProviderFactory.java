package com.sabari.user;

import org.neo4j.driver.Session;
import java.util.List;

import org.neo4j.driver.Query;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sabari.user.CustomUserStorageProviderConstants.*;

public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {
    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProviderFactory.class);    
    protected final List<ProviderConfigProperty> configMetadata;
    
    public CustomUserStorageProviderFactory() {
        
        
        // Create config metadata
        configMetadata = ProviderConfigurationBuilder.create()
          .property()
            .name(CONFIG_KEY_NEO4J_DRIVER)
            .label("Neo4j Driver Class")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue("neo4j")
            .helpText("Neo4j Driver Class being used for connections")
            .add()
          .property()
            .name(CONFIG_KEY_NEO4J_CONNECTION_URL)
            .label("Neo4j connection URL")
            .type(ProviderConfigProperty.STRING_TYPE)
            .helpText("Endpoint connection URL; e.g. localhost:7687")
            .add()
          .property()
            .name(CONFIG_KEY_DB_USERNAME)
            .label("Database User")
            .type(ProviderConfigProperty.STRING_TYPE)
            .helpText("Username used to connect to the database")
            .add()
          .property()
            .name(CONFIG_KEY_DB_PASSWORD)
            .label("Database Password")
            .type(ProviderConfigProperty.STRING_TYPE)
            .helpText("Password used to connect to the database")
            .secret(true)
            .add()
          .property()
            .name(CONFIG_KEY_VALIDATION_QUERY)
            .label("Validation Query")
            .type(ProviderConfigProperty.STRING_TYPE)
            .helpText("Query used to validate a connection")
            .defaultValue("MATCH (n) RETURN n LIMIT 1;")
            .add()
          .build();   
          
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        return new CustomUserStorageProvider(ksession,model);
    }

    @Override
    public String getId() {
        return CustomUserStorageProviderConstants.NEO4J_PROVIDER_ID;
    }

    
    // Configuration support methods
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        
       try (Session c = DbUtil.getSession(config)) {
          log.info("Testing connection..." );
          var txReturn = c.executeWrite(tx -> {
            var query = new Query(config.get(CONFIG_KEY_VALIDATION_QUERY));
            var result = tx.run(query).list();
            return result;
          });
          log.info("Connection OK !" );
       }
       catch(Exception ex) {
           log.warn("[W94] Unable to validate connection: ex={}", ex.getMessage());
           throw new ComponentValidationException("Unable to validate database connection",ex);
       }
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
    }
}
