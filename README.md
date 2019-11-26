Slyce Android SDK
=================



<!-- official release -->
[![GitHub release](https://img.shields.io/github/release/Slyce-Inc/Slyce-Android-SDK.svg?style=flat-square)](https://github.com/Slyce-Inc/Slyce-Android-SDK/releases)

<!-- pre-release -->
<!-- [![GitHub release](https://img.shields.io/github/release/Slyce-Inc/Slyce-Android-SDK/all.svg?style=flat-square)](https://github.com/Slyce-Inc/Slyce-Android-SDK/releases) -->

The Slyce Android SDK makes it easy to add visual search capabilities to mobile apps.

### [About the SlyceSDK](https://docs.slyce.it)


### [API Documentation](https://slyce-inc.github.io/SlyceAndroid.github.io/)

### (Beta) Maven Distribution

Starting with version 5.13.1, the Slyce Android SDK is now available as a maven dependency via the [Github Package Registry](https://github.com/features/package-registry). You'll need to add the repository, usually in your top-level `build.gradle`:
```
allprojects {
    repositories {
        google()
        jcenter()

        maven {
            // Add Slyce-Android-SDK maven repo from Github Package Registry.
            url 'https://maven.pkg.github.com/Slyce-Inc/Slyce-Android-SDK'
            credentials {
                // EXAMPLE
                username = project.findProperty("gpr.user") ?: System.getenv("GPR_USER")
                password = project.findProperty("gpr.key") ?: System.getenv("GPR_API_KEY")
            }
        }
    }
}
```

A personal access token with read permissions on packages is required to download dependencies from the GPR. Please see the [documentation on gradle integration](https://help.github.com/en/github/managing-packages-with-github-package-registry/configuring-gradle-for-use-with-github-package-registry#authenticating-to-github-package-registry).

Once the maven repo is set up you may add the Slyce SDK as a normal dependency in your application's `build.gradle`:

```
// Standard Slyce SDK
implementation 'it.slyce:slycesdk:5.13.2'

// Lite Version
implementation 'it.slyce:slycesdk-lite:5.13.2'
```

---

Copyright Slyce, Inc 2014-2019
