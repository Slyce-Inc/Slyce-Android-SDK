package com.android.slyce;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;

import com.android.slyce.utils.SlyceLog;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

import interfaces.ProductInterface;
import models.Product2D;
import models.Product3D;

public class ProductsGridActivity extends Activity {

    public static final String PRODUCTS_KEY = "products_key";
    public static final String PRODUCTS_2D_KEY = "products_2d_key";
    public static final String PRODUCTS_3D_KEY = "products_3d_key";
    public static final String PRODUCTS_TYPE = "products_type";


    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<Product2D> _2DProducts;
    private ArrayList<Product3D> _3DProducts;

    private Product product;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        String productsString = getIntent().getStringExtra(PRODUCTS_KEY);
        String productsType = getIntent().getStringExtra(PRODUCTS_TYPE);

        if (TextUtils.equals(productsType, PRODUCTS_2D_KEY)) {

            product = new Product<Product2D>(Product2D.class, productsString);

        } else if (TextUtils.equals(productsType, PRODUCTS_3D_KEY)) {

            product = new Product<Product3D>(Product3D.class, productsString);

        } else {
            // Should not get here
        }

        setContentView(R.layout.activity_products_grid);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new ProductsGridAdapter(product.getProductsArray());
        mRecyclerView.setAdapter(mAdapter);
    }


    public class Product<T> {

        private final Class<T> clazz;
        private final String products;

        private ArrayList<Class<T>> productsArray = new ArrayList<Class<T> >();

        public Product(Class<T> clazz, String products){

            this.clazz = clazz;
            this.products = products;

            parseProducts();
        }

        private ArrayList<Class<T>> parseProducts(){

            try {
                JSONArray productsJsonArray = new JSONArray(products);
                if (productsJsonArray != null) {

                    Gson gson = new Gson();
                    for (int i = 0; i < productsJsonArray.length(); i++) {

                        JSONObject productJson = productsJsonArray.optJSONObject(i);

                        productsArray.add(gson.<Class<T>>fromJson(productJson.toString(), clazz));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.e("", "");
            }finally {
                return productsArray;
            }
        }

        public ArrayList<Class<T>> getProductsArray(){
            return productsArray;
        }
    }

}
