package com.lwkandroid.widget.ngv;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.DragStartHelper;
import androidx.core.view.OnReceiveContentListener;
import androidx.draganddrop.DropHelper;

import java.util.List;

/**
 * @description: 默认UI的适配器实现
 * @author:
 * @date: 2021/5/25 10:40
 */
public class DefaultNgvAdapter<D> extends AbsNgvAdapter<ImageView, NgvChildImageView, D>
{
    private INgvImageLoader<D> mImageLoader;
    private OnChildClickedListener<D> mListener;
    private Activity activity;

    public DefaultNgvAdapter(int maxDataSize, INgvImageLoader<D> mImageLoader, Activity activity)
    {
        super(maxDataSize);
        this.mImageLoader = mImageLoader;
        this.activity = activity;
    }

    public DefaultNgvAdapter(int maxDataSize, List<D> dataList, INgvImageLoader<D> mImageLoader)
    {
        super(maxDataSize, dataList);
        this.mImageLoader = mImageLoader;
    }

    @Override
    ImageView createPlusView(Context context)
    {
        return new ImageView(context);
    }

    @Override
    void bindPlusView(ImageView plusView, NgvAttrOptions attrOptions)
    {
        plusView.setImageDrawable(attrOptions.getIconPlusDrawable());
        plusView.setScaleType(ImageView.ScaleType.FIT_XY);
        plusView.setOnClickListener(v -> {
            if (mListener != null)
                mListener.onPlusImageClicked(plusView, getDValueToLimited());
        });
    }

    @Override
    NgvChildImageView createContentView(Context context)
    {
        return new NgvChildImageView(context);
    }


    @Override
    void bindContentView(NgvChildImageView childView, D data, int position, NgvAttrOptions attrOptions)
    {
        childView.getImageContent().setScaleType(attrOptions.getImageScaleType());
        childView.setDeleteImageSizeRatio(attrOptions.getIconDeleteSizeRatio());
        childView.setDeleteImageDrawable(attrOptions.getIconDeleteDrawable());
        childView.showDeleteImageView(attrOptions.isEnableEditMode());
        if (mImageLoader != null)
        {
            String path = mImageLoader.load(data);
            childView.setPath(path);
            mImageLoader.load(path, childView,
                    childView.getContentImageWidth(), childView.getContentImageHeight());
        }

        childView.getImageContent().setOnLongClickListener( v -> {

            // Create a new ClipData.
            // This is done in two steps to provide clarity. The convenience method
            // ClipData.newPlainText() can create a plain text ClipData in one step.

            // Create a new ClipData.Item from the ImageView object's tag.
            ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());

            // Create a new ClipData using the tag as a label, the plain text MIME type, and
            // the already-created item. This creates a new ClipDescription object within the
            // ClipData and sets its MIME type to "text/plain".
            ClipData dragData = new ClipData(
                    (CharSequence) v.getTag(),
                    new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN },
                    item);

            // Instantiate the drag shadow builder.
            View.DragShadowBuilder myShadow =  new View.DragShadowBuilder(v);

            // Start the drag.
            v.startDragAndDrop(dragData,  // The data to be dragged
                    myShadow,  // The drag shadow builder
                    null,      // No need to use local data
                    0          // Flags (not currently used, set to 0)
            );

            // Indicate that the long-click was handled.
            return true;
        });

        ImageView srcImageView =  childView.getImageContent();


        srcImageView.setHapticFeedbackEnabled(false);

        srcImageView.setTag(position+"");
// Enable the view to detect and start the drag event.
        new DragStartHelper(srcImageView, (view, helper) -> {

            ClipData.Item item = new ClipData.Item((CharSequence) childView.getPath());

            // Create a new ClipData using the tag as a label, the plain text MIME type, and
            // the already-created item. This creates a new ClipDescription object within the
            // ClipData and sets its MIME type to "text/plain".
            ClipData dragData = new ClipData(
                    (CharSequence) view.getTag(),
                    new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN },
                    item);

            // Must include DRAG_FLAG_GLOBAL to allow for dragging data between apps.
            // This example provides read-only access to the data.
            int flags = View.DRAG_FLAG_OPAQUE;
            return view.startDragAndDrop(dragData,
                    new View.DragShadowBuilder(view),
                    null,
                    flags);
        }).attach();

        DropHelper.configureView(
                activity,
                srcImageView,
                new String[] {"image/*"},
                new OnReceiveContentListener(){

                    @Nullable
                    @Override
                    public ContentInfoCompat onReceiveContent(@NonNull View target, @NonNull ContentInfoCompat source) {
                        String num = (String)target.getTag();
                        mListener.onContentImageClicked(Integer.parseInt(num), childView.getPath(), source, childView.getContentImageWidth(), childView.getContentImageHeight());
                        //
                        return null;
                    }


                });

    }

    public OnChildClickedListener<D> getOnChildClickedListener()
    {
        return mListener;
    }

    public void setOnChildClickListener(OnChildClickedListener<D> listener)
    {
        this.mListener = listener;
    }

    public interface OnChildClickedListener<D>
    {
        void onPlusImageClicked(ImageView plusImageView, int dValueToLimited);

        void onContentImageClicked(@NonNull int targetNum, String targetPath,  @NonNull ContentInfoCompat source, int width, int height);

        void onImageDeleted(int position, D data);
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {

        // The drag shadow image, defined as a drawable object.
        private static Drawable shadow;

        // Constructor
        public MyDragShadowBuilder(View v) {

            // Stores the View parameter.
            super(v);

            // Creates a draggable image that fills the Canvas provided by the system.
            shadow = new ColorDrawable(Color.LTGRAY);
        }

        // Defines a callback that sends the drag shadow dimensions and touch point
        // back to the system.
        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {

            // Defines local variables
            int width, height;

            // Set the width of the shadow to half the width of the original View.
            width = getView().getWidth() / 2;

            // Set the height of the shadow to half the height of the original View.
            height = getView().getHeight() / 2;

            // The drag shadow is a ColorDrawable. This sets its dimensions to be the
            // same as the Canvas that the system provides. As a result, the drag shadow
            // fills the Canvas.
            shadow.setBounds(0, 0, width, height);

            // Set the size parameter's width and height values. These get back to the
            // system through the size parameter.
            size.set(width, height);

            // Set the touch point's position to be in the middle of the drag shadow.
            touch.set(width / 2, height / 2);
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system
        // constructs from the dimensions passed to onProvideShadowMetrics().
        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draw the ColorDrawable on the Canvas passed in from the system.
            shadow.draw(canvas);
        }
    }
}


