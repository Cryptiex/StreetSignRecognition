package org.uniaugsburg.hublukas.testapp;

import android.graphics.Bitmap;

public interface Classifier {

    String TFLITE_FILE = "GTSRB_model.tflite";
    String LABEL_FILE = "GTSRB_labels.txt";
    int IMG_HEIGHT = 64;
    int IMG_WIDTH = 64;

    Prediction detect(Bitmap input);

    void stop();

}

