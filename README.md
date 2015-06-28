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
1. Download or clone this repo. The SDK includes a test application, .arr and docs.
2. Copy the slyce.arr from SlyceSDK directory into your project's libs directory.
3. Configure **build.gradle** file:
```ruby
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    compile(name:'slyce', ext:'aar')
}
```





