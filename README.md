Slyce Android SDK
==============

The Slyce Android SDK makes it easy to add visual search capabilities to mobile apps.

## Contents

- [Use Cases](#use-cases)
- [Requirements](#requirements)
- [Add the SDK to Your Project](#add-the-sdk-to-your-project)
- [Credentials](#credentials)
- [Documentation](#documentation)

## Use Cases

The SDK enables 3 major modes of operation: **Headless**, **Headless/Camera**, **Full UI**.

### Headless

SDK provides the methods required to submit images and receive results. 
Ideal for cases where the app already handles the camera and has its own UI.

### Headless/Camera

A headless mode where the SDK manages the camera. **App developers are responsible to implement their own UI.** 
Ideal for cases where app developers would like to utilize SDK features such as continues barcodes detection, yet would like to maintain full flexibility with anything related to the UI/UX.

### Full UI

The SDK takes care of the entire flow from scanning to getting results. The SDK provides a UI that can be customized. Provides a turnkey scan-to-products solution.

## Requirements
* Android 4.0 (API Level 14) or higher
* Android Studio developemt enviourment
* A Slyce client ID 

## Add the SDK to Your Project
1. Download or clone this repo. The SDK includes a test application, .arr, java docs, release notes and integration doc.
2. Copy the slyce.arr from SlyceSDK directory into your project's libs directory.
3. In your `build.gradle` add `flatDir` entry to your repositories
```ruby
repositories {
    flatDir {
        dirs 'libs'
    }
}
```
4. Add dependency to Slyce SDK. 
```ruby
dependencies {
    compile(name:'slyce', ext:'aar')
}
```

## Credentials

Your mobile integration requires a `client_id`.

You can obtain these Slyce API credentials by visiting the [Products Services page on Slyce site](http://slyce.it/products-services) or via a Slyce representative.

After you obtain one, you should use it when initializing the central `SlyceRequest` object when using of the SDK modes.

## Documentation

* These docs in the SDK, which include an overview of usage (PDF), [API Reference (Java docs)](http://github.com/Slyce-Inc/Slyce-Android-SDK/index.html), and sample code.
* The sample app included in this SDK.


