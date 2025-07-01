package org.rti.ttfinder.wrapper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;


import org.rti.ttfinder.data.entity.Eye;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.util.Arrays;

public class TTFeatureExtractInterpreter extends TTInterpreter{

    private static final float IMAGE_MEAN = 127.0f;
    private static final float IMAGE_STD = 128.0f;

    private ByteBuffer allFeaturesBuffer;

    @Override
    protected String getModelPath() { return "features.tflite"; }

    /**
     * Run inference using input segnentation. It is assumed that the input bitmap is
     * aleady set, and so is the estimated number of pixels belonging to the eyelid region.
     * Need estimated number of pixels in eyelid region mostly for handling memory allaction,
     * but any other more efficient way can easily be used.
     *
     * @param b input image - this is not implemented here
     * @return boolean
     */
    @Override
    public boolean runInference(final Bitmap b ){
        Log.v(TAG, "Not implementing feature extraction from bitmap.");
        return true;
    }

    /**
     * Run inference using input segnentation. It is assumed that the input bitmap is
     * aleady set, and so is the estimated number of pixels belonging to the eyelid region.
     * Need estimated number of pixels in eyelid region mostly for handling memory allaction,
     * but any other more efficient way can easily be used.
     *
     * @param segmentProcessor: this is assumed to have the correct number of subimages for feature extraction
     * @return status of feature extraction as a boolean
     */
    public boolean runInference(final TTProcessSegmentation segmentProcessor) {

        lastInferenceTimeCost = 0;
        lastInferenceDetails.setLength(0);

        // Make sure segment processor has the right number of images:
        if (segmentProcessor == null || segmentProcessor.getNumSubImages()!=NUM_SUBIMAGES){
            Log.e(TAG, "Segment processor does not have the right number of subimages");
            return false;
        }

        Trace.beginSection("ExtractFeatures");

        if(allFeaturesBuffer == null) {
            int[] probabilityShape =
                    tflite.getOutputTensor(0).shape();
            DataType probabilityDataType = tflite.getOutputTensor(0).dataType();
            Log.v(TAG, "***** probability shape: " + probabilityShape[1] + ", " + probabilityShape[0]);
            allFeaturesBuffer = ByteBuffer.allocate(NUM_SUBIMAGES * probabilityShape[1] * probabilityDataType.byteSize());
        }
        else
            allFeaturesBuffer.rewind();
        Log.v(TAG, "Capacity of output buffer :: " + allFeaturesBuffer.capacity());
        updateLastInferenceRunDetails("Capacity of output buffer :: " + allFeaturesBuffer.capacity());
        updateLastInferenceRunDetails("Subimage edge length :: " + SUB_IMAGE_SIZE + "px");

        Trace.beginSection("runInference");
        long startTimeForFeatureExtraction = SystemClock.uptimeMillis();

        int size = SUB_IMAGE_SIZE*SUB_IMAGE_SIZE;
        int[] intValues = new int[size];
        float[] floatValues = new float[size*3];
        for(int subimageInd =0; subimageInd <NUM_SUBIMAGES; subimageInd ++) {
            Log.v(TAG, "SUBIMAGE: " + subimageInd);
            Bitmap subImage = segmentProcessor.getNthSubImage(subimageInd);
            if(subImage == null || subImage.getWidth() != SUB_IMAGE_SIZE || subImage.getHeight() != SUB_IMAGE_SIZE){
                Log.e(TAG, "Subimage at " + subimageInd  + " is null, or not correct size");
            }

            inputImageBuffer.load(subImage);
            TensorBuffer tb = inputImageBuffer.getTensorBuffer();
            //TensorBuffer tb = getPreprocessNormalizeOp().apply(inputImageBuffer.getTensorBuffer());
            Log.v(TAG, "Subimage shape is : ");
            for (int i=0; i<tb.getShape().length; i++){
                Log.v(TAG, ""+tb.getShape()[i]);
            }
            //inputImageBuffer.load(tb);

            Log.v(TAG, "SUBIMAGE: " + subimageInd + "Loaded");
            tflite.run(inputImageBuffer.getBuffer(), outputBuffer.getBuffer().rewind());
            Log.v(TAG, "SUBIMAGE: " + subimageInd + "Got features");
            ByteBuffer tempBB = outputBuffer.getBuffer();
            tempBB.rewind();

//            byte[] iarray = inputImageBuffer.getBuffer().array();
//            Log.v(TAG, "********  InputBuffer: ");
//            for (int j=0; j < iarray.length; j++){
//                Log.v(TAG, ""+j+" "+iarray[j]);
//            }
//            byte[] barray = tempBB.array();
//            Log.v(TAG, "*******  OutputBuffer: " );
//            for (int j=0; j < barray.length; j++) {
//                Log.v(TAG, ""+j+ " "+ barray[j]);
//            }
//            tempBB.rewind();

            Log.v(TAG, "SUBIMAGE: " + subimageInd + "Put in allfeaturebuffer" + outputBuffer.getBuffer().array().length);
            allFeaturesBuffer.put(tempBB);
            Log.v(TAG, "SUBIMAGE: " + subimageInd + "Put in allfeaturebuffer");
        }
        long endTimeForFeatureExtraction = SystemClock.uptimeMillis();
        Trace.endSection();
        logSectionTimeCost("runInference", (endTimeForFeatureExtraction - startTimeForFeatureExtraction));

        Trace.endSection();
        updateLastInferenceRunDetails("Time cost for feature extraction  :: " + lastInferenceTimeCost + "ms");

        return true;
    }

    /**
     * Initializes a {@code TTSegmentInterpreter}.
     *
     * @param activity
     */
    public TTFeatureExtractInterpreter(Context activity, Device device, int numThreads)
            throws IOException {
        super(activity, device, numThreads, true);
        allFeaturesBuffer = null;
        TAG ="TTFeatureExtractor";
    }

    /**
     *  Get the raw buffer with inferred features
     * @return
     */
    public ByteBuffer getFeaturesBuffer() {
        return allFeaturesBuffer;
    }
}
