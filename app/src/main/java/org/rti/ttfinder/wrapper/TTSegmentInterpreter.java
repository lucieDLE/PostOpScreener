package org.rti.ttfinder.wrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.torchvision.TensorImageUtils;
import org.pytorch.Tensor;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class TTSegmentInterpreter extends TTInterpreter{

    private int originalImageSizeX;
    private Bitmap outputBitmap;
    private int numEyelidPoints;
    private TTProcessSegmentation segmentProcessor;
    private File outputDirectory;

    private boolean runPostProcessing;

    @Override
    protected String getModelPath() {
        if(use_tflite) return "segmentation.tflite";
        else return "segmentation.ptl";
    }

    @Override
    public boolean runInference(Bitmap bitmap) {

        lastInferenceTimeCost = 0;
        lastInferenceDetails.setLength(0);
        outputBitmap = null;

        // Check if the image is square or not.
        if(bitmap.getHeight() != bitmap.getWidth()) {
            Log.e(TAG, "Input image should be a square. Returning");
            return false;
        }

        // Logs this method so that it can be analyzed with systrace.
        Trace.beginSection("segmentImage");

        Trace.beginSection("loadImage");
        long startTimeForLoadImage = SystemClock.uptimeMillis();
        if(use_tflite) {
            inputImageBuffer = loadImage(bitmap);
            Log.v(TAG, "Input image buffer size: " + inputImageBuffer.getTensorBuffer().getFlatSize());
        }
        else{
            inputImgTorch = loadTorchImage(bitmap);
            Log.v(TAG, "Input Torch image buffer size: " + (inputImgTorch.getDataAsFloatArray().length));
        }
        long endTimeForLoadImage = SystemClock.uptimeMillis();
        Trace.endSection();
        logSectionTimeCost("loadImage", (endTimeForLoadImage - startTimeForLoadImage));

        // Runs the inference call.

        if (use_tflite) {
            Log.v(TAG, "Output shape :: ");
            for (int i = 0; i < outputBuffer.getShape().length; i++)
                Log.v(TAG, outputBuffer.getShape()[i] + ", ");
            Log.v(TAG, "Output type :: " + outputBuffer.getDataType());
            Log.v(TAG, "Input image buffer size: " + inputImageBuffer.getTensorBuffer().getFlatSize());
            Log.v(TAG, "Output image buffer size: " + outputBuffer.getFlatSize());
        }
        Trace.beginSection("runInference");
        long startTimeForReference = SystemClock.uptimeMillis();
        if (use_tflite) {
            tflite.run(inputImageBuffer.getBuffer(), outputBuffer.getBuffer().rewind());
        }
        else {
            Log.v(TAG, "STarting Torch inference");
            outputImgTorch = torchlite.forward(IValue.from(inputImgTorch)).toTensor();
        }
        long endTimeForReference = SystemClock.uptimeMillis();
        Trace.endSection();
        logSectionTimeCost("runInference" , (endTimeForReference - startTimeForReference));

        boolean success = true;
        // Run post processing
        if (runPostProcessing == true) {
            Trace.beginSection("runPostProcessing");
            long startTimeForPostProcessing = SystemClock.uptimeMillis();
            success = postProcessOutput(bitmap);
            long endTimeForPostProcessing = SystemClock.uptimeMillis();
            Trace.endSection();
            logSectionTimeCost("runPostProcessing", (endTimeForPostProcessing - startTimeForPostProcessing));
            Trace.endSection();
        }

        updateLastInferenceRunDetails("Time cost for segmentation  :: " + lastInferenceTimeCost + "ms");
        return success;
    }

    /** Loads input image, and applies preprocessing. */
    private Tensor loadTorchImage(final Bitmap bitmap) {

        imageSizeY = 512;
        imageSizeX = 512;
        // Creates processor for the TensorImage.
        updateLastInferenceRunDetails("Input image shape :: " + bitmap.getWidth() + " " + bitmap.getHeight());
        updateLastInferenceRunDetails("Resize to :: " + imageSizeX + ", " + imageSizeY);

        Bitmap img_resized = Bitmap.createScaledBitmap(bitmap, imageSizeX, imageSizeY, true);
        // Following line puts the rgb image in 0-1 space. The values are set based on code here:
        // https://github.com/pytorch/pytorch/blob/master/android/pytorch_android_torchvision/src/main/java/org/pytorch/torchvision/TensorImageUtils.java#L88-L111

        // To normalize properly, we can uncomment the next two lines, and use pytorch defined mean/std. Explanation is here:
        // https://pytorch.org/mobile/android/
        Tensor tmpIm = TensorImageUtils.bitmapToFloat32Tensor(img_resized, new float[] {0,0,0}, new float[] {1,1,1});
                //TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                //TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        img_resized.recycle();
        Log.v(TAG, "Input image shape: " + bitmap.getWidth() + " " + bitmap.getHeight());
        Log.v(TAG, "Input image buffer type: " + tmpIm.dtype());
        Log.v(TAG, "Resizing to: " + imageSizeY + " " + imageSizeY);
        Log.v(TAG, "Input Torch Tensor shape: ");
        for (int i=0; i < tmpIm.shape().length; i++)
            Log.v(TAG,  tmpIm.shape()[i] + ", ");

        originalImageSizeX = bitmap.getWidth();
        return tmpIm;
    }

    /** Loads input image, and applies preprocessing. */
    private TensorImage loadImage(final Bitmap bitmap) {

        // Creates processor for the TensorImage.
        updateLastInferenceRunDetails("Input image shape :: " + bitmap.getWidth() + " " + bitmap.getHeight());
        updateLastInferenceRunDetails("Resize to :: " + imageSizeX + ", " + imageSizeY);
        Log.v(TAG, "Input image shape: " + bitmap.getWidth() + " " + bitmap.getHeight());
        Log.v(TAG, "Input image buffer type: " + inputImageBuffer.getDataType());

        TensorImage tmpIm = new TensorImage(inputImageBuffer.getDataType());
        tmpIm.load(bitmap);
        //Log.v(TAG, "Input image buffer size: " + inputImageBuffer.getTensorBuffer().getFlatSize());
        Log.v(TAG, "Resizing to: " + imageSizeY + " " + imageSizeY);
        originalImageSizeX = bitmap.getWidth();
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .build();
        return imageProcessor.process(tmpIm);
    }

    private boolean postProcessOutput(final Bitmap originalImage){

        if (use_tflite) {
            updateLastInferenceRunDetails("Output shape :: " + outputBuffer.getShape()[0] + ", " + outputBuffer.getShape()[1]);
            updateLastInferenceRunDetails("Output type :: " + outputBuffer.getDataType());
            updateLastInferenceRunDetails(" Flat size :: " + outputBuffer.getFlatSize());
            Log.v(TAG, "Output shape: ");
            for (int i=0; i < outputBuffer.getShape().length; i++)
                Log.v(TAG,  outputBuffer.getShape()[i] + ", ");
            Log.v(TAG, "Output type? : " + outputBuffer.getDataType() + " Flat size: " + outputBuffer.getFlatSize());
        }
        else{
            updateLastInferenceRunDetails("Output Torch shape :: " + outputImgTorch.shape()[0] + ", " + outputImgTorch.shape()[1]);
            updateLastInferenceRunDetails("Output Torch type :: " + outputImgTorch.dtype());
            updateLastInferenceRunDetails(" Flat Torch size :: " + outputImgTorch.getDataAsUnsignedByteArray().length);
            Log.v(TAG, "Output shape: ");
            for (int i=0; i < outputImgTorch.shape().length; i++)
                Log.v(TAG,  outputImgTorch.shape()[i] + ", ");
            Log.v(TAG, "Output type? : " + outputImgTorch.dtype() + " Flat size: " + outputImgTorch.getDataAsUnsignedByteArray().length);
        }

        // Reset segment processor
        segmentProcessor.reset(outputDirectory);
        // Ask segment processor to find eyelid points
        Bitmap segmentedImage;
        if (use_tflite) {
            segmentedImage = segmentProcessor.findEyelidPixels(inputImageBuffer.getBitmap(), outputBuffer);
        }
        else{
            Bitmap img_resized = Bitmap.createScaledBitmap(originalImage, imageSizeX, imageSizeY, true);
            outputBitmap = segmentProcessor.findEyelidPixels(img_resized, outputImgTorch);
        }
        // if(outputDirectory != null && segmentedImage != null){
        //     outputBitmap = Bitmap.createScaledBitmap(segmentedImage, originalImageSizeX, originalImageSizeX, false);
        // }
        // Does everything look okay? (number of eyelid points, etc)
        // If not, return false.
        if(!segmentProcessor.areNeededSegmentsPresent()) {
            for(String msg : segmentProcessor.logMessages ){
                updateLastInferenceRunDetails(msg);
            }
            updateLastInferenceRunDetails( " Did not find cornea or eyelid ");
            Log.e(TAG, "ERROR: did not find enough labels");

            return false;
        }

        int flatsize = 0;
        if (use_tflite) {
            flatsize = outputBuffer.getFlatSize();
        }
        else {
            flatsize = outputImgTorch.getDataAsUnsignedByteArray().length;
        }

        if (!segmentProcessor.areNumberOfPointsSufficient(flatsize)) {
            for (String msg : segmentProcessor.logMessages) {
                updateLastInferenceRunDetails(msg);
            }
            updateLastInferenceRunDetails(" Did not find enough eyelid pixels in segmentation ");
            Log.e(TAG, "ERROR: Not enough number of eyelid points detected");

            return false;
        }

        // ask segment processor to fit polynomial
        segmentProcessor.fitPolynomial((double)originalImageSizeX/(double)imageSizeX, (double)originalImageSizeX/(double)imageSizeY);

        // ask segment processor to create a stack of subimages.
        // segmentProcessor.extractSubImages(originalImage, outputBitmap /*Segmentation output if needed */);
        // for(String msg : segmentProcessor.logMessages ){
        //     updateLastInferenceRunDetails(msg);
        // }
        return true;
    }

    private boolean postProcessOutputOld(){
        updateLastInferenceRunDetails("Output shape :: " + outputBuffer.getShape()[0] + ", " + outputBuffer.getShape()[1]);
        updateLastInferenceRunDetails("Output type :: " + outputBuffer.getDataType());
        updateLastInferenceRunDetails( " Flat size :: " + outputBuffer.getFlatSize());

        Log.v(TAG, "Output shape: " + + outputBuffer.getShape()[0] + ", " + outputBuffer.getShape()[1]);
        Log.v(TAG, "Output type? : " + outputBuffer.getDataType() + " Flat size: " + outputBuffer.getFlatSize());

        ByteBuffer bf = outputBuffer.getBuffer();
        numEyelidPoints = 0;

        byte[] bytes = bf.array();
        int[] colors = new int[bytes.length];
        Set<String> labels = new HashSet<String>();

        for (int ind = 0; ind < outputBuffer.getFlatSize(); ind++) {
            int A = 255;
            int R = 0;
            int G = 0;
            int B = 0;
            if (bytes[ind] == 0) { // Assumed to be background
                A = 0;
                labels.add("Background");
            } else if (bytes[ind] == 1) { //Assumed to be cornea
                labels.add("Cornea");
                B = 255;
            } else if (bytes[ind] == 2) { //Assumed to be the pupil
                labels.add("Iris");
                G = 255;
            }
            if (bytes[ind] == 3) { //Assumed to be eyelid region
                labels.add("Eyelid");
                R = 255;
                numEyelidPoints += 1;
            }
            colors[ind] = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);
            if (A == 0) {
                // For background, use RGB from the original image
                int r = (int) Math.floor(ind / imageSizeX);
                int c = ind % imageSizeY;
                colors[ind] = inputImageBuffer.getBitmap().getPixel(c, r);
            }
        }
        updateLastInferenceRunDetails(" Labels in segmentation :: " + labels);
        double scale_factor = (originalImageSizeX / imageSizeY) * (originalImageSizeX / imageSizeX);
        updateLastInferenceRunDetails(" Scale factor :: " + scale_factor);
        updateLastInferenceRunDetails( " Number of eyelid points :: " + numEyelidPoints);

        if(!(labels.contains("Cornea") && labels.contains("Eyelid"))) {
            updateLastInferenceRunDetails( " Did not find cornea or eyelid ");
            Log.e(TAG, "ERROR: did not find enough labels");
            return false;
        }

        if(numEyelidPoints < outputBuffer.getFlatSize()*0.01) {
            updateLastInferenceRunDetails( " Did not find enough eyelid pixels ");
            Log.e(TAG, "ERROR: Not enough number of eyelid points detected");
            return false;
        }
        Bitmap seg = Bitmap.createBitmap(colors, imageSizeX, imageSizeY, Bitmap.Config.ARGB_8888);
        outputBitmap = Bitmap.createScaledBitmap(seg, originalImageSizeX, originalImageSizeX, false);
        numEyelidPoints *= (int) scale_factor;
        seg.recycle();
        return true;
    }

    public void cleanup(){
        if (runPostProcessing && segmentProcessor!= null){
            segmentProcessor.cleanup();
        }
    }

    /**
     * Initializes a {@code TTSegmentInterpreter}.
     *
     * @param activity
     */
    public TTSegmentInterpreter(Context activity, Device device, int numThreads, boolean use_tflite)
            throws IOException {
        super(activity, device, numThreads, use_tflite);
        numEyelidPoints = 0;
        outputBitmap = null;
        segmentProcessor = new TTProcessSegmentation(SUB_IMAGE_SIZE, NUM_SUBIMAGES);
        outputDirectory = null;
        runPostProcessing = true;
        TAG ="TTSegmentInterpreter";
    }

    public Bitmap getSegmentation(){
        return outputBitmap;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public TTProcessSegmentation getSegmentProcessor() {
        return segmentProcessor;
    }

    public void setRunPostProcessing(boolean runPostProcessing) {
        this.runPostProcessing = runPostProcessing;
    }
}
