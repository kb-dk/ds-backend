

# StatusDto

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**application** | **String** | The name of the application | 
**version** | **String** | The version of the application | 
**build** | **String** | When the application was build |  [optional]
**java** | **String** | The Java version that runs the container |  [optional]
**heap** | **Long** | The maximum number of bytes available to the container in megabytes |  [optional]
**server** | **String** | The hostname for the server |  [optional]
**health** | **String** | Self diagnosed health |  [optional]
**gitCommitChecksum** | **String** | The checksum of the deployed commit. |  [optional]
**gitBranch** | **String** | The current deployed branch. |  [optional]
**gitCurrentTag** | **String** | The current tag of the deployed branch. |  [optional]
**gitClosestTag** | **String** | The closest tag of the deployed branch. |  [optional]
**gitCommitTime** | **String** | The time for the latest commit of the deplyed branch. |  [optional]



