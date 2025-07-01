package org.rti.ttfinder.wrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import org.apache.commons.collections.functors.FalsePredicate;
import org.rti.ttfinder.models.ProcessedImageResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * This is a wrapper class for processing an image, and identifying if it has
 * TT or not.
 */

public class TTProcessor {

    private static final String TAG = "TTProcessor";
    // 2021.03.24.16.34.26
    private static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");


    /** Output parameters when all details of the run need to be recorded */
    private File outputDirectory = null;
    private boolean saveDetails;
    private long lastPipelineTimeCost = 0;
    private String lastClassificationResult = "";
    private boolean runFeatureExtractionAsStep;
    private boolean createDebugImages;
    private Bitmap lastVisualizedResult = null;

    public boolean isRunFeatureExtractionAsStep() {
        return runFeatureExtractionAsStep;
    }

    /** List of interpreters */
    TTClassifierWithFeaturesInterpreter classifyWithFeaturesInterpreter = null;
    TTClassifierInterpreter classifyInterpreter = null;
    TTSegmentInterpreter segmentInterpreter = null;
    TTFeatureExtractInterpreter featureExtractor = null;
    TTDetectionInterpreter detectionInterpreter = null;

    /**
     * Default constructor for the processor. Will not save details of the run to device storage.
     *
     * @param context
     * @throws IOException
     */
    public TTProcessor(Context context) throws IOException {
        this.outputDirectory = null;
        this.saveDetails = false;
        this.runFeatureExtractionAsStep = true;
        initializeInterpreters(context);
    }

    /**
     *
     * This will save out the details of the run to device stora
     *
     * @param outputDirectoryPath : Absolute path to the output directory. Directories for each
     *                            record identifier will be created, and details of the run written.
     * @param context :
     * @throws IOException
     */
    public TTProcessor(final File outputDirectoryPath,
                        Context context,
                       boolean runFeatureExtractionAsStep) throws IOException {
        if(outputDirectoryPath.toString().length() > 0){
            this.saveDetails = true;
            this.runFeatureExtractionAsStep = runFeatureExtractionAsStep;
            this.outputDirectory = outputDirectoryPath;
        }
        initializeInterpreters(context);
    }

    /**
     *
     * @param context
     * @return
     * @throws IOException
     */
    private boolean initializeInterpreters(Context context) throws IOException{
        Log.v(TAG, "***** SEGMENTATION SETUP **** ");
        segmentInterpreter = (TTSegmentInterpreter) TTInterpreter.create(context, TTInterpreter.Stage.SEGMENT, TTInterpreter.Device.CPU, 4, false);
        Log.v(TAG, "***** Detection SETUP **** ");
        detectionInterpreter = (TTDetectionInterpreter) TTInterpreter.create(context, TTInterpreter.Stage.DETECT,  TTInterpreter.Device.CPU, 4, true);

//        if (runFeatureExtractionAsStep == true){
//            Log.v(TAG, "***** FEATURE EXTRACTION SETUP **** ");
//             featureExtractor = (TTFeatureExtractInterpreter)  TTInterpreter.create(context,
//                                                                TTInterpreter.Stage.FEATURE_EXTRACT,
//                                                                TTInterpreter.Device.GPU,
//                                                                4, true);
//            Log.v(TAG, "***** CLASSIFIER SETUP **** ");
//            classifyInterpreter = (TTClassifierInterpreter) TTInterpreter.create(context,
//                    TTInterpreter.Stage.CLASSIFY,
//                    TTInterpreter.Device.CPU,
//                    4, true);
//        }
//        else {
//            featureExtractor = null;
//            classifyWithFeaturesInterpreter = (TTClassifierWithFeaturesInterpreter) TTInterpreter.create(context,
//                    TTInterpreter.Stage.EXTRACT_AND_CLASSIFY,
//                    TTInterpreter.Device.CPU,
//                    4, true);
//        }
        return true;
    }

    public void setCreateDebugImages(boolean createDebugImages) {
        this.createDebugImages = createDebugImages;
    }

    public File getOutputDirectoryPath() {
        return new File(outputDirectory.getAbsolutePath());
    }

    public static void saveImage(final Bitmap bitmap, final String filepath, boolean compress){
        try (FileOutputStream out = new FileOutputStream(filepath)) {
            if(compress)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            else
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance

        } catch (IOException e) {
            Log.e(TAG,"Error saving image: \n" + e);
        }
    }

    /**
     * Method to process and image and create prediction.
     *
     * @param inputImage : should be a square image. Otherwise error would be return
     * @param recordIdentifier : this would identify the image, and details will be written in a directory with this name
     * @return :  true if processing successful.
     * @throws SecurityException
     */
    public ProcessedImageResult processImage(final Bitmap inputImage, String recordIdentifier)
            throws SecurityException, IOException  {
        return this.processImage(inputImage, recordIdentifier, false);
    }

