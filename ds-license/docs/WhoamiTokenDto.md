

# WhoamiTokenDto

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**present** | **Boolean** | True if an OAuth2 accessToken was present in the request, else false | 
**valid** | **Boolean** | True is an OAuth2 accessToken was present and valid, else false |  [optional]
**error** | **String** | If the accessToken is not valid, the reason will be stated here |  [optional]
**roles** | **List&lt;String&gt;** | The roles stated in the OAuth2 accessToken |  [optional]



