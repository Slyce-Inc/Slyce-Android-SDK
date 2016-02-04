Slyce Android SDK release notes//
===============================

2.1 - 2015/07/13
----------------
* Add: landscape support.
* Add: `SlyceCamera`: Scanning products/barcodesQR codes. Managing the camera and displaying its preview.
* Add: `SlyceCameraFragment`: Full UI Mode, Full UI implementation of SlyceCamera.
* Add: Support Public Products/Keywords requests.
* Update: `SlyceProductsRequest` changed to `SlyceRequest`.  
* Update: `OnSlyceRequestListener`: 
    * `on2DRecognition` changed to `onImageDetected`   
    * `on2DExtendedRecognition` changed to `onImageInfoReceived`
    * `on3DRecognition` changed to `onResultsReceived`
    * `onStageLevelFinish` changed to `onSlyceRequestStage`
* Add: `OnSlyceRequestListener`: 
    * `onBarcodeDetected` called when barcode is found.
    * `onItemDescriptionReceived` called when item description is found.
    * `onFinished` called when Slyce search proccess ended.


2.2 - 2015/10/27
----------------
* New: Full UI Mode - Total NEW design for `SlyceCameraFragment`.
* New: Full UI Mode - Use/Retake screen.
* New: Full UI Mode - Image crop functionality.  
* New: Public Flow API.
* New: UPC resolution - Get additional product\s after a barcode has been detected.
* New: `Slyce.open(appKey, appID, ...)` method for initialize public users.
* Added: `OnSlyceRequestListener.onBarcodeInfoReceived(...)` called when additional info for the previously recognized barcode is found.
* Added: `OnSlyceCameraListener.onCameraBarcodeInfoReceived(...)` same as above.
* Added: `OnSlyceCameraFragmentListener.onCameraFragmentBarcodeInfoReceived(...)` same as above.
* Added: `OnSlyceCameraListener.onCameraPreviewMode(boolean front)` will be invoke after calling     `SlyceCamera.flipCamera()` indicating the camera preview mode. 
* Added: `SlyceCamera.flipCamera()` changing cemara preview from back to front and vise versa. 
* Added: `SlyceCamera.turnFalsh()` will return a boolean indicating the falsh state (on/off).
* Fixed: Disable flash on front camera mode.
* Fixed: Crash on getProducts() with image url in public flow.
* Add: Bug fixing and improvements.


2.2.1 - 2015/10/29
------------------
* Fixed: `SlyceCamera.resume()` did not resume automatic barcode detection when `SlyceCamera.shouldPausedScan = true`
* Fixed: `Slyce.setVibrate(false)` did not disabled vibration.

2.2.2 - 2015/11/4
-----------------
* Fixed: During initial cache sync, the camera wasn't able to detect barcodes.

2.2.3 - 2015/11/8
-----------------
* Fixed: bugs related to 2D detection.

2.3 - 2015/12/20
-----------------
* ADDED: new 1D barcodes supported (ITF, code 128, code 39, codebar and more)

* FIXED: barcode detection in landscape orientation

* FIXED: local image cache synchronization bug

* IMPROVED: reduced SDK size

* IMPROVED: analytics reporting

2.3.1 - 2016/01/03
-----------------
* IMPROVED: UPC resolution reporting

* FIXED: Analitycs reporting suppressed when app is in background

2.4 - 2016/01/14
-----------------
* IMPROVED: SDK size and method count reduced significantly

* ADDED: `setShouldPauseScannerDelayTime(int delayTimeInMilliseconds)` - custom delay after scanning pause

* ADDED: `setContinuousRecognition2D(boolean value)`,`setContinuousRecognitionBarcodes(boolean value)` - enable and disable continuous recognition of barcodes/QR and Images separately

2.5 - 2016/02/03
-----------------
* ADDED: error callback during 2D scan when internet service is down.

* ADDED: Customization (Full UI mode) - override the default 'help' dialog fragment with your custom fragment.

* ADDED: Customization (Full UI mode) - override the default 'Not Found' dialog fragment with your custom fragment.

* ADDED: Customization (Full UI mode) - add your own custom button with a custom fragment to SlyceCameraFragment.

* ADDED: Customization (Full UI mode) - set your own circular progress color.

* ADDED: Choose your own set of barcode format to detect.
