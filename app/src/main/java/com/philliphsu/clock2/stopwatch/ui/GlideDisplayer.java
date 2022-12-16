package com.philliphsu.clock2.stopwatch.ui;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lwkandroid.imagepicker.data.ImageBean;
import com.lwkandroid.widget.ngv.INgvImageLoader;
import com.lwkandroid.widget.ngv.NgvChildImageView;

/**
 * @description:
 * @author:
 * @date: 2021/5/27 14:45
 */
class GlideDisplayer implements INgvImageLoader<ImageBean>
{
    @Override
    public String load(ImageBean source)
    {
        return source.getImagePath();
    }

    @Override
    public void load(String path, NgvChildImageView imageView1, int width, int height)
    {
        ImageView imageView = imageView1.getImageContent();
        imageView1.setPath(path);
        Glide.with(imageView.getContext())
                .load(path)
                .apply(new RequestOptions().override(width, height))
                .into(imageView);
    }
}
