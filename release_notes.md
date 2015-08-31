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
* Fix: Disable flash on front camera mode
* Add: `flipCamera()` method to `SlyceCamera`. Changing cemara preview from back to front and vise versa. 
* Add: `onCameraPreviewMode(boolean front)` to `OnSlyceCameraListener`. Will be invoke after calling `SlyceCamera.flipCamera()` indicating the camera preview mode. 
* Add: `SlyceCamera.turnFalsh()` will return a boolean indicating the falsh state (on/off)
* Fix: Crash on getProducts() with image url in public flow.
* Add: New design for SlyceCameraFragment
* Add: Bug fixing and impeovements. 