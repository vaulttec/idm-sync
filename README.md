IDM Synchronizer
================

Spring Boot application for synchronizing identity information (group memberships) from an identity provider (Keycloak) with various applications (GitLab)



## Install Maven Wrapper
```
cd /path/to/project
mvn -N io.takari:maven:wrapper
```

## Run the project with

```
./mvnw clean spring-boot:run
```

Open browser to http://localhost:8080/


## To package the project run

```
./mvnw clean package
```
