# DsLicenseApi

All URIs are relative to *http://$*

Method | HTTP request | Description
------------- | ------------- | -------------
[**checkAccessForIds**](DsLicenseApi.md#checkAccessForIds) | **POST** /checkAccessForIds | Takes an array of recordIds. Will filter the ids and return only those that the users has access to by the licences granted to the user. ID filter field is defined in the YAML configuration
[**checkAccessForResourceIds**](DsLicenseApi.md#checkAccessForResourceIds) | **POST** /checkAccessForResourceIds | Takes an array of resource Ids. Will filter the ids and return only those that the users has access to by the licences granted to the user. ResourceID filter field is defined in the YAML configuration
[**getUserGroups**](DsLicenseApi.md#getUserGroups) | **POST** /getUserGroups | Get the groups that the user has access to
[**getUserGroupsAndLicenses**](DsLicenseApi.md#getUserGroupsAndLicenses) | **POST** /getUserGroupsAndLicenses | Get all licenses and groups/presentationtypes that the user has access to. Will also list all licenses defined and presentationtypes
[**getUserLicenseQuery**](DsLicenseApi.md#getUserLicenseQuery) | **POST** /getUserLicenseQuery | Shows the filter query for Solr generated from the user attributes. PresentationType are defined in configuration. Example: Search
[**getUserLicenses**](DsLicenseApi.md#getUserLicenses) | **POST** /getUserLicenses | Get a list of all licences that validates from user attributes.
[**validateAccess**](DsLicenseApi.md#validateAccess) | **POST** /validateAccess | Validate if user has access to all groups in input.


<a name="checkAccessForIds"></a>
# **checkAccessForIds**
> CheckAccessForIdsOutputDto checkAccessForIds(checkAccessForIdsInputDto)

Takes an array of recordIds. Will filter the ids and return only those that the users has access to by the licences granted to the user. ID filter field is defined in the YAML configuration

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

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

[KBOAuth](../README.md#KBOAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Returns the IDs that has not been filtered by the query. Also return the Solr filter query that was used. |  -  |

<a name="checkAccessForResourceIds"></a>
# **checkAccessForResourceIds**
> CheckAccessForIdsOutputDto checkAccessForResourceIds(checkAccessForIdsInputDto)

Takes an array of resource Ids. Will filter the ids and return only those that the users has access to by the licences granted to the user. ResourceID filter field is defined in the YAML configuration

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

    DsLicenseApi apiInstance = new DsLicenseApi(defaultClient);
    CheckAccessForIdsInputDto checkAccessForIdsInputDto = new CheckAccessForIdsInputDto(); // CheckAccessForIdsInputDto | 
    try {
      CheckAccessForIdsOutputDto result = apiInstance.checkAccessForResourceIds(checkAccessForIdsInputDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#checkAccessForResourceIds");
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

[KBOAuth](../README.md#KBOAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Returns the IDs that has not been filtered by the query. Also return the Solr filter query that was used. IDs that exists but with no access will be return in nonAccessId field |  -  |

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
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

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

[KBOAuth](../README.md#KBOAuth)

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
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

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

[KBOAuth](../README.md#KBOAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | List all licenses and groups/presentationtypes that the user has access to. Will also list all licenses defined and presentationtypes |  -  |

<a name="getUserLicenseQuery"></a>
# **getUserLicenseQuery**
> GetUsersFilterQueryOutputDto getUserLicenseQuery(getUserQueryInputDto)

Shows the filter query for Solr generated from the user attributes. PresentationType are defined in configuration. Example: Search

### Example
```java
// Import classes:
import dk.kb.license.ApiClient;
import dk.kb.license.ApiException;
import dk.kb.license.Configuration;
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

    DsLicenseApi apiInstance = new DsLicenseApi(defaultClient);
    GetUserQueryInputDto getUserQueryInputDto = new GetUserQueryInputDto(); // GetUserQueryInputDto | 
    try {
      GetUsersFilterQueryOutputDto result = apiInstance.getUserLicenseQuery(getUserQueryInputDto);
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

[**GetUsersFilterQueryOutputDto**](GetUsersFilterQueryOutputDto.md)

### Authorization

[KBOAuth](../README.md#KBOAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The Solr filter query. |  -  |

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
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

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

[KBOAuth](../README.md#KBOAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Returns the names of all the licences that validates for the user. |  -  |

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
import dk.kb.license.auth.*;
import dk.kb.license.models.*;
import dk.kb.license.api.DsLicenseApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://$");
    
    // Configure OAuth2 access token for authorization: KBOAuth
    OAuth KBOAuth = (OAuth) defaultClient.getAuthentication("KBOAuth");
    KBOAuth.setAccessToken("YOUR ACCESS TOKEN");

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

[KBOAuth](../README.md#KBOAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | True or false |  -  |

