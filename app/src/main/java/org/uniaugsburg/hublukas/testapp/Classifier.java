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
    private static final int IMG_HEIGHT = 224;
    private static final int IMG_WIDTH = 224;
    private static final int PIXEL_SIZE = 3;
    private static final int BYTES_PER_CHANNEL = 1;
    private static final String LABEL_FILE = "labels.txt";


    private  Interpreter interpreter;
    private Context context;
    private ByteBuffer inputImageBuffer;
    private byte[][] output = null;
    private String[] labels;

    protected Classifier(Context context)
    {
        try
        {
            this.context = context;
            interpreter = new Interpreter(loadModelFile("mobilenet_quant_v1_224.tflite"));

            inputImageBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * PIXEL_SIZE * BYTES_PER_CHANNEL);

            // Creates a buffer with the natural byte order (Little/Big Endian)
            inputImageBuffer.order(ByteOrder.nativeOrder());
            output = new byte[1][1001];
            labels = new String[1001];
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
        bitmap.getPixels(bitmapPixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixelCounter = 0;

        for(int i = 0; i < IMG_WIDTH; i++)
        {
            for(int j = 0; j < IMG_HEIGHT; j++)
            {
                // Red
                inputImageBuffer.put((byte) ((bitmapPixels[pixelCounter] >> 16) & 0xFF));
                // Green
                inputImageBuffer.put((byte) ((bitmapPixels[pixelCounter] >> 8) & 0xFF));
                // Blue
                inputImageBuffer.put((byte) (bitmapPixels[pixelCounter] & 0xFF));

                pixelCounter++;
            }
        }
    }

    protected String detect(Bitmap input)
    {

        /*AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open("dog.bmp");
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }*/


        convertBitmapToByteBuffer(input);


       interpreter.run(inputImageBuffer, output);


        byte max = Byte.MIN_VALUE;
        int prediction = -1;

        for(int i = 0; i < output[0].length; i++)
        {
            if(Byte.compare(output[0][i], max) > 0)
            {
                max = output[0][i];
                prediction = i;
            }
        }


        return labels[prediction];

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
