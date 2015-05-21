package com.android.slyce;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

import models.Product;

public class ProductsGridActivity extends Activity {

    public static final String PRODUCTS_KEY = "products_key";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<Product> products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        products = new ArrayList<Product>();
        String productsString = getIntent().getStringExtra(PRODUCTS_KEY);
        try {
            JSONArray productsJsonArray = new JSONArray(productsString);
            if (productsJsonArray != null) {

                Gson gson = new Gson();
                for (int i = 0; i < productsJsonArray.length(); i++) {

                    JSONObject productJson = productsJsonArray.optJSONObject(i);
                    products.add(gson.fromJson(productJson.toString(), Product.class));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e){

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
        mAdapter = new ProductsGridAdapter(products);
        mRecyclerView.setAdapter(mAdapter);
    }
}
