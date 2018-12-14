package org.uniaugsburg.hublukas.testapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;


public class Camera2Manager
{

    private static Camera2Manager camera2Manager;
    private CameraDevice currentCamera;
    private CameraCaptureSession captureSession;
    private TextureView textureView;
    private Context context;
    private Activity activity;
    private int width;
    private int height;

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

    private Camera2Manager(Context context, Activity activity, TextureView textureView, int width, int height)
    {
        this.context = context;
        this.activity = activity;
        this.textureView = textureView;
        this.width = width;
        this.height = height;
    }

    public static Camera2Manager instance (Context context, Activity activity, TextureView textureView, int width, int height)
    {
        if(camera2Manager == null)
            camera2Manager = new Camera2Manager(context, activity, textureView, width, height);

        return camera2Manager;
    }

    public void openCamera()
    {
        if(currentCamera != null)
            return;

        android.hardware.camera2.CameraManager cameraManager = context.getSystemService(android.hardware.camera2.CameraManager.class);

        try
        {
            String[] cameraIDList = cameraManager.getCameraIdList();

            // Ask for camera permission of not yet granted
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, 4);
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

        Toast.makeText(context.getApplicationContext(), "Opened camera", Toast.LENGTH_SHORT).show();
        Log.i("TAG", "Opened Camera");
    }

    private void setPreviewSize(CameraCharacteristics characteristics, int textureWidth, int textureHeight)
    {


        List<Size> bigEnough= new ArrayList<>();
        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Point displaySize =  new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        boolean swappedDimensions = false;

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

        Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);

        for(Size size : sizes)
        {
            // Only consider sizes with the right apsect ratiow
            if(size.getHeight() != size.getWidth() * displaySize.x / displaySize.y)
                continue;

            if(!swappedDimensions && size.getWidth() >= textureWidth && size.getHeight() >= textureHeight)
                bigEnough.add(size);

            else if(swappedDimensions && size.getHeight() >= textureWidth && size.getWidth() >= textureHeight)
                bigEnough.add(size);
        }

        if(bigEnough.isEmpty())
            return;

        Size finalSize = Collections.max(bigEnough, new Comparator<Size>()
        {
            @Override
            public int compare(Size o1, Size o2)
            {
                return Long.signum((long) o1.getWidth() * o1.getHeight() -
                        (long) o2.getWidth() * o2.getHeight());
            }
        });


        textureView.getSurfaceTexture().setDefaultBufferSize(finalSize.getWidth(), finalSize.getHeight());


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


}


