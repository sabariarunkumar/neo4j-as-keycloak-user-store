package com.sabari.user;

public final class CustomUserStorageProviderConstants {
    public static final String CONFIG_KEY_NEO4J_DRIVER = "neo4j";
    public static final String CONFIG_KEY_NEO4J_CONNECTION_URL = "connectionUrl";
    public static final String CONFIG_KEY_DB_USERNAME = "username";
    public static final String CONFIG_KEY_DB_PASSWORD = "password";
    public static final String CONFIG_KEY_VALIDATION_QUERY = "validationQuery";
    public static final String WILDCARD = "*";
    public static final String EMPTY_STRING = "";
    public static final String ATTRIBUTE_USER_SOURCE_KEY = "userSource";    
    public static final String ATTRIBUTE_USER_SOURCE_VALUE = "neo4j";

    
    public static final String DB_KEY_USER_NAME = "userName";
    public static final String DB_KEY_USER_EMAIL = "email";  
    public static final String DB_KEY_USER_COUNT = "count";  
    public static final String DB_KEY_USER_BIRTHDATE = "birthDate";   
    public static final String DB_KEY_USER_FIRST_NAME = "firstName";      
    public static final String DB_KEY_USER_LAST_NAME = "lastName";  


    public static final String DB_KEY_BIRTH_DATE_FORMAT =  "yyyy-MM-dd";
    
    
    
    public static final String RECORD_SKIP_KEY = "skip";    
    public static final String RECORD_LIMIT_KEY = "maxResults";
    public static final String NEO4J_PROVIDER_ID = "custom-neo4j-user-provider";    
    public static final String KC_RESOURCE_PATH_USER_SUFFIX = "users/";
    public static final String KC_EVENT_CREATE = "CREATE";





    public static final String QUERY_GET_USER_INFO_BY_NAME = "MATCH (u:User {userName: $userName}) RETURN u.userName as userName,u.firstName as firstName, u.lastName as lastName, u.email as email, u.birthDate as birthDate LIMIT 1;";
    public static final String QUERY_GET_USER_INFO_BY_EMAIL =  "MATCH (u:User {email: $email}) RETURN u.userName as userName,u.firstName as firstName, u.lastName as lastName, u.email as email, u.birthDate as birthDate LIMIT 1;" ;
    public static final String QUERY_GET_PASSWORD_FOR_USER = "MATCH (user:User {userName: $userName}) -[:HAS_PASSWORD]-> (password:Password) RETURN password.hash as password LIMIT 1" ;
    public static final String QUERY_GET_USER_COUNT = "Match (u:User) Return COUNT(u) as count;";
    public static final String QUERY_GET_USER_STREAM_WITH_OFFSET_MAXRECORDS = "Match (u:User) Return u.userName as userName,u.firstName as firstName, u.lastName as lastName, u.email as email, u.birthDate as birthDate ORDER BY u.userName SKIP $skip LIMIT $maxResults;";
    public static final String QUERY_SEARCH_USER_STREAM_WITH_OFFSET_MAXRECORDS = "Match (u:User) where u.userName CONTAINS $userName Return u.userName as userName,u.firstName as firstName, u.lastName as lastName, u.email as email, u.birthDate as birthDate ORDER BY u.userName SKIP $skip LIMIT $maxResults;";
    public static final String QUERY_CREATE_USER = "CREATE (:User {firstName: $firstName, lastName: $lastName,email: $email,userName: $userName});";
    public static final String QUERY_DELETE_USER = "MATCH (u:User {userName: $userName}) DETACH DELETE u;";
}