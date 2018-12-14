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

public class MainActivity extends AppCompatActivity
{

    private TextureView textureView;
    private Button analyzeButton;
    private Classifier classifier;
    private Camera2Manager camera2Manager;
    private TextView detectionTextView;
    private Context context;



    //TODO: Write Camera2Manager class
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = this.getApplicationContext();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if(getSupportActionBar() != null)
            getSupportActionBar().hide();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        final Activity activity = this;

        textureView =  findViewById(R.id.textureView);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener()
        {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
            {
                camera2Manager =Camera2Manager.instance(context, activity, textureView, width, height);
                camera2Manager.openCamera();

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

        detectionTextView = findViewById(R.id.textView);

        classifier = new Classifier(this.getApplicationContext());

        analyzeButton = findViewById(R.id.analyzeButton);
        analyzeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                detectionTextView.setText("Detected: " + classifier.detect(textureView.getBitmap(224,224)));
            }
        });
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
