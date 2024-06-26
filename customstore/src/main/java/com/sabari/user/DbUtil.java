package com.sabari.user;


import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.AuthTokens;
import org.keycloak.component.ComponentModel;
import static com.sabari.user.CustomUserStorageProviderConstants.*;

public class DbUtil {
    public static Session getSession(ComponentModel config) {
        String driverClass = config.get(CONFIG_KEY_NEO4J_DRIVER);
        String endpoint = config.get(CONFIG_KEY_NEO4J_CONNECTION_URL);
        String user = config.get(CONFIG_KEY_DB_USERNAME);    
        String password = config.get(CONFIG_KEY_DB_PASSWORD);
        var driver = GraphDatabase.driver(driverClass + "://" + endpoint, AuthTokens.basic(user, password));
        return driver.session();
    }
}
