package models;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didiuzan on 5/20/15.
 */

public class Product {

    @Expose
    private String itemId;
    @Expose
    private Boolean loadSwatches;
    @Expose
    private String name;
    @Expose
    private String originalPrice;
    @Expose
    private String productDescription;
    @Expose
    private String productImageURL;
    @Expose
    private String productName;
    @Expose
    private String productPrice;
    @Expose
    private String productURL;
    @Expose
    private String rating;
    @Expose
    private String salePrice;
    @Expose
    private List<Swatch> swatches = new ArrayList<Swatch>();
    @Expose
    private String url;

    /**
     * @return The itemId
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @param itemId The itemId
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Product withItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    /**
     * @return The loadSwatches
     */
    public Boolean getLoadSwatches() {
        return loadSwatches;
    }

    /**
     * @param loadSwatches The loadSwatches
     */
    public void setLoadSwatches(Boolean loadSwatches) {
        this.loadSwatches = loadSwatches;
    }

    public Product withLoadSwatches(Boolean loadSwatches) {
        this.loadSwatches = loadSwatches;
        return this;
    }

    /**
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
    }

    public Product withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return The originalPrice
     */
    public String getOriginalPrice() {
        return originalPrice;
    }

    /**
     * @param originalPrice The originalPrice
     */
    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Product withOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
        return this;
    }

    /**
     * @return The productDescription
     */
    public String getProductDescription() {
        return productDescription;
    }

    /**
     * @param productDescription The productDescription
     */
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public Product withProductDescription(String productDescription) {
        this.productDescription = productDescription;
        return this;
    }

    /**
     * @return The productImageURL
     */
    public String getProductImageURL() {
        return productImageURL;
    }

    /**
     * @param productImageURL The productImageURL
     */
    public void setProductImageURL(String productImageURL) {
        this.productImageURL = productImageURL;
    }

    public Product withProductImageURL(String productImageURL) {
        this.productImageURL = productImageURL;
        return this;
    }

    /**
     * @return The productName
     */
    public String getProductName() {
        return productName;
    }

    /**
     * @param productName The productName
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Product withProductName(String productName) {
        this.productName = productName;
        return this;
    }

    /**
     * @return The productPrice
     */
    public String getProductPrice() {
        return productPrice;
    }

    /**
     * @param productPrice The productPrice
     */
    public void setProductPrice(String productPrice) {
        this.productPrice = productPrice;
    }

    public Product withProductPrice(String productPrice) {
        this.productPrice = productPrice;
        return this;
    }

    /**
     * @return The productURL
     */
    public String getProductURL() {
        return productURL;
    }

    /**
     * @param productURL The productURL
     */
    public void setProductURL(String productURL) {
        this.productURL = productURL;
    }

    public Product withProductURL(String productURL) {
        this.productURL = productURL;
        return this;
    }

    /**
     * @return The rating
     */
    public String getRating() {
        return rating;
    }

    /**
     * @param rating The rating
     */
    public void setRating(String rating) {
        this.rating = rating;
    }

    public Product withRating(String rating) {
        this.rating = rating;
        return this;
    }

    /**
     * @return The salePrice
     */
    public String getSalePrice() {
        return salePrice;
    }

    /**
     * @param salePrice The salePrice
     */
    public void setSalePrice(String salePrice) {
        this.salePrice = salePrice;
    }

    public Product withSalePrice(String salePrice) {
        this.salePrice = salePrice;
        return this;
    }

    /**
     * @return The swatches
     */
    public List<Swatch> getSwatches() {
        return swatches;
    }

    /**
     * @param swatches The swatches
     */
    public void setSwatches(List<Swatch> swatches) {
        this.swatches = swatches;
    }

    public Product withSwatches(List<Swatch> swatches) {
        this.swatches = swatches;
        return this;
    }

    /**
     * @return The url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public Product withUrl(String url) {
        this.url = url;
        return this;
    }

}

class Swatch {

    @Expose
    private String altText;
    @Expose
    private String swatchUrl;
    @Expose
    private String url;

    /**
     * @return The altText
     */
    public String getAltText() {
        return altText;
    }

    /**
     * @param altText The altText
     */
    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Swatch withAltText(String altText) {
        this.altText = altText;
        return this;
    }

    /**
     * @return The swatchUrl
     */
    public String getSwatchUrl() {
        return swatchUrl;
    }

    /**
     * @param swatchUrl The swatchUrl
     */
    public void setSwatchUrl(String swatchUrl) {
        this.swatchUrl = swatchUrl;
    }

    public Swatch withSwatchUrl(String swatchUrl) {
        this.swatchUrl = swatchUrl;
        return this;
    }

    /**
     * @return The url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public Swatch withUrl(String url) {
        this.url = url;
        return this;
    }

}

