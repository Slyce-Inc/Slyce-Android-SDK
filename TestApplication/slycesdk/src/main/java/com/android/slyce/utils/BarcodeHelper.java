package com.android.slyce.utils;

import com.android.slyce.enums.SlyceBarcodeType;
import com.android.slyce.SlyceBarcode;

/**
 * Created by davidsvilem on 5/6/15.
 */
public class BarcodeHelper {

    /* 3D barcode codes */
//    public static final int NONE        = 0;
//    public static final int PARTIAL     = 1;
//    public static final int EAN8        = 8;
//    public static final int UPCE        = 9;
//    public static final int ISBN10      = 10;
//    public static final int UPCA        = 12;
//    public static final int EAN13       = 13;
//    public static final int ISBN13      = 14;
//    public static final int I25         = 25;
//    public static final int DATABAR     = 34;
//    public static final int DATABAR_EXP = 35;
//    public static final int CODABAR     = 38;
//    public static final int CODE39      = 39;
//    public static final int PDF417      = 57;
//    public static final int QRCODE      = 64;
//    public static final int CODE93      = 93;
//    public static final int CODE128     = 128;

    /* 2D barcode codes */
//    public static final int NONE        = 0;
//    public static final int EAN8        = 1;
//    public static final int EAN13       = 2;
//    public static final int QRCODE      = 4;
//    public static final int DATAMATRIX  = 8;

    /* Slyce barcode types response */
    private static final String EAN_13 = "EAN_13";
    private static final String EAN_8 = "EAN_8";
    private static final String DATA_MATRIX = "DATA_MATRIX";
    private static final String QR_CODE = "QR_CODE";
    private static final String AZTEC = "AZTEC";
    private static final String CODABAR = "CODABAR";
    private static final String CODE_128 = "CODE_128";
    private static final String CODE_39 = "CODE_39";
    private static final String CODE_93 = "CODE_93";
    private static final String ITF = "ITF";
    private static final String MAXICODE = "MAXICODE";
    private static final String RSS_14 = "RSS_14";
    private static final String RSS_EXPANDED = "RSS_EXPANDED";
    private static final String UPC_E = "UPC_E";
    private static final String UPC_A = "UPC_A";
    private static final String UPC_EAN_EXTENSION = "UPC_EAN_EXTENSION";

    public enum ScannerType{
        _2D,
        _3D,
        _Slyce,
    }

    public static SlyceBarcode createSlyceBarcode(int detectionType, ScannerType scannerType, String barcodeValue){
        return createSlyceBarcode(String.valueOf(detectionType), scannerType, barcodeValue);
    }

    public static SlyceBarcode createSlyceBarcode(String detectionType, ScannerType scannerType, String barcodeValue){

        // Extract the SlyceBarcodeType from barcode scanner result
        SlyceBarcodeType barcodeType = parseBarcode(detectionType, scannerType);

        // Create SlyceBarcode object
        SlyceBarcode barcode = new SlyceBarcode(barcodeType, barcodeValue);

        return barcode;
    }

    /**
     * @param type: one of the numbers above returned by automatic scanners (2D/3D) or by Slyce response.
     * @param scannerType: type of scanner 2D/3D/Slyce {@link ScannerType}
     * @return Slyce type of barcode passed to host application {@link SlyceBarcodeType}
     */
    private static SlyceBarcodeType parseBarcode(String type, ScannerType scannerType){

        switch(type){

            case "0":
                return SlyceBarcodeType.SFBarcodeTypeNone;

            case "2":
            case "13":
            case EAN_13:
                return SlyceBarcodeType.SFBarcodeTypeEan13;

            case "4":
            case "64":
            case QR_CODE:
                return SlyceBarcodeType.SFBarcodeTypeQRCode;

            case "8":
            case EAN_8:

                if(scannerType == ScannerType._2D){

                    return SlyceBarcodeType.SFBarcodeTypeDataMatrix;

                }else if(scannerType == scannerType._3D){

                    return SlyceBarcodeType.SFBarcodeTypeEan8;

                }else if(scannerType == scannerType._Slyce){

                    return SlyceBarcodeType.SFBarcodeTypeEan8;
                }

            case "9":
            case UPC_E:
                return SlyceBarcodeType.SFBarcodeTypeUPCE;
            case "12":
            case UPC_A:
                return SlyceBarcodeType.SFBarcodeTypeUPCA;
            case "38":
            case CODABAR:
                return SlyceBarcodeType.SFBarcodeTypeCodabar;
            case "39":
            case CODE_39:
                return SlyceBarcodeType.SFBarcodeTypeCode39;
            case "57":
                return SlyceBarcodeType.SFBarcodeTypePDF417;
            case "93":
            case CODE_93:
                return SlyceBarcodeType.SFBarcodeTypeCode93;
            case "128":
            case CODE_128:
                return SlyceBarcodeType.SFBarcodeTypeCode128;
            case DATA_MATRIX:
                return SlyceBarcodeType.SFBarcodeTypeDataMatrix;
            case AZTEC:
                return SlyceBarcodeType.SFBarcodeTypeAztec;
            case ITF:
                return SlyceBarcodeType.SFBarcodeTypeITF;
            case MAXICODE:
                return SlyceBarcodeType.SFBarcodeTypeMaxiCode;
            case RSS_14:
                return SlyceBarcodeType.SFBarcodeTypeRSS14;
            case RSS_EXPANDED:
                return SlyceBarcodeType.SFBarcodeTypeRSSExpanded;
            case UPC_EAN_EXTENSION:
                return SlyceBarcodeType.SFBarcodeTypeUPCEANExtension;
        }

        return SlyceBarcodeType.SFBarcodeTypeNone;
    }

    private static SlyceBarcodeType parseBarcode(int type, ScannerType scannerType){
       return parseBarcode(String.valueOf(type), scannerType);
    }

}
