# DsLicenseApi

All URIs are relative to *http://localhost/ds-license/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addBook**](DsLicenseApi.md#addBook) | **POST** /book | Add or update a single book
[**colorize**](DsLicenseApi.md#colorize) | **POST** /colorizer | Perform some image processing and return the result as an image
[**deleteBook**](DsLicenseApi.md#deleteBook) | **DELETE** /book/{id} | Deletes metadata for a single book
[**getArticle**](DsLicenseApi.md#getArticle) | **GET** /article/{id} | Sample OpenAPI definition for a service that constructs a PDF and delivers it
[**getBook**](DsLicenseApi.md#getBook) | **GET** /book/{id} | Retrieves metadata for a single book
[**getBooks**](DsLicenseApi.md#getBooks) | **GET** /books | Delivers metadata on books
[**getGreeting**](DsLicenseApi.md#getGreeting) | **GET** /hello | Request a Hello World message, for testing purposes
[**ping**](DsLicenseApi.md#ping) | **GET** /ping | Ping the server to check if the server is reachable.


<a name="addBook"></a>
# **addBook**
> BookDto addBook(bookDto)

Add or update a single book

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
    BookDto bookDto = new BookDto(); // BookDto | Add or update a single book
    try {
      BookDto result = apiInstance.addBook(bookDto);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#addBook");
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
 **bookDto** | [**BookDto**](BookDto.md)| Add or update a single book |

### Return type

[**BookDto**](BookDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/xml, application/x-www-form-urlencoded
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | If the book was added successfully |  -  |

<a name="colorize"></a>
# **colorize**
> String colorize(image, method, intensity)

Perform some image processing and return the result as an image

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
    File image = new File("/path/to/file"); // File | The image to use as source for the colorization
    String method = "GAN-1"; // String | The algorithm used to colorize the image
    Double intensity = 3.4D; // Double | The intensity of the colorization
    try {
      String result = apiInstance.colorize(image, method, intensity);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#colorize");
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
 **image** | **File**| The image to use as source for the colorization |
 **method** | **String**| The algorithm used to colorize the image | [optional] [default to GAN-1] [enum: Random, CNN-1, GAN-1]
 **intensity** | **Double**| The intensity of the colorization | [optional] [default to 0.8d]

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: image/jpeg

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The colorized image |  -  |

<a name="deleteBook"></a>
# **deleteBook**
> String deleteBook(id)

Deletes metadata for a single book

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
    String id = book_id87; // String | The ID for the book to delete
    try {
      String result = apiInstance.deleteBook(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#deleteBook");
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
 **id** | **String**| The ID for the book to delete |

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
**404** | Not found |  -  |

<a name="getArticle"></a>
# **getArticle**
> String getArticle(id)

Sample OpenAPI definition for a service that constructs a PDF and delivers it

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
    String id = article-123A-v2; // String | The ID of the article to process
    try {
      String result = apiInstance.getArticle(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getArticle");
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
 **id** | **String**| The ID of the article to process |

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
**404** | Article ID is unknown |  -  |

<a name="getBook"></a>
# **getBook**
> BookDto getBook(id)

Retrieves metadata for a single book

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
    String id = book_id87; // String | The ID for the book to retrieve
    try {
      BookDto result = apiInstance.getBook(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getBook");
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
 **id** | **String**| The ID for the book to retrieve |

### Return type

[**BookDto**](BookDto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/xml, text/plain

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Structured representation of the Book. |  -  |
**404** | Not found |  -  |

<a name="getBooks"></a>
# **getBooks**
> String getBooks(query, max, format)

Delivers metadata on books

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
    String query = horses OR cows; // String | Search query for the books
    Long max = 87; // Long | The maximum number of books to return
    String format = JSONL; // String | The delivery format. This can also be specified using headers, as seen in the Responses section. If both headers and format are specified, format takes precedence.  * JSONL: Newline separated single-line JSON representations of Books * JSON: Valid JSON in the form of a single array of Books * XML: Valid XML in the form of a single container with Books * CSV: Comma separated, missing values represented with nothing, strings encapsulated in quotes 
    try {
      String result = apiInstance.getBooks(query, max, format);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DsLicenseApi#getBooks");
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
 **query** | **String**| Search query for the books | [optional]
 **max** | **Long**| The maximum number of books to return | [optional]
 **format** | **String**| The delivery format. This can also be specified using headers, as seen in the Responses section. If both headers and format are specified, format takes precedence.  * JSONL: Newline separated single-line JSON representations of Books * JSON: Valid JSON in the form of a single array of Books * XML: Valid XML in the form of a single container with Books * CSV: Comma separated, missing values represented with nothing, strings encapsulated in quotes  | [optional] [enum: JSONL, JSON, XML, CSV]

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/x-ndjson, application/json, application/xml, text/csv, text/plain

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |
**400** | HTTP 400: Bad request |  -  |

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

