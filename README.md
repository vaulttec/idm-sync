IDM Synchronizer  [![Build Status](https://travis-ci.org/vaulttec/idm-sync.svg?branch=master)](https://travis-ci.org/vaulttec/idm-sync) [![Docker Image](https://img.shields.io/docker/pulls/tjuerge/idm-sync.svg)](https://hub.docker.com/r/tjuerge/idm-sync)
================

Spring Boot application for synchronizing identity information (group memberships) from an identity provider (Keycloak) with various applications (GitLab, Mattermost, ...)



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
