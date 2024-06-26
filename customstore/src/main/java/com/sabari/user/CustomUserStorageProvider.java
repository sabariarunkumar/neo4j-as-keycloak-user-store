/**
 * 
 */
package com.sabari.user;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.List;

import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.MapAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.keycloak.events.Event;

import static org.neo4j.driver.Values.parameters;

import java.util.ArrayList;
import java.util.Date;

public class CustomUserStorageProvider implements UserStorageProvider, 
  UserLookupProvider, 
  CredentialInputValidator,
  UserRegistrationProvider,
  UserQueryProvider,
  EventListenerProvider  {
    
    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProvider.class);
    private KeycloakSession ksession;    
    private KeycloakSession epksession;
    private ComponentModel model;
    private RealmProvider realm;

    public CustomUserStorageProvider(KeycloakSession ksession) {
        this.epksession = ksession;
        this.realm = epksession.realms();
    }
    public CustomUserStorageProvider(KeycloakSession ksession, ComponentModel model) {
        this.ksession = ksession;
        this.model = model;
    }

    @Override
    public void onEvent(Event event) {
       
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // nothing to do
        this.model = null;
        OperationType opsType =  event.getOperationType();
        if ((opsType.name() == CustomUserStorageProviderConstants.KC_EVENT_CREATE ) && (event.getResourcePath().contains(CustomUserStorageProviderConstants.KC_RESOURCE_PATH_USER_SUFFIX))){
            String userID = event.getResourcePath().replaceFirst(CustomUserStorageProviderConstants.KC_RESOURCE_PATH_USER_SUFFIX, CustomUserStorageProviderConstants.EMPTY_STRING);
            RealmModel realm = this.realm.getRealm(event.getRealmId());
            var componentStream = realm.getComponentsStream();
             for (var component : componentStream.toList()) {
                if (component.getProviderId().equals(CustomUserStorageProviderConstants.NEO4J_PROVIDER_ID)) {
                    this.model = component;
                }
            }

            UserModel newRegisteredLocalUser = this.epksession.users().getUserById(realm,  userID);
            if (newRegisteredLocalUser!= null) {
                if (getUserByUsername(realm,newRegisteredLocalUser.getUsername()) == null) {
                    log.info("User {} will be registered with neo4j", newRegisteredLocalUser.getUsername());
                    try (Session s = DbUtil.getSession(this.model)) {
                        var txReturn = s.executeWrite(tx -> {
                            var query = new Query(CustomUserStorageProviderConstants.QUERY_CREATE_USER , parameters(
                                CustomUserStorageProviderConstants.DB_KEY_USER_FIRST_NAME, newRegisteredLocalUser.getFirstName(),
                                CustomUserStorageProviderConstants.DB_KEY_USER_LAST_NAME, newRegisteredLocalUser.getLastName(),
                                CustomUserStorageProviderConstants.DB_KEY_USER_EMAIL, newRegisteredLocalUser.getEmail(),
                                CustomUserStorageProviderConstants.DB_KEY_USER_NAME, newRegisteredLocalUser.getUsername()));
                            tx.run(query).list();
                            return null;
                        });
                    }
                    catch(Exception ex) {
                        log.warn("Database error: error in  create user {} in neo4j;  ex={}", newRegisteredLocalUser.getUsername(), ex.getMessage());
                    }
                } else {
                    log.info("User {} is already registered with neo4j", newRegisteredLocalUser.getUsername());
                }

            } else {
                log.info("User {} not found in keycloak database");
            }
        }
        
    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId sid = new StorageId(id);
        return getUserByUsername(realm, sid.getExternalId());
    }

    // protected void importUserToKeycloak(RealmModel realm, UserModel neo4jUser) {

    //     log.debug("Creating user %s with username: {}, email: {} to local Keycloak storage", neo4jUser.getUsername(),  neo4jUser.getEmail());
    //     // kcUser => keyCloakUser
    //     UserModel kcUser = UserStoragePrivateUtil.userLocalStorage(ksession).addUser(realm, neo4jUser.getUsername());
    //     kcUser.setEnabled(true);
    //     kcUser.setEmail(neo4jUser.getEmail());
    //     kcUser.setFederationLink(model.getId());
    //     kcUser.setFirstName(neo4jUser.getFirstName());        
    //     kcUser.setLastName(neo4jUser.getLastName());
    //     kcUser.setSingleAttribute(CustomUserStorageProviderConstants.ATTRIBUTE_USER_SOURCE_KEY, CustomUserStorageProviderConstants.ATTRIBUTE_USER_SOURCE_VALUE);
    // }


    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        UserModel neo4jUser;
        try (Session c = DbUtil.getSession(this.model)) {
            neo4jUser = c.executeWrite(tx -> {
            var query 
                = new Query(CustomUserStorageProviderConstants.QUERY_GET_USER_INFO_BY_NAME,
                parameters(CustomUserStorageProviderConstants.DB_KEY_USER_NAME, username));
            var result = tx.run(query).list();
            if (result.size() == 1){
                return mapUser(realm, result.get(0));
            }
            return null;
          });
        }
        catch(Exception ex) {
            log.warn("Database error: unable to fetch record by username; ex={}", ex.getMessage());
            throw new RuntimeException("Database error: unable to fetch record by username",ex);
        }
        
        // UserModel kcUser = UserStoragePrivateUtil.userLocalStorage(ksession).getUserByUsername(realm, username);
        // if (kcUser == null) {
        //     importUserToKeycloak(realm, neo4jUser);
        // }
       return neo4jUser;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        UserModel neo4jUser;
        try (Session c = DbUtil.getSession(this.model)) {
            neo4jUser = c.executeWrite(tx -> {
            var query 
                = new Query(CustomUserStorageProviderConstants.QUERY_GET_USER_INFO_BY_EMAIL , 
                parameters(CustomUserStorageProviderConstants.DB_KEY_USER_EMAIL, email));
            var result = tx.run(query).list();
            if (result.size() == 1){
                return mapUser(realm, result.get(0));
            }
            return null;
          });
        }
        catch(Exception ex) {
            log.warn("Database error: unable to fetch record by user-email: ex={}", ex.getMessage());
            throw new RuntimeException("Database error: unable to fetch record by email",ex);
        }
        // UserModel kcUser = UserStoragePrivateUtil.userLocalStorage(ksession).getUserByUsername(realm, neo4jUser.getUsername());
        // if (kcUser == null) {
        //     importUserToKeycloak(realm, neo4jUser);
        // }
        return neo4jUser;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        log.info("supportsCredentialType({})",credentialType);
        return PasswordCredentialModel.TYPE.endsWith(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        log.info("isConfiguredFor(realm={},user={},credentialType={})",realm.getName(), user.getUsername(), credentialType);
        // In our case, password is the only type of credential, so we allways return 'true' if
        // this is the credentialType
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if( !this.supportsCredentialType(credentialInput.getType())) {
            return false;
        }
        StorageId sid = new StorageId(user.getId());
        String username = sid.getExternalId();
       
        boolean isPasswordAuthentic = false;
        try (Session s = DbUtil.getSession(this.model)) {
            isPasswordAuthentic = s.executeWrite(tx -> {
                var query = new Query(CustomUserStorageProviderConstants.QUERY_GET_PASSWORD_FOR_USER , parameters(CustomUserStorageProviderConstants.DB_KEY_USER_NAME, username));
                var result = tx.run(query).list();
                if (result.size() == 1){
                    String password = result.get(0).get("password").asString();
                    return password.equals(credentialInput.getChallengeResponse());
                }
                return false;
            });
       }
       catch(Exception ex) {
           log.warn("Database error: unable to validate password: ex={}", ex.getMessage());
           throw new RuntimeException("Database error: unable to validate password",ex);
       }
       return isPasswordAuthentic;
    }

    // UserQueryProvider implementation
    
    @Override
    public int getUsersCount(RealmModel realm) {
        int count = 0;
        try (Session c = DbUtil.getSession(this.model)) {
            count = c.executeWrite(tx -> {
            var query 
                = new Query(CustomUserStorageProviderConstants.QUERY_GET_USER_COUNT);
            var result = tx.run(query).list();
            if (result.size() == 1){
                return result.get(0).get(CustomUserStorageProviderConstants.DB_KEY_USER_COUNT).asInt();
            }
            return 0;
          });
        }
        catch(Exception ex) {
           log.warn("Database error: unable to get user count; ex={}", ex.getMessage());
           throw new RuntimeException("Database error: unable to get user count",ex);
        }
        return count;
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        log.info("getGroupMembersStream: realm={}", realm.getName());
        
        List<UserModel> users = new ArrayList<>();

    
        try (Session c = DbUtil.getSession(this.model)) {
            var txReturn = c.executeWrite(tx -> {
                int skipRecords = firstResult;
                if (firstResult > 0) {
                    skipRecords = firstResult - 1;
                };
                var query = new Query(CustomUserStorageProviderConstants.QUERY_GET_USER_STREAM_WITH_OFFSET_MAXRECORDS
                    , parameters(CustomUserStorageProviderConstants.RECORD_SKIP_KEY, skipRecords, CustomUserStorageProviderConstants.RECORD_LIMIT_KEY, maxResults));
                var result = tx.run(query);
                for (var user: result.list() )
                {
                    // Skip listing user thats already added to keycloak
                    UserModel kcUser = UserStoragePrivateUtil.userLocalStorage(ksession).getUserByUsername(realm, user.get(CustomUserStorageProviderConstants.DB_KEY_USER_NAME).asString());
                    if (kcUser != null) {   
                        continue;
                    }
                    users.add(mapUser(realm,user));
                };
                return null;
          });
        }
        catch(Exception ex) {
           log.warn("Database error: unable to get user member stream; ex={}", ex.getMessage());
           throw new RuntimeException("Database error: unable to get user member stream",ex);
        }
        return users.stream();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        log.info("searchForUserStream: realm={}", realm.getName());

        List<UserModel> users = new ArrayList<>();

        try (Session c = DbUtil.getSession(this.model)) {
            var txReturn = c.executeWrite(tx -> {
                int skipRecords = firstResult;
                if (firstResult > 0) {
                    skipRecords = firstResult - 1;
                };
                String neo4jCompatibleSearchValue;
                if (search.equals(CustomUserStorageProviderConstants.WILDCARD)) {
                    neo4jCompatibleSearchValue = CustomUserStorageProviderConstants.EMPTY_STRING;
                } else {
                    neo4jCompatibleSearchValue = search;
                }
                log.info("user-record {} skip: {}, maxResults: {}", neo4jCompatibleSearchValue, skipRecords, maxResults);
                var query = new Query(CustomUserStorageProviderConstants.QUERY_SEARCH_USER_STREAM_WITH_OFFSET_MAXRECORDS
                    , parameters(CustomUserStorageProviderConstants.DB_KEY_USER_NAME, neo4jCompatibleSearchValue,CustomUserStorageProviderConstants.RECORD_SKIP_KEY,skipRecords, CustomUserStorageProviderConstants.RECORD_LIMIT_KEY, maxResults));
                var result = tx.run(query);
                for (var user: result.list() )
                {
                    // Skip listing user thats already added to keycloak
                    UserModel kcUser = UserStoragePrivateUtil.userLocalStorage(ksession).getUserByUsername(realm, user.get(CustomUserStorageProviderConstants.DB_KEY_USER_NAME).asString());
                    if (kcUser != null) {   
                        continue;
                    }
                    log.info("retrieved user stream: {}", user);
                    users.add(mapUser(realm,user));
                };
                return null;
          });
        }
        catch(Exception ex) {
           log.warn("Database error: unable to get user member stream; ex={}", ex.getMessage());
           throw new RuntimeException("Database error: unable to get user member stream",ex);
        }
        return users.stream();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }
    
    // Need To explore user attributes addition, roles and credentials
    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String userName = user.getUsername();
        try (Session s = DbUtil.getSession(this.model)) {
            var txReturn = s.executeWrite(tx -> {
                var query = new Query(CustomUserStorageProviderConstants.QUERY_DELETE_USER , parameters(CustomUserStorageProviderConstants.DB_KEY_USER_NAME, userName));
                tx.run(query).list();
                return null;
            });
        }
        catch(Exception ex) {
           log.warn("Database error: unable to remove user {};  ex={}", userName, ex.getMessage());
           throw new RuntimeException("Database error: unable to remove user",ex);
        }
        return true;
    }
    private UserModel mapUser(RealmModel realm, MapAccessor rs)  {
        
        DateFormat dateFormat = new SimpleDateFormat(CustomUserStorageProviderConstants.DB_KEY_BIRTH_DATE_FORMAT);
        Date date;
        try {
            date = dateFormat.parse(rs.get(CustomUserStorageProviderConstants.DB_KEY_USER_BIRTHDATE).asString());
        } catch (ParseException e) {
           return null;
        }
        CustomUser user = new CustomUser.Builder(ksession, realm, model, rs.get(CustomUserStorageProviderConstants.DB_KEY_USER_NAME).asString())
          .email(rs.get(CustomUserStorageProviderConstants.DB_KEY_USER_EMAIL).asString())
          .firstName(rs.get(CustomUserStorageProviderConstants.DB_KEY_USER_FIRST_NAME).asString())
          .lastName(rs.get(CustomUserStorageProviderConstants.DB_KEY_USER_LAST_NAME).asString())
          .birthDate(date)
          .build();
        
        return user;
    }

}
