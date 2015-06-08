package models;

import com.google.gson.annotations.SerializedName;

import interfaces.ProductInterface;

public class Product2D implements ProductInterface{

    @SerializedName("urlWeb")
    private String urlWeb;

    @SerializedName("name")
    private String name;

    @SerializedName("price")
    private String price;

    @SerializedName("image")
    private String image;

    @Override
    public String productImageUr() {
        return image;
    }

    @Override
    public String productPrice() {
        return price;
    }

    @Override
    public String productWebUrl() {
        return urlWeb;
    }

    @Override
    public String productName() {
        return name;
    }
}
