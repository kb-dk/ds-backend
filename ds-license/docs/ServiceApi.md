# ServiceApi

All URIs are relative to *http://$*

Method | HTTP request | Description
------------- | ------------- | -------------
[**extractStatistics**](ServiceApi.md#extractStatistics) | **POST** /monitor | Still TODO, Some statistics for monitoring the application
[**ping**](ServiceApi.md#ping) | **GET** /monitor/ping | Ping the server to check if the server is reachable.
[**probeWhoami**](ServiceApi.md#probeWhoami) | **GET** /monitor/whoami | Extract OAuth2 accessToken in the &#x60;Authorization&#x60; HTTP header and return the roles from it
[**status**](ServiceApi.md#status) | **GET** /monitor/status | Detailed status / health check for the service


<a name="extractStatistics"></a>
# **extractStatistics**
> String extractStatistics()

Still TODO, Some statistics for monitoring the application

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.models.*;
import dk.kb.license.api.ServiceApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");

    ServiceApi apiInstance = new ServiceApi(defaultClient);
    try {
      String result = apiInstance.extractStatistics();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServiceApi#extractStatistics");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | TODO |  -  |

<a name="ping"></a>
# **ping**
> String ping()

Ping the server to check if the server is reachable.

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.models.*;
import dk.kb.license.api.ServiceApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");

    ServiceApi apiInstance = new ServiceApi(defaultClient);
    try {
      String result = apiInstance.ping();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServiceApi#ping");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/plain

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

<a name="probeWhoami"></a>
# **probeWhoami**
> WhoamiDto probeWhoami()

Extract OAuth2 accessToken in the &#x60;Authorization&#x60; HTTP header and return the roles from it

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.ServiceApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

    ServiceApi apiInstance = new ServiceApi(defaultClient);
    try {
      WhoamiDto result = apiInstance.probeWhoami();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServiceApi#probeWhoami");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**WhoamiDto**](WhoamiDto.md)

### Authorization

[KBOAuth](../README.md#KBOAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

<a name="status"></a>
# **status**
> StatusDto status()

Detailed status / health check for the service

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.models.*;
import dk.kb.license.api.ServiceApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");

    ServiceApi apiInstance = new ServiceApi(defaultClient);
    try {
      StatusDto result = apiInstance.status();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServiceApi#status");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**StatusDto**](StatusDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |
**500** | Internal Error |  -  |

