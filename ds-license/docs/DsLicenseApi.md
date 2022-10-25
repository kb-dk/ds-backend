# DsLicenseApi

All URIs are relative to *http://localhost/ds-license/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**checkAccessForIds**](DsLicenseApi.md#checkAccessForIds) | **POST** /checkAccessForIds | Takes an array of recordIds. Will filter the ids and return only those that the users has access to by the licences granted to the user.
[**extractStatistics**](DsLicenseApi.md#extractStatistics) | **POST** /monitor | Still TODO, Some statistics for monitoring the application
[**getGreeting**](DsLicenseApi.md#getGreeting) | **GET** /hello | Request a Hello World message, for testing purposes
[**getUserGroups**](DsLicenseApi.md#getUserGroups) | **POST** /getUserGroups | Get the groups that the user has access to
[**getUserGroupsAndLicenses**](DsLicenseApi.md#getUserGroupsAndLicenses) | **POST** /getUserGroupsAndLicenses | Get all licenses and groups/presentationtypes that the user has access to. Will also list all licenses defined and presentationtypes
[**getUserLicenseQuery**](DsLicenseApi.md#getUserLicenseQuery) | **POST** /getUserLicenseQuery | Shows the filter query for Solr generated from the user attributes. This query is used to filter IDs
[**getUserLicenses**](DsLicenseApi.md#getUserLicenses) | **POST** /getUserLicenses | Get a list of all licences that validates from user attributes.
[**ping**](DsLicenseApi.md#ping) | **GET** /ping | Ping the server to check if the server is reachable.
[**validateAccess**](DsLicenseApi.md#validateAccess) | **POST** /validateAccess | Validate if user has access to all groups in input.


<a name="checkAccessForIds"></a>
# **checkAccessForIds**
> CheckAccessForIdsOutputDto checkAccessForIds(checkAccessForIdsInputDto)

Takes an array of recordIds. Will filter the ids and return only those that the users has access to by the licences granted to the user.

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
    CheckAccessForIdsInputDto checkAccessForIdsInputDto = new CheckAccessForIdsInputDto(); // CheckAccessForIdsInputDto | 
    try {
      CheckAccessForIdsOutputDto result = apiInstance.checkAccessForIds(checkAccessForIdsInputDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#checkAccessForIds");
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
 **checkAccessForIdsInputDto** | [**CheckAccessForIdsInputDto**](CheckAccessForIdsInputDto.md)|  | [optional]

### Return type

[**CheckAccessForIdsOutputDto**](CheckAccessForIdsOutputDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Returns the IDs that has not been filtered by the query. Also return the Solr filter query that was used. |  -  |

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
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost/ds-license/v1");

    DsLicenseApi apiInstance = new DsLicenseApi(defaultClient);
    try {
      String result = apiInstance.extractStatistics();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#extractStatistics");
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

<a name="getUserGroups"></a>
# **getUserGroups**
> GetUserGroupsOutputDto getUserGroups(getUserGroupsInputDto)

Get the groups that the user has access to

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
    GetUserGroupsInputDto getUserGroupsInputDto = new GetUserGroupsInputDto(); // GetUserGroupsInputDto | 
    try {
      GetUserGroupsOutputDto result = apiInstance.getUserGroups(getUserGroupsInputDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getUserGroups");
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
 **getUserGroupsInputDto** | [**GetUserGroupsInputDto**](GetUserGroupsInputDto.md)|  | [optional]

### Return type

[**GetUserGroupsOutputDto**](GetUserGroupsOutputDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Names of the groups and presentationtype that the user has access to |  -  |

<a name="getUserGroupsAndLicenses"></a>
# **getUserGroupsAndLicenses**
> GetUserGroupsAndLicensesOutputDto getUserGroupsAndLicenses(getUserGroupsAndLicensesInputDto)

Get all licenses and groups/presentationtypes that the user has access to. Will also list all licenses defined and presentationtypes

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
    GetUserGroupsAndLicensesInputDto getUserGroupsAndLicensesInputDto = new GetUserGroupsAndLicensesInputDto(); // GetUserGroupsAndLicensesInputDto | 
    try {
      GetUserGroupsAndLicensesOutputDto result = apiInstance.getUserGroupsAndLicenses(getUserGroupsAndLicensesInputDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getUserGroupsAndLicenses");
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
 **getUserGroupsAndLicensesInputDto** | [**GetUserGroupsAndLicensesInputDto**](GetUserGroupsAndLicensesInputDto.md)|  | [optional]

### Return type

[**GetUserGroupsAndLicensesOutputDto**](GetUserGroupsAndLicensesOutputDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | List all licenses and groups/presentationtypes that the user has access to. Will also list all licenses defined and presentationtypes |  -  |

<a name="getUserLicenseQuery"></a>
# **getUserLicenseQuery**
> String getUserLicenseQuery(getUserQueryInputDto)

Shows the filter query for Solr generated from the user attributes. This query is used to filter IDs

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
    GetUserQueryInputDto getUserQueryInputDto = new GetUserQueryInputDto(); // GetUserQueryInputDto | 
    try {
      String result = apiInstance.getUserLicenseQuery(getUserQueryInputDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getUserLicenseQuery");
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
 **getUserQueryInputDto** | [**GetUserQueryInputDto**](GetUserQueryInputDto.md)|  | [optional]

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The Solr filter query |  -  |

<a name="getUserLicenses"></a>
# **getUserLicenses**
> GetUsersLicensesOutputDto getUserLicenses(getUsersLicensesInputDto)

Get a list of all licences that validates from user attributes.

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
    GetUsersLicensesInputDto getUsersLicensesInputDto = new GetUsersLicensesInputDto(); // GetUsersLicensesInputDto | 
    try {
      GetUsersLicensesOutputDto result = apiInstance.getUserLicenses(getUsersLicensesInputDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getUserLicenses");
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
 **getUsersLicensesInputDto** | [**GetUsersLicensesInputDto**](GetUsersLicensesInputDto.md)|  | [optional]

### Return type

[**GetUsersLicensesOutputDto**](GetUsersLicensesOutputDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Returns the names of all the licences that validates for the user. |  -  |

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

<a name="validateAccess"></a>
# **validateAccess**
> ValidateAccessOutputDto validateAccess(validateAccessInputDto)

Validate if user has access to all groups in input.

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
    ValidateAccessInputDto validateAccessInputDto = new ValidateAccessInputDto(); // ValidateAccessInputDto | 
    try {
      ValidateAccessOutputDto result = apiInstance.validateAccess(validateAccessInputDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#validateAccess");
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
 **validateAccessInputDto** | [**ValidateAccessInputDto**](ValidateAccessInputDto.md)|  | [optional]

### Return type

[**ValidateAccessOutputDto**](ValidateAccessOutputDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | True or false |  -  |

