package com.sabari.user;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new CustomUserStorageProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {
        // nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public java.lang.String getId() {
        return "custom-event-listener";
    }
}