package com.stickercamera.app.camera.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.common.util.FileUtils;
import com.common.util.ImageUtils;
import com.common.util.StringUtils;
import com.common.util.TimeUtils;
import com.customview.LabelSelector;
import com.customview.LabelView;
import com.customview.MyHighlightView;
import com.customview.MyImageViewDrawableOverlay;
import com.face_detection.FaceView;
import com.github.skykai.stickercamera.R;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.stickercamera.App;
import com.stickercamera.AppConstants;
import com.face_detection.SafeFaceDetector;
import com.stickercamera.app.camera.CameraBaseActivity;
import com.stickercamera.app.camera.CameraManager;
import com.stickercamera.app.camera.EffectService;
import com.stickercamera.app.camera.adapter.FilterAdapter;
import com.stickercamera.app.camera.adapter.StickerToolAdapter;
import com.stickercamera.app.camera.effect.FilterEffect;
import com.stickercamera.app.camera.util.EffectUtil;
import com.stickercamera.app.camera.util.GPUImageFilterTools;
import com.stickercamera.app.model.Addon;
import com.stickercamera.app.model.FeedItem;
import com.stickercamera.app.model.TagItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import it.sephiroth.android.library.widget.HListView;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

/**
 * 图片处理界面
 * Created by sky on 2015/7/8.
 * Weibo: http://weibo.com/2030683111
 * Email: 1132234509@qq.com
 */
public class PhotoProcessActivity extends CameraBaseActivity {


    private static final String TAG = "PhotoViewerActivity";
    //Filter picture
    @InjectView(R.id.gpuimage)
    GPUImageView mGPUImageView;
    //Drawing area
    @InjectView(R.id.drawing_view_container)
    ViewGroup drawArea;
    //Bottom button
    //@InjectView(R.id.sticker_btn)
    //TextView stickerBtn;
   // @InjectView(R.id.filter_btn)
  //  TextView filterBtn;
  //  @InjectView(R.id.text_btn)
  //  TextView labelBtn;
    //Tool area
    @InjectView(R.id.list_tools)
    HListView bottomToolBar;
    @InjectView(R.id.toolbar_area)
    ViewGroup toolArea;
    private MyImageViewDrawableOverlay mImageView;
    private LabelSelector labelSelector;

    //The bottom button is currently selected
    private TextView currentBtn;
    //Current picture
    private Bitmap currentBitmap;
    //A small image for preview
    private Bitmap smallImageBackgroud;
    //White point label
    private LabelView emptyLabelView;

    private List<LabelView> labels = new ArrayList<LabelView>();
    ProgressDialog pd;
    //Label area
    private View commonLabelArea;

