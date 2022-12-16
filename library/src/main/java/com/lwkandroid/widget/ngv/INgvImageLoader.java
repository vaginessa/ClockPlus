package com.lwkandroid.widget.ngv;

import android.widget.ImageView;

/**
 * @description: 加载图片的接口
 * @author:
 * @date: 2021/5/26 15:46
 */
public interface INgvImageLoader<D>
{


    public String load(D source);
    public void load(String path, NgvChildImageView imageView, int width, int height);
}
