package org.uniaugsburg.hublukas.testapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;

import androidx.annotation.NonNull;

public class MainActivity extends AppCompatActivity
{

    private TextureView textureView;
    private Button openCameraButton;
    private CameraDevice currentCamera;
    private CameraCaptureSession captureSession;


    private final CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback()
    {

        @Override
        public void onOpened(@androidx.annotation.NonNull CameraDevice camera)
        {
            currentCamera = camera;
            createCameraSession();
        }

        @Override
        public void onDisconnected(@androidx.annotation.NonNull CameraDevice camera)
        {
            camera.close();
            currentCamera  = null;
        }

        @Override
        public void onError(@androidx.annotation.NonNull CameraDevice camera, int error)
        {
            camera.close();
            currentCamera  = null;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        textureView =  findViewById(R.id.textureView);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener()
        {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
            {
                openCamera(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
            {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
            {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface)
            {}
        });


        openCameraButton = (Button) findViewById(R.id.openCameraButton);



    }

    public void openCamera(int width, int height)
    {
        if(currentCamera != null)
            return;

        CameraManager cameraManager = this.getSystemService(CameraManager.class);

        try
        {
            String[] cameraIDList = cameraManager.getCameraIdList();

            // Ask for camera permission of not yet granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 4);
            }

            for(String cameraID : cameraIDList)
            {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                int lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

                // Search for the front facing camera
                if(lensFacing == CameraCharacteristics.LENS_FACING_BACK)
                {
                    // Set preview size
                    StreamConfigurationMap configs = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    setPreviewSize(cameraCharacteristics, width, height);

                    cameraManager.openCamera(cameraID, cameraDeviceStateCallback, null);
                    break;
                }

            }

        }
        catch(CameraAccessException e)
        {
            e.printStackTrace();
            return;
        }

        Toast.makeText(getApplicationContext(), "Opened camera", Toast.LENGTH_SHORT).show();
        Log.i("TAG", "Opened Camera");
    }

    private void setPreviewSize(CameraCharacteristics characteristics, int textureWidth, int textureHeight)
    {
        //TODO: Set preview size dynamically

        int width, height;

        int displayRotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swappedDimensions;

        if(displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180)
        {
            if(sensorOrientation == 90 || sensorOrientation == 270)
                swappedDimensions = true;
        }
        else if(displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270)
        {
            if(sensorOrientation == 0 || sensorOrientation == 180)
                swappedDimensions = true;
        }


        textureView.getSurfaceTexture().setDefaultBufferSize(1920, 1080);


    }

    private void createCameraSession()
    {
        if(currentCamera == null)
            return;


        try
        {
            currentCamera.createCaptureSession(Arrays.asList(new Surface(textureView.getSurfaceTexture())), new CameraCaptureSession.StateCallback(){

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session)
                {
                    captureSession = session;
                    createCaptureRequest();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session)
                {

                }
            }, null);
        }
        catch(CameraAccessException e)
        {
            e.printStackTrace();
        }



        Log.i("TAG", "Created Session");
    }

    private void createCaptureRequest()
    {
        try
        {
            CaptureRequest.Builder request = currentCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            request.addTarget(new Surface(textureView.getSurfaceTexture()));
            captureSession.setRepeatingRequest(request.build(), new CameraCaptureSession.CaptureCallback()
            {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    // updated values can be found here
                }

            }, null);

        }
        catch(CameraAccessException e)
        {
            e.printStackTrace();
        }

        Log.i("TAG", "Created Capture Request");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // TODO: Implement onResume
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // TODO: Implement onResume
    }
}