    /*@InjectView(R.id.faceView)
    FaceView faceView;
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_process);
        ButterKnife.inject(this);
        EffectUtil.clear();
        initView();
        initEvent();
        initStickerToolBar();

        ImageUtils.asyncLoadImage(this, getIntent().getData(), new ImageUtils.LoadImageCallback() {
            @Override
            public void callback(Bitmap result) {
                currentBitmap = result;
                mGPUImageView.setImage(currentBitmap);

                pd=new ProgressDialog(PhotoProcessActivity.this);
                pd.setTitle("Precessing Images");
                pd.setMessage("detecting faces");
                pd.show();

                detectFace(currentBitmap);
            }
        });

        ImageUtils.asyncLoadSmallImage(this, getIntent().getData(), new ImageUtils.LoadImageCallback() {
            @Override
            public void callback(Bitmap result) {
                smallImageBackgroud = result;
            }
        });

    }
    private void initView() {
        //Add a sticker to the watermark
        View overlay = LayoutInflater.from(PhotoProcessActivity.this).inflate(
                R.layout.view_drawable_overlay, null);
        mImageView = (MyImageViewDrawableOverlay) overlay.findViewById(R.id.drawable_overlay);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(App.getApp().getScreenWidth(),
                App.getApp().getScreenWidth());
        mImageView.setLayoutParams(params);
        overlay.setLayoutParams(params);
        drawArea.addView(overlay);
        //Add a tag selector
        RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(App.getApp().getScreenWidth(), App.getApp().getScreenWidth());
        labelSelector = new LabelSelector(this);
        labelSelector.setLayoutParams(rparams);
        drawArea.addView(labelSelector);
        labelSelector.hide();

        //Initialize the filter image
        mGPUImageView.setLayoutParams(rparams);


        //Initialize the blank label
        emptyLabelView = new LabelView(this);
        emptyLabelView.setEmpty();
        EffectUtil.addLabelEditable(mImageView, drawArea, emptyLabelView,
                mImageView.getWidth() / 2, mImageView.getWidth() / 2);
        emptyLabelView.setVisibility(View.INVISIBLE);

        //Initialize the recommended tab bar
        commonLabelArea = LayoutInflater.from(PhotoProcessActivity.this).inflate(
                R.layout.view_label_bottom,null);
        commonLabelArea.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        toolArea.addView(commonLabelArea);
        commonLabelArea.setVisibility(View.GONE);
    }

    private void initEvent() {
        /*stickerBtn.setOnClickListener(v ->{
            if (!setCurrentBtn(stickerBtn)) {
                return;
            }
            bottomToolBar.setVisibility(View.VISIBLE);
            labelSelector.hide();
            emptyLabelView.setVisibility(View.GONE);
            commonLabelArea.setVisibility(View.GONE);
            initStickerToolBar();
        });

        filterBtn.setOnClickListener(v -> {
            if (!setCurrentBtn(filterBtn)) {
                return;
            }
            bottomToolBar.setVisibility(View.VISIBLE);
            labelSelector.hide();
            emptyLabelView.setVisibility(View.INVISIBLE);
            commonLabelArea.setVisibility(View.GONE);
            initFilterToolBar();
        });
        labelBtn.setOnClickListener(v -> {
            if (!setCurrentBtn(labelBtn)) {
                return;
            }
            bottomToolBar.setVisibility(View.GONE);
            labelSelector.showToTop();
            commonLabelArea.setVisibility(View.VISIBLE);

        });
        labelSelector.setTxtClicked(v -> {
            EditTextActivity.openTextEdit(PhotoProcessActivity.this,"",8, AppConstants.ACTION_EDIT_LABEL);
        });
        labelSelector.setAddrClicked(v -> {
            EditTextActivity.openTextEdit(PhotoProcessActivity.this,"",8, AppConstants.ACTION_EDIT_LABEL_POI);

        });
        mImageView.setOnDrawableEventListener(wpEditListener);
        mImageView.setSingleTapListener(()->{
                emptyLabelView.updateLocation((int) mImageView.getmLastMotionScrollX(),
                        (int) mImageView.getmLastMotionScrollY());
                emptyLabelView.setVisibility(View.VISIBLE);

                labelSelector.showToTop();
                drawArea.postInvalidate();
        });
        labelSelector.setOnClickListener(v -> {
            labelSelector.hide();
            emptyLabelView.updateLocation((int) labelSelector.getmLastTouchX(),
                    (int) labelSelector.getmLastTouchY());
            emptyLabelView.setVisibility(View.VISIBLE);
        });*/


