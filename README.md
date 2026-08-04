# Developer documentation

This project is a repository found at [github](https://github.com/kb-dk/ds-backend) from the Royal Danish Library.

The information in this document is aimed at developers who are going to work at the Digitale Samlinger project.

The repository is the "parent" project with several ds-xxx modules in subdirectories. The parent pom.xml serves two
purposes:

- Defines the shared configuration for all the ds-xxx modules
- Lists which modules belong to the project

# Local development

```shell
git clone git@github.com:kb-dk/ds-backend.git
```

If you want to use Docker to have all services up and running locally you need to clone `aegis` which contains the .env
file.

```shell
cd ds-backend
git clone git@github.com:kb-dk/aegis.git
```

You also need to clone the frontend `ds-web` repository. `ds-web` should not be cloned into the `ds-backend`
directory, but be beside it. Then clone `aegis` inside `ds-web`.

```shell
cd ..
git clone git@github.com:kb-dk/ds-web.git
cd ds-web
git checkout maltand
git clone git@github.com:kb-dk/aegis.git
docker compose up --detach --build
```

Your folder structure should now look like this:

```text
{folder_where_you_have_your_repositories}
├── {some_random_repository}
├── ds-web
│   └── aegis
└── ds-backend
    ├── aegis
    ├── ds-{xxx}
    └── ...
```

In some situations, Keycloak needs to use the same hostname both on your computer and inside the container. To set this
up, add the following line to your `/etc/hosts` file:

```text
127.0.0.1 keycloak.local
```

This tells your computer that the address `keycloak.local` points to your local machine `(127.0.0.1)`.

## Start the Docker services

**You can always check `docker-compose.yml` for what port different services uses.**

```shell
docker compose up --detach --build
```

### Service logs

Shows all containers logs:

```shell
docker compose logs --follow
```

To view logs from one container, run:
example:
`docker compose logs ds-discover`

## Load data

### Get a Keycloak token (valid for 1 hour)

```shell
export ACCESS_TOKEN=$(curl --request POST "http://keycloak.local:8087/realms/DS/protocol/openid-connect/token" \
--data "client_id=kb-ad" \
--data "client_secret=my-super-secure-dev-secret-12345" \
--data "username=testuser" \
--data "password=password123" \
--data "grant_type=password" \
| jq --raw-output .access_token)
```

**Note**
The Keycloak container may take a moment to start. If you get a connection error like the following
`curl: (56) Recv failure: Connection reset by peer`, wait a few seconds and try again.

### Fetch records from Preservica and save it in `ds_records` table:

```shell
curl --request GET "http://localhost:8084/ds-datahandler/v1/oai/import/delta?oaiTarget=stage_preservica_dr_arkiv" \
--header "Authorization: Bearer $ACCESS_TOKEN"
```

### Index records in Solr (we only have one collection (read and write in one))

tv

```shell
curl --request GET "http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.tv&type=full" \
--header "Authorization: Bearer $ACCESS_TOKEN"
```

radio

```shell
curl --request GET "http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.radio&type=full" \
--header "Authorization: Bearer $ACCESS_TOKEN"
```

When you have used the following commands you can see the data in Solr here: `http://localhost:8089`.

### Kaltura upload

```shell
curl --request POST "http://localhost:8084/ds-datahandler/v1/kaltura/deltaupload" \
--header "Authorization: Bearer $ACCESS_TOKEN"
```

### Reindex Solr

So kaltura_id can be found in Solr

tv

```shell
curl --request GET "http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.tv&type=full" \
--header "Authorization: Bearer $ACCESS_TOKEN"
```

radio

```shell
curl --request GET "http://localhost:8084/ds-datahandler/v1/solr/index?origin=ds.radio&type=full" \
--header "Authorization: Bearer $ACCESS_TOKEN"
```

### View the Frontend

Open your browser and go to `http://localhost:3000`. Search for `has_kaltura_id:true` and click on a record to play the
video.

# Deploy services

If you need to deploy the services, first run `mvn clean package` or `mvn clean install` in the root of "ds-backend"
folder. Look in aegis README.md for a guide.

# Purpose

The purpose of a maven-multi-project is to consolidate all versions in the parent pom so that the submodules only depend
on dependencies noted in the parent pom file. If a version changes in the parent pom all submodules are updated at once.

If a submodule use a specific dependency only for that module it can be added in the submodule having the version
specified - hence it is not added to the parent pom.

We have chosen to define the build version in the parent pom which is inherited by all submodules. In that way the build
ensures that the dependencies between submodules are consistent. This choice implies that all modules should be released
when doing a hotfix or a new major release. In the future the dependencies between the submodules must be replaced by
loose coupling REST services.