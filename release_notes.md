Slyce Android SDK release notes
===============================

2.1 - 2015/07/13
------------------
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


2.2 - unknown release date 
--------------------------
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
* Add: Bug fixing and impeovements.
