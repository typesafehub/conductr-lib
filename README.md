# Typesafe ConductR Bundle Library

## Introduction

This project provides a number of libraries to facilitate ConductR's status service and its service lookup service. The libraries are intended to be delivered by the Typesafe Reactive Platform (Typesafe RP) and are structured as follows:

* conductr-bundle-lib
* scala-conductr-bundle-lib
* akka-conductr-bundle-lib
* play-conductr-bundle-lib

## conductr-bundle-lib

This library provides a base level of functionality mainly formed around constructing the requisite payloads for ConductR's RESTful services. The library is pure Java and has no dependencies other than the JDK.

Two services are covered by this library:

* LocationService
* StatusService

### Location Service

ConductR's location service is able to respond with a URL declaring where a given service (as named by a bundle component's endpoint) resides. The http payload can be constructed as follows:

```java
HttpPayload payload = LocationService.createLookupPayload("/someservice")
```

The `HttpPayload` object may then be queried for elements that will help you make an http request. These methods are:

* `getUrl`
* `getRequestMethod`
* `getFollowRedirects`

When processing the http response you should check for the following http status codes:

* 308 - permanent redirect - the service can be found at the location indicated by the `Location` header
* 404 - not found - the service cannot be located at this time

You should also prepare for timing out on a request and process as per a 404.

### Status Service

Conduct's status service is required by a bundle component in order to signal when it has started. A successful startup is anything that the application is required to do to become available for processing. For example, this may involved validating configuration. The http payload can be constructed as follows:

 ```java
 HttpPayload payload = StatusService.createSignalStartedPayload()
 ```

The `HttpPayload` object may then be queried for elements in the same way as for the location service of the previous section.

When processing the http response you should check for the following http status codes:

* 2xx - success - any 200 series response is a success meaning that ConductR has
* xxx - failure - anything else constitutes an error and you should cause the bundle component to exit

You should also prepare for timing out on a request and exit the bundle component if this occurs.
