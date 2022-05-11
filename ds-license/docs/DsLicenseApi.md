# DsLicenseApi

All URIs are relative to *http://localhost/ds-license/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getGreeting**](DsLicenseApi.md#getGreeting) | **GET** /hello | Request a Hello World message, for testing purposes
[**ping**](DsLicenseApi.md#ping) | **GET** /ping | Ping the server to check if the server is reachable.


<a name="getGreeting"></a>
# **getGreeting**
> HelloReplyDto getGreeting(alternateHello)

Request a Hello World message, for testing purposes

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost/ds-license/v1");

    DsLicenseApi apiInstance = new DsLicenseApi(defaultClient);
    String alternateHello = "\"World\""; // String | Optional alternative to using the word 'Hello' in the reply
    try {
      HelloReplyDto result = apiInstance.getGreeting(alternateHello);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getGreeting");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alternateHello** | **String**| Optional alternative to using the word &#39;Hello&#39; in the reply | [optional] [default to &quot;World&quot;]

### Return type

[**HelloReplyDto**](HelloReplyDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | A JSON structure containing a Hello World message |  -  |

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
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost/ds-license/v1");

    DsLicenseApi apiInstance = new DsLicenseApi(defaultClient);
    try {
      String result = apiInstance.ping();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#ping");
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
 - **Accept**: text/plain, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |
**406** | Not Acceptable |  -  |
**500** | Internal Error |  -  |

