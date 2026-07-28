# Developer documentation

This project is a repository found at [github](https://github.com/kb-dk/ds_backend)
from the Royal Danish Library.

The information in this document is aimed at developers who are going to work at the Digitale Samlinger project

This project is a parent project for the ds-xxx modules. It contains a pom.xml which is the parent pom for all
submodules (ds-xxx). The submodules can be found in pom.xml. The repository has all the modules inside so you only need
to download this repository.

```shell
git clone git@github.com:kb-dk/ds_backend.git
```

## Local development

If you want to use Docker to have all the services up and running locally you also need to clone `aegis` where you find
the .env file.

```shell
cd ds_backend
git clone git@github.com:kb-dk/aegis.git
```

Keycloak in some use cases needs to have the same name on the host as in the container, so you need to add
`127.0.0.1 keycloak.local` in your `/etc/hosts` file. You can always check `docker-compose.yml` for what port different
services uses.

You can now start the platform up with these Docker commands:

```shell
docker compose up --detach --build
docker compose logs --follow
```

If you need to see the logs of the containers either run `docker compose logs` that shows all the containers logs in one
go, or example `docker compose logs ds-discover` to only see logs from one container.

### To get data into the platform use the following commands from commandline

#### Retrieve keycloak token from local keycloak (it is valid for an hour)

```shell
export ACCESS_TOKEN=$(curl -X POST "http://keycloak.local:8087/realms/DS/protocol/openid-connect/token" -d "client_id=kb-ad" -d "client_secret=my-super-secure-dev-secret-12345" -d "username=testuser" -d "password=password123" -d "grant_type=password" | jq -r .access_token)
```

OBS: The Keycloak container is sometimes a little while to start, so if you get the following error
`curl: (56) Recv failure: Connection reset by peer`, just wait a second or two and run the command again.

#### Fetch records from Preservica and save it in ds_records table:

```shell
curl -X 'GET' 'http://localhost:8084/ds-datahandler/v1/oai/import/delta?oaiTarget=ds.radiotv' -H 'accept: application/json' -H "Authorization: Bearer $ACCESS_TOKEN"
```

#### Solr indexing (we only have one collection (read and write in one))

tv

```shell
curl -X 'GET' 'http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.tv&type=full' -H "Authorization: Bearer $ACCESS_TOKEN"
```

radio

```shell
curl -X 'GET' 'http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.radio&type=full' -H "Authorization: Bearer $ACCESS_TOKEN"
```

When you have used the following commands you can see the data in Solr here: `http://localhost:8089`.

#### Kaltura upload so a record in the frontend can be seen

```shell
curl -X 'POST' 'http://localhost:8084/ds-datahandler/v1/kaltura/deltaupload' -H "Authorization: Bearer $ACCESS_TOKEN" -H 'accept: */*' -d ''
```

#### Solr reindex so kaltura_id can be found in Solr

tv

```shell
curl -X 'GET' 'http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.tv&type=full' -H "Authorization: Bearer $ACCESS_TOKEN"
```

radio

```shell
curl -X 'GET' 'http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.radio&type=full' -H "Authorization: Bearer $ACCESS_TOKEN"
```

#### Find records with uploaded files to Kaltura

In your browser hit `http://localhost:8090` to see the frontend. Search after `has_kaltura_id:true` in the search bar,
and click on a record and click play on the video.

## Deploy of services

If you need to deploy the services, first run `mvn clean package` or `mvn clean install` in the root of "ds_backend"
folder. Look in aegis README.md for a guide.

## Purpose

The purpose of a maven-multi-project is to consolidate all versions in the parent pom so that the submodules only depend
on dependencies noted in the parent pom file. If a version changes in the parent pom all submodules are updated at once.

If a submodule use a specific dependency only for that module it can be added in the submodule having the version
specified - hence it is not added to the parent pom.

We have chosen to define the build version in the parent pom which is inherited by all submodules. In that way the build
ensures that the dependencies between submodules are consistent. This choice implies that all modules should be released
when doing a hotfix or a new major release. In the future the dependencies between the submodules must be replaced by
loose coupling REST services.