    /**
     * Method to process and image and create prediction.
     *
     * @param inputImage : should be a square image. Otherwise error would be return
     * @param recordIdentifier : this would identify the image, and details will be written in a directory with this name
     * @param runThroughSegmentation : Run only till segmentation step.
     * @return :  true if processing successful.
     * @throws SecurityException
     */
    public ProcessedImageResult processImage(final Bitmap inputImage, String recordIdentifier, boolean runThroughSegmentation)
            throws SecurityException, IOException  {

        String outputFileName = "";
        ProcessedImageResult result = new ProcessedImageResult();

        if (saveDetails &&
                (recordIdentifier.length() == 0 || outputDirectory == null)) {
            Log.e(TAG, "Need Record Identifier and Directory Paths");
            result.setSuccess(false);
            return result;
        }

        // Checks on Bitmap:
        if(inputImage.getHeight() != inputImage.getWidth()) {
            Log.e(TAG, "Input image should be a square. Returning");
            result.setSuccess(false);
            return result;
        }

        FileOutputStream outputStream = null;
        File recordDir = null;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (saveDetails) {
            // Check if there's a directory for the identifier
            if(this.createDebugImages) {
                recordDir = new File(outputDirectory, recordIdentifier);
            }
            else
                recordDir = outputDirectory;

            boolean outputDirExists = recordDir.exists();
            if (!outputDirExists) {
                outputDirExists = recordDir.mkdirs();
                if (!outputDirExists) {
                    Log.e(TAG, "Requested Output Directory not generated");
                }
            }
            if(outputDirExists) {
                outputFileName =  "log_" + recordIdentifier + "_" + sdf1.format(timestamp) + ".txt";
                try {
                    File file = new File(recordDir, outputFileName);
                    if (!file.exists())
                        file.createNewFile();
                    outputStream = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Did not find file to write log to. \n " + e);
                    outputStream = null;
                    recordDir = null;
                } catch (IOException e) {
                    Log.e(TAG, "IO Exception when creating the output log file \n" + e);
                    outputStream = null;
                    recordDir = null;
                }
            }
        }

        // Run the pipeline
        boolean success;
        long pipelineTimeCost = 0;

        //Save input
        if(recordDir != null && this.createDebugImages){
            String out_in_path = recordDir.getAbsolutePath() + File.separator + "Input_" +sdf1.format(timestamp) +".png";
            saveImage(inputImage, out_in_path, true);
        }

        if(outputStream != null) {
            outputStream.write( ("Processing Case :: " + recordIdentifier + "\n").getBytes() );
        }

        // SEGMENTATION
        if (this.createDebugImages)
            segmentInterpreter.setOutputDirectory(recordDir);
        else
            segmentInterpreter.setOutputDirectory(null);

        if (runThroughSegmentation){
            segmentInterpreter.setRunPostProcessing(false);
        }

        success = segmentInterpreter.runInference(inputImage);
        if(!success) {
            Log.e(TAG, "Error running segmentation, returning");
            long segmentationTimeCost = segmentInterpreter.getLastTmeCostForInference();
            pipelineTimeCost += segmentationTimeCost;
            lastPipelineTimeCost = pipelineTimeCost;
            if(outputStream != null) {
                outputStream.write(segmentInterpreter.getLastInferenceRunDetails().getBytes());
            }
            result.setSuccess(false);
            return result;
        }
        /* If this was the last step to be run, return */
        if(runThroughSegmentation){
            result.setSuccess(true);
            return result;
        }

        // If need to save details, write out the information here
        long segmentationTimeCost = segmentInterpreter.getLastTmeCostForInference();
        pipelineTimeCost += segmentationTimeCost;
        if(outputStream != null) {
            outputStream.write(segmentInterpreter.getLastInferenceRunDetails().getBytes());
        }
        Bitmap out_seg = segmentInterpreter.getSegmentation();
        if(recordDir != null && out_seg != null){
            String out_seg_path = recordDir.getAbsolutePath() + File.separator + "Segmentation_"+ sdf1.format(timestamp) + ".png";
            saveImage(out_seg, out_seg_path, false);
        }
        // if (out_seg != null){
        //     out_seg.recycle();
        // }

        // Get the main cropped region (rectangular crop)
        // Bitmap mainCrop = segmentInterpreter.getSegmentProcessor().getMainCrop();
        int pixel = out_seg.getPixel(out_seg.getWidth()/2, out_seg.getHeight()/2);
        Log.d(TAG, "Center pixel: " + Integer.toHexString(pixel));

        if (out_seg == null) {
            Log.e(TAG, "Failed to get main crop from segmentation");
            result.setSuccess(false);
            return result;
        }

        success = detectionInterpreter.runInference(out_seg);
        Log.v(TAG, "***** End  detection ******: ");
        if(!success) {
            Log.e(TAG, "Error running detection, returning");
        }
        // Get detections
        List<TTDetectionInterpreter.Detection> detections = detectionInterpreter.getDetections();
        Log.d(TAG, "Detections: " + detections);

        // Visualize detection results on the cropped image
        Bitmap visualizedCrop = visualizeDetections(out_seg, detections);
        lastVisualizedResult = visualizedCrop; // Store for UI access
        if (recordDir != null && visualizedCrop != null) {
            String visualized_path = recordDir.getAbsolutePath() + File.separator + "Detection_Results_" + sdf1.format(timestamp) + ".png";
            saveImage(visualizedCrop, visualized_path, false);
        }
//
//        // CLASSIFICATION and FEATURE EXTRACTION
//        if (runFeatureExtractionAsStep == true){
//            success = featureExtractor.runInference(segmentInterpreter.getSegmentProcessor());
//            if(outputStream != null) {
//                outputStream.write(featureExtractor.getLastInferenceRunDetails().getBytes());
//            }
//            pipelineTimeCost += featureExtractor.getLastTmeCostForInference();
//            if(!success) {
//                Log.e(TAG, "Error running feature extraction, returning");
//                result.setSuccess(false);
//                return result;
//            }
//
//            // CLASSIFICATION
//            ByteBuffer subimage_features = featureExtractor.getFeaturesBuffer();
//            subimage_features.rewind();
//            success = classifyInterpreter.runInference(subimage_features);
//            if(outputStream != null) {
//                outputStream.write(classifyInterpreter.getLastInferenceRunDetails().getBytes());
//            }
//            if(!success) {
//                Log.e(TAG, "Error running classification, returning");
//                result.setSuccess(false);
//                return result;
//            }
//            pipelineTimeCost += classifyInterpreter.getLastTmeCostForInference();
//
//            lastPipelineTimeCost = pipelineTimeCost;
//            List<TTClassifierInterpreter.Recognition> classification = classifyInterpreter.getTopKResults();
//            lastClassificationResult = "";
//            for(TTClassifierInterpreter.Recognition recognition : classification) {
//                lastClassificationResult += recognition + "\n";
//            }
//
//            segmentInterpreter.cleanup();

        // }
//         else {
//             success = classifyWithFeaturesInterpreter.runInference(segmentInterpreter.getSegmentProcessor());
//             if (outputStream != null) {
//                 outputStream.write(classifyWithFeaturesInterpreter.getLastInferenceRunDetails().getBytes());
//             }
//             if (!success) {
//                 Log.e(TAG, "Error running classification, returning");
//                 result.setSuccess(false);
//                 return result;
//             }
//             pipelineTimeCost += classifyWithFeaturesInterpreter.getLastTmeCostForInference();
// //            if (recordDir != null) {
// //                String out_img_path = recordDir.getAbsolutePath() + File.separator + "SegmentationAndImageSamples_" + sdf1.format(timestamp) + ".png";
// //                saveImage(out_seg, out_img_path, false);
// //            }

//             lastPipelineTimeCost = pipelineTimeCost;
//             List<TTClassifierWithFeaturesInterpreter.Recognition> classification = classifyWithFeaturesInterpreter.getTopKResults();
//             lastClassificationResult = "";
//             for (TTClassifierWithFeaturesInterpreter.Recognition recognition : classification) {
//                 lastClassificationResult += recognition + "\n";
//             }
//         }
//         if(outputStream != null) {
//             outputStream.write("CLASSIFICATION RESULTS\n".getBytes());
//             outputStream.write(lastClassificationResult.getBytes());
//             outputStream.write(("Time to run pipeline: " + lastPipelineTimeCost + "ms\n").getBytes());
//         }
        result.setSuccess(true);
        result.setLogFileName(outputFileName);
        return result;
    }

    public long getLastPipelineTimeCost() {
        return lastPipelineTimeCost;
    }

    public String getLastClassificationResult() {
        return lastClassificationResult;
    }

    public Bitmap getLastVisualizedResult() {
        return lastVisualizedResult;
    }

    private Bitmap visualizeDetections(Bitmap inputImage, List<TTDetectionInterpreter.Detection> detections) {
        if (inputImage == null || detections == null) {
            return inputImage;
        }
        
        // Create a mutable copy to draw on
        Bitmap outputBitmap = inputImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);

        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(32);
        textPaint.setStyle(Paint.Style.FILL);

        for (TTDetectionInterpreter.Detection detection : detections) {
            // Draw bounding box
            canvas.drawRect(detection.box, boxPaint);
            
            // Draw label and score
            String label = detection.label + String.format(" %.2f", detection.score);
            canvas.drawText(label, detection.box.left, detection.box.top - 10, textPaint);
        }

        return outputBitmap;
    }

}
