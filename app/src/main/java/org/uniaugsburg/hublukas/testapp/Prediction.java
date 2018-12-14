package org.uniaugsburg.hublukas.testapp;

class Prediction
{


    private String label;
    private float confidence;

    public Prediction(String label, float confidence)
    {
        this.label = label;
        this.confidence = confidence;
    }

    public float getConfidence()
    {
        return confidence;
    }

    public String getLabel()
    {
        return label;
    }
}
