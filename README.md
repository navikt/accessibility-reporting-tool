# accessibility-reporting-tool

App for rapportering av tilgjengelighet fra team på NAV sine sider.

## Utvikling

* `docker-compose up`
* start app med VM-option: `-Dlogback.configurationFile=logback-dev.xml` og miljøvaribler:

```
AZURE_APP_CLIENT_ID=a11y;AZURE_APP_WELL_KNOWN_URL=http://host.docker.internal:8080/issueissue/.well-known/openid-configuration;DB_DATABASE=a11y;DB_HOST=localhost;DB_PASSWORD=a11y;DB_PORT=5432;DB_USERNAME=postgres'
```

Appen er satt opp med defaultverdier for mocked jwt som kan endres i definisjonen av mock-oauth2-server
i [docker-compose](docker-compose.yml)

```
"aud": ["a11y"],
"email" : "carl@good.morning",
"name": "Carl Good Morning",
"oid": "tadda-i-fixed-it"
```

Default verdi på dev-logging er DEBUG, kan endres i [logback-dev.xml](app/src/main/resources/logback-dev.xml)

