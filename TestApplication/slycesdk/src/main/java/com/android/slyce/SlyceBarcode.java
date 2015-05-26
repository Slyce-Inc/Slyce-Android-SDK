package com.android.slyce;

import com.android.slyce.enums.SlyceBarcodeType;

public class SlyceBarcode {

    /* Barcode result type */
    private SlyceBarcodeType type;

    /* Barcode result type as String */
    private String typeString;

    /* Scan result */
    private String barcode;

    public SlyceBarcode(SlyceBarcodeType type, String barcode){
        this.type = type;
        this.barcode = barcode;
        this.typeString = type.toString();
    }

    public SlyceBarcodeType getType() {
        return type;
    }

    public String getTypeString() {
        return typeString;
    }

    public String getBarcode() {
        return barcode;
    }
}
