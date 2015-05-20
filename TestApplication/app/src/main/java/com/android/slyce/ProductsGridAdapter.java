package com.android.slyce;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;

/**
 * Created by didiuzan on 5/20/15.
 */
public class ProductsGridAdapter extends RecyclerView.Adapter<ProductsGridAdapter.ViewHolder> {
        private ArrayList<Product> products;
        private ImageLoader imageLoader;
        private DisplayImageOptions options;


        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public ImageView mProductImageView;
            public TextView mProductPriceTv;
            public TextView mProductNameTv;
            public ViewHolder(View v) {
                super(v);
                mProductImageView = (ImageView) v.findViewById(R.id.product_image);
                mProductPriceTv = (TextView) v.findViewById(R.id.product_price);
                mProductNameTv = (TextView) v.findViewById(R.id.product_name);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public ProductsGridAdapter(ArrayList<Product> myDataset) {
            products = myDataset;
            imageLoader = ImageLoader.getInstance();

            options = new DisplayImageOptions.Builder().cacheInMemory(true)
                    .resetViewBeforeLoading(true).build();
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ProductsGridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_grid, parent, false);
            // set the view's size, margins, paddings and layout parameters
            //
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            imageLoader.displayImage(products.get(position).getProductImageURL(), holder.mProductImageView, options);
            holder.mProductPriceTv.setText(products.get(position).getProductPrice());
            holder.mProductNameTv.setText(products.get(position).getName());

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return products.size();
        }
    }

