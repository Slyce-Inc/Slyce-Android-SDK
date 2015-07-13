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
