package com.example.komori.opencv.main;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.example.komori.opencv.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Main2Activity extends Activity implements CameraBridgeViewBase.CvCameraViewListener{

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;

    public static int CAMERA_FRONT = 0;
    public static int CAMERA_BACK = 1;
    private  int camera_scene = CAMERA_BACK;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status){
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies(){
        try{
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File casadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(casadeDir,"lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1){
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("OpenCVActivity","Error Loading casade",e);
        }

        openCvCameraView.enableView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        final RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.relative);

        openCvCameraView = new JavaCameraView(this, CameraBridgeViewBase.CAMERA_ID_BACK);
        openCvCameraView.setCvCameraViewListener(this);


        final Button button = new Button(this);

        button.setText("切换摄像头");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if (camera_scene == CAMERA_FRONT) {
                    relativeLayout.removeAllViews();
                    openCvCameraView.disableView();
                    openCvCameraView = null;
                    cascadeClassifier = null;

                    openCvCameraView = new JavaCameraView(Main2Activity.this, CameraBridgeViewBase.CAMERA_ID_BACK);
                    openCvCameraView.setCvCameraViewListener(Main2Activity.this);
                    openCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);

                    camera_scene = CAMERA_BACK;

                    relativeLayout.addView(openCvCameraView);
                    relativeLayout.addView(button);

                    initializeOpenCVDependencies();
                } else {
                    relativeLayout.removeAllViews();
                    openCvCameraView.disableView();
                    openCvCameraView = null;
                    cascadeClassifier = null;

                    openCvCameraView = new JavaCameraView(Main2Activity.this, CameraBridgeViewBase.CAMERA_ID_FRONT);
                    openCvCameraView.setCvCameraViewListener(Main2Activity.this);
                    openCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);

                    camera_scene = CAMERA_FRONT;

                    relativeLayout.addView(openCvCameraView);
                    relativeLayout.addView(button);

                    initializeOpenCVDependencies();
                }
            }
        });

        relativeLayout.addView(openCvCameraView);
        relativeLayout.addView(button);

        if (camera_scene == CAMERA_FRONT) {
            openCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);

        } else if (camera_scene == CAMERA_BACK) {
            openCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);

        }


    }

    @Override
    public void onCameraViewStarted(int width, int height){
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        absoluteFaceSize = (int)(height * 0.2);

    }

    @Override
    public void onCameraViewStopped(){

    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame){

        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);


        //使前置的图像也是正的
        if (camera_scene == CAMERA_FRONT) {
            Core.flip(aInputFrame, aInputFrame, 1);
            Core.flip(grayscaleImage, grayscaleImage, 1);
        }


        MatOfRect faces = new MatOfRect();

        if (cascadeClassifier != null){
            cascadeClassifier.detectMultiScale(grayscaleImage,faces,1.1,2,2,new Size(absoluteFaceSize,absoluteFaceSize),new Size());
        }

        Rect[] faceArray = faces.toArray();
        for (int i = 0; i < faceArray.length; i++)

            Imgproc.rectangle(aInputFrame, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 1);

        return aInputFrame;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);

        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}

