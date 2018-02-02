package com.hmj.nx6313.imgtoword;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int PHOTO_CAPTURE = 0x11;
    private static final int PHOTO_RESULT = 0x12;
    private static final int PHOTO_REQUEST_GALLERY = 0x13;

    private static String LANGUAGE = "eng";
    private static String IMG_PATH = getSDPath() + java.io.File.separator + "ocrtest";

    private static EditText tvResult;
    private static TextView tvResult1;
    private static ImageView ivSelected;
    private static ImageView ivTreated;
    private static Button btnCamera;
    private static Button btnSelect;
    private static Button btnCapy;
    private static CheckBox chPreTreat;
    private static RadioGroup radioGroup;

    private static String textResult;
    private static Bitmap bitmapSelected;
    private static Bitmap bitmapTreated;
    private static final int SHOWRESULT = 0x101;
    private static final int SHOWTREATEDIMG = 0x102;

    public static Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            tvResult.setText("");
            switch (msg.what) {
                case SHOWRESULT:
                    if (textResult.equals(""))
                        tvResult1.setText("● 结果为空");
                    else {
                        tvResult.setText(textResult);
                        tvResult1.setText("★ 结果转换成功：");
                    }
                    break;
                case SHOWTREATEDIMG:
                    tvResult1.setText("正在二值化处理......");
                    showPicture(ivTreated, bitmapTreated);
                    break;
            }
            super.handleMessage(msg);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 若文件夹不存在 首先创建文件夹
        File path = new File(IMG_PATH);
        if (!path.exists()) {
            path.mkdirs();
        }

        tvResult = findViewById(R.id.tv_result);
        tvResult1 = findViewById(R.id.tv_result1);
        ivSelected = findViewById(R.id.iv_selected);
        ivTreated = findViewById(R.id.iv_treated);
        btnCamera = findViewById(R.id.btn_camera);
        btnSelect = findViewById(R.id.btn_select);
        btnCapy = findViewById(R.id.btn_capy);
        chPreTreat = findViewById(R.id.ch_pretreat);
        radioGroup = findViewById(R.id.radiogroup);

        btnCamera.setOnClickListener(new cameraButtonListener());
        btnSelect.setOnClickListener(new selectButtonListener());
        btnCapy.setOnClickListener(new capyButtonListener());

        if (!isDirExist("tessdata")) {
            Toast.makeText(getApplicationContext(), "SD卡缺少语言包，复制中...", Toast.LENGTH_LONG).show();
            new SaveFile_Thread().start();
        }
        // 用于设置解析语言
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_en:
                        LANGUAGE = "eng";
                        break;
                    case R.id.rb_ch:
                        LANGUAGE = "chi_sim";
                        break;
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
//	        OnTouch = v.getId();
            if (isShouldHideInput(v, ev)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
            return super.dispatchTouchEvent(ev);
        }
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] leftTop = {0, 0};

            v.getLocationInWindow(leftTop);

            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED)
            return;

        if (requestCode == PHOTO_CAPTURE) {
            tvResult1.setText("abc");
            startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
        }

        if (requestCode == PHOTO_REQUEST_GALLERY) {
            startPhotoCrop(data.getData());
        }

        if (requestCode == PHOTO_RESULT) {
            bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(IMG_PATH,
                    "temp_cropped.jpg")));
            if (chPreTreat.isChecked())
                tvResult1.setText("11......");
            else
                tvResult1.setText("22......");
            showPicture(ivSelected, bitmapSelected);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (chPreTreat.isChecked()) {
                        bitmapTreated = ImgPretreatment
                                .doPretreatment(bitmapSelected);
                        Message msg = new Message();
                        msg.what = SHOWTREATEDIMG;
                        myHandler.sendMessage(msg);
                        textResult = doOcr(bitmapTreated, LANGUAGE);
                    } else {
                        bitmapTreated = ImgPretreatment
                                .converyToGrayImg(bitmapSelected);
                        Message msg = new Message();
                        msg.what = SHOWTREATEDIMG;
                        myHandler.sendMessage(msg);
                        textResult = doOcr(bitmapTreated, LANGUAGE);
                    }
                    Message msg2 = new Message();
                    msg2.what = SHOWRESULT;
                    myHandler.sendMessage(msg2);
                }

            }).start();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    class cameraButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
            startActivityForResult(intent, PHOTO_CAPTURE);
        }
    }

    class selectButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
//			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//			intent.addCategory(Intent.CATEGORY_OPENABLE);
//			intent.setType("image/*");
//			intent.putExtra("crop", "true");
//			intent.putExtra("scale", true);
//			intent.putExtra("return-data", false);
//			intent.putExtra(MediaStore.EXTRA_OUTPUT,
//					Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
//			intent.putExtra("outputFormat",
//					Bitmap.CompressFormat.JPEG.toString());
//			intent.putExtra("noFaceDetection", true); // no face detection
//			startActivityForResult(intent, PHOTO_RESULT);

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            boolean dele = delete(new File(IMG_PATH));
            startActivityForResult(intent, PHOTO_REQUEST_GALLERY);
        }

    }

    class capyButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (tvResult.length() == 0) {
                Toast.makeText(getApplicationContext(), "复制失败-1", Toast.LENGTH_SHORT).show();
                return;
            }
            cm.setText(tvResult.getText());
            Toast.makeText(getApplicationContext(), "复制成功-1", Toast.LENGTH_SHORT).show();
        }
    }


    public static void showPicture(ImageView iv, Bitmap bmp) {
        iv.setImageBitmap(bmp);
    }

    public String doOcr(Bitmap bitmap, String language) {
        TessBaseAPI baseApi = new TessBaseAPI();

        baseApi.init(getSDPath(), language);

        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        baseApi.setImage(bitmap);

        String text = baseApi.getUTF8Text();

        baseApi.clear();
        baseApi.end();

        return text;
    }

    public void startPhotoCrop(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, PHOTO_RESULT);
    }

    private Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver()
                    .openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    public static boolean delete(File file) {
//		File file = new File(path);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    delete(f);
                    //Log.d("fileName", f.getName());
                }
            }
            //file.delete();
        }
        return true;
    }

    public boolean isDirExist(String dir) {
        String SDCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;

        File file = new File(SDCardRoot + dir + File.separator);
        if (!file.exists())
            return false;
        else
            return true;
    }

    public boolean SaveFileToSDCard() {
        SDUtils sdutils_Chinese = new SDUtils("tessdata", "chi_sim.traineddata", this, R.raw.chi_sim);
        SDUtils sdutils_English = new SDUtils("tessdata", "eng.traineddata", this, R.raw.eng);
        try {
            sdutils_Chinese.getSQLiteDatabase();
            sdutils_English.getSQLiteDatabase();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public class SaveFile_Thread extends Thread {
        public SaveFile_Thread() {
        }

        public void run() {
            synchronized (this) {
                boolean iret;
                do {
                    iret = SaveFileToSDCard();
                } while (false);
                if (iret) {
                    ShowMsg(1);
                } else
                    ShowMsg(2);
            }
        }
    }

    public void ShowMsg(int what) {
        mLoadKeyHandler.sendEmptyMessage(what);
    }

    public Handler mLoadKeyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Toast.makeText(getApplicationContext(), "复制成功", Toast.LENGTH_LONG).show();
            } else if (msg.what == 2)
                Toast.makeText(getApplicationContext(), "复制失败", Toast.LENGTH_LONG).show();
        }
    };

    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        return sdDir.toString();
    }
}
