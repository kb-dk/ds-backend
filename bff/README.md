# bff

**Backend for Frontend implementation for The Royal Library

OAuth proxy for The Royal Library

## Requirements



## Setup


## Build & run

Build with
```
mvn package
```

Test the webservice with
```
mvn jetty:run
```


## Test it

retrieve cookie
curl -s -X GET 'http://localhost:9062/bff/v1/authenticate'


test with poc-middleware
curl -i -X GET "http://localhost:9062/bff/v1/proxy/poc-middleware/v1/books?query=horses%20OR%20cows&max=5&format=JSON" -b "Authorization=xxxx1234...."

make sure that poc-middleware and poc-backend is running
