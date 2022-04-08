

# InlineObjectDto

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**image** | [**File**](File.md) | The image to use as source for the colorization | 
**method** | [**MethodEnum**](#MethodEnum) | The algorithm used to colorize the image |  [optional]
**intensity** | **Double** | The intensity of the colorization |  [optional]



## Enum: MethodEnum

Name | Value
---- | -----
RANDOM | &quot;Random&quot;
CNN_1 | &quot;CNN-1&quot;
GAN_1 | &quot;GAN-1&quot;



