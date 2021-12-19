/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.volley.RequestQueue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */

public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = false;
//  private static final String TF_OD_API_MODEL_FILE = "best_fp16.tflite";
  private static final String TF_OD_API_MODEL_FILE = "249260.tflite";
  private static final String TF_OD_API_LABELS_FILE = "label.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640,480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Detector detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  private RequestQueue RequestQueue;

  private GpsTracker gpsTracker;


  public void println(String data){
    System.out.println(data);
//    textView.append(data + "\n");
  }
  public void sendRequest(String result, String temp) {
//        String url = "http://3.38.115.198:8081/image";
//        String url = "http://3.38.115.198:8081";
    String url = "http://140.238.33.162:8080/register";
//    String url = "https://www.google.com";


    gpsTracker = new GpsTracker(DetectorActivity.this);

    double latitude = gpsTracker.getLatitude();
    double longitude = gpsTracker.getLongitude();

    println("위도 => " + latitude);
    println("경도 => " + longitude);
    //StringRequest 선언.
    //요청객체는 보내는방식(GET,POST), URL, 응답성공리스너, 응답실패리스너 이렇게 4개의 파라미터를 전달할 수 있다.
    //화면에 결과를 표시할때 핸들러를 사용하지 않아도 됨.

    StringRequest request = new StringRequest(
                Request.Method.POST,
//            Request.Method.GET,
            url,
            new Response.Listener<String>() { //응답을 문자열로 받아옴(응답을 성공적으로 받았을 때 자동으로 호출되는 메소드.)
              @Override
              public void onResponse(String response) {
                println("응답 => " + response); //코틀린 아님!! 메소드에 문자열을 파라메터로 전달해서 텍뷰에 띄움.
              }
            },
            new Response.ErrorListener(){ //에러발생시 호출될 리스너 객체
              @Override
              public void onErrorResponse(VolleyError error) {
                println("에러 => "+ error.getMessage()); //코틀린 아님!! 메소드에 문자열을 파라메터로 전달해서 텍뷰에 띄움.
              }
            }
    ){
      //만약 POST 방식에서 전달할 요청 파라미터가 있다면, getParams 메소드가 리턴하는 HashMap 객체에 넣어줄 것.
      //이렇게 만든 요청 객체(StringRequest request)를 요청 큐(requestQueue)에 넣어주기만 하면 됨.
      //POST방식을 이용할 때만 필요한 듯.
      @Override
      protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        final int tmp = 1;

        // kakaoLogin한 이메일주소를 받아서 서버에 userid로 전송
        Intent intent = getIntent();
        String strEmail = intent.getStringExtra("email");
//        params.put("num", "heo hyun jun babo post");
//        params.put("userid", "asdlkjw1차시도@nate.com");
        params.put("userid", strEmail);
        params.put("detect", result);
//        params.put("detect", String.format("%d", tmp));
        params.put("latitude", String.format("%.15f", latitude));
        params.put("longitude", String.format("%.15f", longitude));
        return params;
      }
    };

    //아래 add코드처럼 넣어줄때 Volley가 내부에서 캐싱을 해주기 때문에 한 번 보내고 받은 응답결과가 있으면
    //그 다음에 보냈을 떄 이전 것을 보여줄 수도 있다.
    //따라서 매번 받은 결과를 그대로 보여주기 위해 다음과 같이 setShouldCache를 false로 설정.
    //결과적으로 이전 결과가 있어도 새로 요청한 응답을 보여줌
    request.setShouldCache(false);
    RequestQueue.add(request);
    println("요청 보냄!!");
  }


////////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              this,
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing Detector!");
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight =size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
//    sensorOrientation = rotation - 90;

    LOGGER.i("aaaa %dx%d " , rotation , getScreenOrientation());
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
                sensorOrientation,MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
//    tracker.setFrameConfiguration(previewWidth, previewHeight, 0);
  }

  @Override
  protected void processImage() {
    //리퀘스트큐 생성. 이미지 생성할때 큐생성 해둠.
    if(RequestQueue == null) {
      RequestQueue = Volley.newRequestQueue(getApplicationContext());
    }
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    //이미지 불러오고 크롭이미지 생성?

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }


    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            int tmp = 1;
            // 결과값  Recognition 에서  받아오기 //
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            System.out.println("결과값 사이즈 : " + results.size());
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            }

            final List<Detector.Recognition> mappedRecognitions =
                new ArrayList<Detector.Recognition>();

            for (final Detector.Recognition result : results) {
              final RectF location = result.getLocation();

              System.out.println("location 1 : " + location);
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
//                System.out.println("set_location  : " + location);
                mappedRecognitions.add(result);

                String temp = "Empty";
                if (result.getTitle() != "Yes_Helmet") {
                  System.out.println("no__hel");
                  temp = BitmapToString(cropCopyBitmap);
                  System.out.println("temp : " + temp);
                };

                System.out.println("set_result : " + result);
                if (tmp == 1) {
                  tmp--;
//                  sendRequest();
                  sendRequest(result.getTitle(), temp);
                  //////////////////시간조절////////////////////////////////
//                  try {
//                    Thread.sleep(1000);
//                  }catch(InterruptedException e){
//                    System.out.println(e.getMessage()); //sleep 메소드가 발생하는 InterruptedException
//                  }
                  //////////////////////////////////////////////////
                }
//                System.out.println("set_result getLocation : " + result.getLocation());
//                System.out.println("set_result getClass : " + result.getClass());
//                System.out.println("set_result getId : " + result.getId());
//                System.out.println("set_result getConfidence : " + result.getConfidence());
//                System.out.println("set_result getTitle : " + result.getTitle());
              }
            }

            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

            runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {  // cropCopyBitmap 이 크롭된 인풋사진데이터
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                  }
                });
          }
        });
  }

  public static String BitmapToString(Bitmap bitmap) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 70, baos);
    byte[] bytes = baos.toByteArray();
    String temp = Base64.encodeToString(bytes, Base64.DEFAULT);
    return temp;
  }

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(
        () -> {
          try {
            detector.setUseNNAPI(isChecked);
          } catch (UnsupportedOperationException e) {
            LOGGER.e(e, "Failed to set \"Use NNAPI\".");
            runOnUiThread(
                () -> {
                  Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
          }
        });
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }



}


