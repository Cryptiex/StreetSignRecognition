package org.uniaugsburg.hublukas.testapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private Button analyzeButton;
    private Classifier classifier;
    private Camera camera2Manager;
    private TextView detectionTextView;
    private TextView fpsTextView;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getApplicationContext();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        final Activity activity = this;

        textureView = findViewById(R.id.textureView);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                camera2Manager = Camera2Manager.instance(context, activity, textureView, width, height);
                camera2Manager.openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        detectionTextView = findViewById(R.id.textView);
        fpsTextView = findViewById(R.id.FPS);

        classifier = new TFClassifier(this.getApplicationContext());

        analyzeButton = findViewById(R.id.analyzeButton);
        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(context, "Active", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        while (true) {
                            final long start = System.nanoTime();

                            final Prediction prediction = classifier.detect(textureView.getBitmap(Classifier.IMG_WIDTH, Classifier.IMG_WIDTH));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (prediction.getConfidence() > Prediction.THRESHOLD)
                                        detectionTextView.setText("Detected: " + prediction.getLabel() + ": " + prediction.getConfidence());
                                    int fps = (int) (1000 / ((System.nanoTime() - start) / 1000000.0));
                                    fpsTextView.setText("FPS: " + fps);
                                }
                            });
                        }
                    }
                }).start();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(camera2Manager != null)
            camera2Manager.openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(camera2Manager != null)
            camera2Manager.closeCamera();
    }
}