        titleBar.setRightBtnOnclickListener(v -> {
            savePicture();
        });
    }

    //Save the picture
    private void savePicture(){
        //Plus filter
        final Bitmap newBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newBitmap);
        RectF dst = new RectF(0, 0, mImageView.getWidth(), mImageView.getHeight());
        try {
            cv.drawBitmap(mGPUImageView.capture(), null, dst, null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            cv.drawBitmap(currentBitmap, null, dst, null);
        }
        //Add watermark
        EffectUtil.applyOnSave(cv, mImageView);

        new SavePicToFileTask().execute(newBitmap);
    }

    private class SavePicToFileTask extends AsyncTask<Bitmap,Void,String>{
        Bitmap bitmap;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("Image processing...");
        }

        @Override
        protected String doInBackground(Bitmap... params) {
            String fileName = null;
            try {
                bitmap = params[0];

                String picName = TimeUtils.dtFormat(new Date(), "yyyyMMddHHmmss");
                 fileName = ImageUtils.saveToFile(FileUtils.getInst().getPhotoSavedPath() + "/"+ picName, false, bitmap);

            } catch (Exception e) {
                e.printStackTrace();
                toast("Image processing error，Please exit the camera and try again", Toast.LENGTH_LONG);
            }
            return fileName;
        }

        @Override
        protected void onPostExecute(String fileName) {
            super.onPostExecute(fileName);
            dismissProgressDialog();
            if (StringUtils.isEmpty(fileName)) {
                return;
            }

            //Save the photo information to sharedPreference
            // Save the tag information
            List<TagItem> tagInfoList = new ArrayList<TagItem>();
            for (LabelView label : labels) {
                tagInfoList.add(label.getTagInfo());
            }

            //Send the image information to MainActivity via EventBus
            FeedItem feedItem = new FeedItem(tagInfoList,fileName);
            EventBus.getDefault().post(feedItem);
            CameraManager.getInst().close();
        }
    }


    public void tagClick(View v){
        TextView textView = (TextView)v;
        TagItem tagItem = new TagItem(AppConstants.POST_TYPE_TAG,textView.getText().toString());
        addLabel(tagItem);
    }

    private MyImageViewDrawableOverlay.OnDrawableEventListener wpEditListener   = new MyImageViewDrawableOverlay.OnDrawableEventListener() {
        @Override
        public void onMove(MyHighlightView view) {
        }

        @Override
        public void onFocusChange(MyHighlightView newFocus, MyHighlightView oldFocus) {
        }

        @Override
        public void onDown(MyHighlightView view) {

        }

        @Override
        public void onClick(MyHighlightView view) {
            labelSelector.hide();
        }

        @Override
        public void onClick(final LabelView label) {
            if (label.equals(emptyLabelView)) {
                return;
            }
            alert("Tips", "Whether you need to delete the label！", "determine", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EffectUtil.removeLabelEditable(mImageView, drawArea, label);
                    labels.remove(label);
                }
            }, "cancel", null);
        }
    };

    private boolean setCurrentBtn(TextView btn) {
        if (currentBtn == null) {
            currentBtn = btn;
        } else if (currentBtn.equals(btn)) {
            return false;
        } else {
            currentBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
        Drawable myImage = getResources().getDrawable(R.drawable.select_icon);
        btn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, myImage);
        currentBtn = btn;
        return true;
    }


    //Initialize the map
    private void initStickerToolBar(){

        bottomToolBar.setAdapter(new StickerToolAdapter(PhotoProcessActivity.this, EffectUtil.addonList));
        bottomToolBar.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> arg0,
                                    View arg1, int arg2, long arg3) {
                labelSelector.hide();
                Addon sticker = EffectUtil.addonList.get(arg2);
                EffectUtil.addStickerImage(mImageView, PhotoProcessActivity.this, sticker,
                        new EffectUtil.StickerCallback() {
                            @Override
                            public void onRemoveSticker(Addon sticker) {
                                labelSelector.hide();
                            }
                        });
            }
        });
       // setCurrentBtn(stickerBtn);
    }


    //Initialize the filter
    private void initFilterToolBar(){
        final List<FilterEffect> filters = EffectService.getInst().getLocalFilters();
        final FilterAdapter adapter = new FilterAdapter(PhotoProcessActivity.this, filters,smallImageBackgroud);
        bottomToolBar.setAdapter(adapter);
        bottomToolBar.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                labelSelector.hide();
                if (adapter.getSelectFilter() != arg2) {
                    adapter.setSelectFilter(arg2);
                    GPUImageFilter filter = GPUImageFilterTools.createFilterForType(
                            PhotoProcessActivity.this, filters.get(arg2).getType());
                    mGPUImageView.setFilter(filter);
                    GPUImageFilterTools.FilterAdjuster mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(filter);
                    //Adjustable color filters
                    if (mFilterAdjuster.canAdjust()) {
                        //mFilterAdjuster.adjust(100); Select an appropriate value for the adjustable filter
                    }
                }
            }
        });
    }

    //add tag
    private void addLabel(TagItem tagItem) {
        labelSelector.hide();
        emptyLabelView.setVisibility(View.INVISIBLE);
        if (labels.size() >= 5) {
            alert("Tips", "You can only add 5 tags！", "determine", null, null, null, true);
        } else {
            int left = emptyLabelView.getLeft();
            int top = emptyLabelView.getTop();
            if (labels.size() == 0 && left == 0 && top == 0) {
                left = mImageView.getWidth() / 2 - 10;
                top = mImageView.getWidth() / 2;
            }
            LabelView label = new LabelView(PhotoProcessActivity.this);
            label.init(tagItem);
            EffectUtil.addLabelEditable(mImageView, drawArea, label, left, top);
            labels.add(label);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        labelSelector.hide();
        super.onActivityResult(requestCode, resultCode, data);
        if (AppConstants.ACTION_EDIT_LABEL== requestCode && data != null) {
            String text = data.getStringExtra(AppConstants.PARAM_EDIT_TEXT);
            if(StringUtils.isNotEmpty(text)){
                TagItem tagItem = new TagItem(AppConstants.POST_TYPE_TAG,text);
                addLabel(tagItem);
            }
        }else if(AppConstants.ACTION_EDIT_LABEL_POI== requestCode && data != null){
            String text = data.getStringExtra(AppConstants.PARAM_EDIT_TEXT);
            if(StringUtils.isNotEmpty(text)){
                TagItem tagItem = new TagItem(AppConstants.POST_TYPE_POI,text);
                addLabel(tagItem);
            }
        }
    }


    public void detectFace(Bitmap bitmap){



        // A new face detector is created for detecting the face and its landmarks.
        //
        // Setting "tracking enabled" to false is recommended for detection with unrelated
        // individual images (as opposed to video or a series of consecutively captured still
        // images).  For detection on unrelated individual images, this will give a more accurate
        // result.  For detection on consecutive images (e.g., live video), tracking gives a more
        // accurate (and faster) result.
        //
        // By default, landmark detection is not enabled since it increases detection time.  We
        // enable it here in order to visualize detected landmarks.
        FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        // This is a temporary workaround for a bug in the face detector with respect to operating
        // on very small images.  This will be fixed in a future release.  But in the near term, use
        // of the SafeFaceDetector class will patch the issue.
        Detector<Face> safeDetector = new SafeFaceDetector(detector);

        // Create a frame from the bitmap and run face detection on the frame.
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = safeDetector.detect(frame);

        if (!safeDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        /*FaceView overlay = (FaceView) findViewById(R.id.faceView);
        overlay.setContent(bitmap, faces);*/


        drawFaceAnnotations(faces);






        // Although detector may be used multiple times for different images, it should be released
        // when it is no longer needed in order to free native resources.
        safeDetector.release();
    }


    private void drawFaceAnnotations(SparseArray<Face> faces) {

        /*Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);*/

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
               /* int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                canvas.drawCircle(cx, cy, 10, paint);*/
                Addon sticker = null;
               switch (landmark.getType())
               {
                   case 0://bottom mouth
                       sticker = EffectUtil.addonList.get(1);
                       break;

                   case 4://left eye
                       sticker = new Addon(R.drawable.eye);
                       break;

                   case 5://left mouth
                     //  sticker =new Addon(R.drawable.mark);
                       break;

                   case 6://nose base
                     //  sticker = new Addon(R.drawable.mark);
                       break;

                   case 10://right eye
                       sticker = new Addon(R.drawable.eye);
                       break;

                   case 11://right mouth
                       //sticker = new Addon(R.drawable.mark);
                    //   sticker = EffectUtil.addonList.get(4);

                       break;


                   default:
                      // sticker = new Addon(R.drawable.mark);
                       break;
               }

               if(sticker!=null) {
                   EffectUtil.addStickerImageAuto(mImageView, PhotoProcessActivity.this, sticker,
                           new EffectUtil.StickerCallback() {
                               @Override
                               public void onRemoveSticker(Addon sticker) {
                                   labelSelector.hide();
                               }
                           }, (int) landmark.getPosition().x, (int) landmark.getPosition().y);
               }
            }
        }

        pd.dismiss();

    }

}
