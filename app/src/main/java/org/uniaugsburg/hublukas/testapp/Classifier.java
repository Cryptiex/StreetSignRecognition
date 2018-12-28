package org.uniaugsburg.hublukas.testapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class Classifier {
    private static final int BATCH_SIZE = 1;
    public static final int IMG_HEIGHT = 64;
    public static final int IMG_WIDTH = 64;
    private static final int PIXEL_SIZE = 3;
    private static final int BYTES_PER_CHANNEL = 4; // 1 Byte = 8 Bit if quantisized, 4 Byte = 32 bit for float
    private static final int NUM_LABELS = 43;
    private static final String TFLITE_FILE = "GTSRB_model.tflite";
    private static final String LABEL_FILE = "GTSRB_labels.txt";


    private Interpreter interpreter;
    private Context context;
    private ByteBuffer inputImageBuffer;
    private float[][] output = null;
    private String[] labels;

    protected Classifier(Context context)
    {
        try
        {
            this.context = context;
            interpreter = new Interpreter(loadModelFile(TFLITE_FILE));

            inputImageBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * PIXEL_SIZE * BYTES_PER_CHANNEL);

            // Creates a buffer with the natural byte order (Little/Big Endian)
            inputImageBuffer.order(ByteOrder.nativeOrder());
            output = new float[1][NUM_LABELS];
            labels = new String[NUM_LABELS];
            loadLabels();
        }
        catch(IOException e)
        {
            Toast.makeText(context, "Cannot find model!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    private void convertBitmapToByteBuffer(Bitmap bitmap)
    {
        if(inputImageBuffer == null)
            return;

        int[] bitmapPixels = new int[IMG_WIDTH * IMG_HEIGHT];

        // Resets Buffer
        inputImageBuffer.rewind();
        bitmap.getPixels(bitmapPixels, 0, IMG_WIDTH, 0, 0, IMG_WIDTH, IMG_HEIGHT);

        int pixelCounter = 0;

        for(int i = 0; i < IMG_WIDTH; i++)
        {
            for(int j = 0; j < IMG_HEIGHT; j++)
            {
                // Red
                inputImageBuffer.putFloat(((bitmapPixels[pixelCounter] >> 16) & 0xFF));
                // Green
                inputImageBuffer.putFloat(((bitmapPixels[pixelCounter] >> 8) & 0xFF));
                // Blue
                inputImageBuffer.putFloat(bitmapPixels[pixelCounter] & 0xFF);

                //  inputImageBuffer.put((byte) (bitmapPixels[pixelCounter] & 0xFF)); for quantized

                pixelCounter++;
            }
        }

    }

    protected Prediction detect(Bitmap input)
    {


       convertBitmapToByteBuffer(input);
       interpreter.run(inputImageBuffer, output);


        //byte max = Byte.MIN_VALUE;
        float  max = 0.0f;
        int prediction = 0;

        for(int i = 0; i < output[0].length; i++)
        {
            // Only for quantized models
            /*if(Byte.compare(output[0][i], max) > 0)
            {
                max = output[0][i];
                prediction = i;
            }*/

            if(output[0][i] > max)
            {
                max = output[0][i];
                prediction = i;
            }

        }


        //return new Prediction(labels[prediction], max / 255.0f);
        return new Prediction(labels[prediction], max);

    }

    private MappedByteBuffer loadModelFile(String modelName) throws IOException
    {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    // TODO: Use array instead of ArrayList
    private void loadLabels() throws IOException
    {
        BufferedReader abc = new BufferedReader(new InputStreamReader(context.getAssets().open(LABEL_FILE)));

        String line;
        ArrayList<String> labels = new ArrayList<>();

        while((line = abc.readLine()) != null)
        {
            labels.add(line);
        }

        abc.close();
        this.labels = labels.toArray(new String[labels.size()]);
;
    }

    public void stop()
    {
        if(interpreter != null)
            interpreter.close();


        interpreter = null;
    }


}
