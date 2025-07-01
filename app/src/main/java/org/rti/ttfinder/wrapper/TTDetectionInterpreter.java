package org.rti.ttfinder.wrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tensorflow.lite.Interpreter;
// import org.tensorflow.lite.flex.FlexDelegate;
import java.nio.MappedByteBuffer;

// public class TensorFlowLiteModelWithFlex {
    
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;

public class TTDetectionInterpreter extends TTInterpreter {
    private List<String> labels;
    private static final int NUM_PROPOSALS = 100;
    private static final float MIN_CONFIDENCE = 0.5f;
    private List<Detection> detections;
    private TensorImage inputImage;
    
    private Interpreter rpnInterpreter;
    private Interpreter roiInterpreter;
    // private FlexDelegate flexDelegate;
    
    @Override
    protected String getModelPath() {
        return "rpn_p1_float32.tflite"; // Not used, but required by base class
    }

    public static class Detection {
        public final RectF box;
        public final String label;
        public final float score;
        public Detection(RectF box, String label, float score) {
            this.box = box;
            this.label = label;
            this.score = score;
        }
        @Override
        public String toString() {
            return String.format("%s: %.2f [%.1f, %.1f, %.1f, %.1f]", label, score, box.left, box.top, box.right, box.bottom);
        }
    }


    private void checkBitmap(Bitmap bitmap){
        Log.d(TAG, "=== BITMAP AND TENSOR LOADING DEBUG ===");

        if (bitmap == null) {
            Log.e(TAG, "Bitmap is NULL!");
        }

        Log.d(TAG, "Bitmap info:");
        Log.d(TAG, "  Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        Log.d(TAG, "  Config: " + bitmap.getConfig());
        Log.d(TAG, "  Has alpha: " + bitmap.hasAlpha());
        Log.d(TAG, "  Is recycled: " + bitmap.isRecycled());
        Log.d(TAG, "  Byte count: " + bitmap.getByteCount());


        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
           bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }

        // Sample some pixel values from bitmap
        int[] pixels = new int[Math.min(10, bitmap.getWidth() * bitmap.getHeight())];
        if (bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0,
                    Math.min(bitmap.getWidth(), 10), 1);

            Log.d(TAG, "First 10 pixel values (ARGB):");
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                Log.d(TAG, String.format("  Pixel %d: ARGB(%d,%d,%d,%d) = 0x%08X",
                        i, a, r, g, b, pixel));
            }
        }

    }

//    public void drawImage(Bitmap bitmap){
//            // 1. Save the input image
//            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//            File appDir = new File(picturesDir, "TTDetection"); // Use your app's name
//            if (!appDir.exists()) appDir.mkdirs();
//
//            // 2. Save input image
//            File inputImageFile = new File(appDir, "input_image.png");
//            try (FileOutputStream out = new FileOutputStream(inputImageFile)) {
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//            }
//            // 2. Draw RPN boxes on a copy of the image
//    }



    public TTDetectionInterpreter(Context context, Device device, int numThreads) throws IOException {
        super(context, device, numThreads, true);
        // Load RPN and ROI models
        // flexDelegate = new FlexDelegate();
        Interpreter.Options roiOptions = new Interpreter.Options();
        roiOptions.setUseXNNPACK(true);
        roiOptions.setNumThreads(numThreads);

        // Load model
        MappedByteBuffer rpnModel = FileUtil.loadMappedFile(context, "rpn_p1_float32.tflite");

        // Create interpreter options with Flex delegate
        Interpreter.Options rpnOptions = new Interpreter.Options();
        // rpnOptions.addDelegate(flexDelegate);

        // Initialize interpreter with Flex delegate
        rpnInterpreter = new Interpreter(rpnModel, roiOptions);

        MappedByteBuffer roiModel = FileUtil.loadMappedFile(context, "roi_p2_float32.tflite");

        roiInterpreter = new Interpreter(roiModel);
        labels = new ArrayList<>();
        labels.add("Healthy");
        labels.add("Overcorrected");
        labels.add("Undercorrected");
        TAG = "TTDetectionInterpreter";
    }

    @Override
    public boolean runInference(Bitmap bitmap) {
        lastInferenceTimeCost = 0;
        lastInferenceDetails.setLength(0);
        detections = new ArrayList<>();

        Trace.beginSection("DetectionInput");
        long startTime = SystemClock.uptimeMillis();
        
        checkBitmap(bitmap);

        // Create TensorImage and load
        // TensorImage inputImage = new TensorImage(DataType.FLOAT32);
        // inputImage.load(bitmap);
        inputImage = createInputWithBatchDimension(bitmap);

        Log.d(TAG, "=== Detection started ===");
        
        // 1. Run RPN model to get proposals
        float[][] rpnBoxes = new float[NUM_PROPOSALS][4];
        Log.d(TAG, "rpn started");
        rpnInterpreter.run(inputImage.getBuffer(), rpnBoxes);

        Log.d(TAG, "Boxes array length: " + rpnBoxes.length);
        // Show first 10 values
        StringBuilder displaysmth = new StringBuilder("First 10 input values: ");
        for (int i = 0; i < Math.min(10, rpnBoxes.length); i++) {
            if (i > 0) displaysmth.append(", ");
            displaysmth.append(String.format("Box: [%.3f, %.3f, %.3f, %.3f]", rpnBoxes[i][0], rpnBoxes[i][1], rpnBoxes[i][2], rpnBoxes[i][3] ));

        }
        Log.d(TAG, displaysmth.toString());
        Log.d(TAG, "rpn ended, starting roi ");

        // debugModelStructure();


        // for (int i = 0; i < roiInterpreter.getInputTensorCount(); i++) {
        //     Tensor inputTensor = roiInterpreter.getInputTensor(i);
        //     int[] shape = inputTensor.shape();
        //     Log.d(TAG, String.format("Input %d: shape=%s, type=%s",
        //             i, Arrays.toString(shape), inputTensor.dataType()));
        // } //The needed shape for  inputs (1,666,1333,3), (100,4)

        // Log.d(TAG, "rpnBoxes[0].length (coordinates per box): " + rpnBoxes[0].length);
        // Log.d(TAG, "Full shape: [" + rpnBoxes.length + "][" + rpnBoxes[0].length + "]");
        // //The needed shape for  inputs (1,666,1333,3), (100,4)

        // 2. Run ROI model with image and proposals
        Object[] roiInputs = new Object[] { inputImage.getBuffer(), rpnBoxes };
        float[][] finalBoxes = new float[NUM_PROPOSALS][4];
        long[] finalLabels = new long[NUM_PROPOSALS];
        float[] finalScores = new float[NUM_PROPOSALS];
        Map<Integer, Object> roiOutputs = new HashMap<>();
        roiOutputs.put(0, finalBoxes);
        roiOutputs.put(1, finalLabels);
        roiOutputs.put(2, finalScores);
        roiInterpreter.runForMultipleInputsOutputs(roiInputs, roiOutputs);
        Log.d(TAG, "roi ended");
        Log.d(TAG, "Boxes array length: " + finalBoxes.length);

        displaysmth = new StringBuilder("First 10 input values: ");
        for (int i = 0; i < Math.min(10, finalBoxes.length); i++) {
            if (i > 0) displaysmth.append(", ");
            displaysmth.append(String.format("Box: [%.3f, %.3f, %.3f, %.3f]", finalBoxes[i][0], rpnBoxes[i][1], rpnBoxes[i][2], rpnBoxes[i][3] ));
        }
        Log.d(TAG, displaysmth.toString());

        long endTime = SystemClock.uptimeMillis();
        Trace.endSection();
        logSectionTimeCost("DetectionInput", (endTime - startTime));
        
        // processDetectionsWithActualShapes(roiOutputs);

        // 3. Parse detections and visualize
        // for (int i = 0; i < NUM_PROPOSALS; i++) {
        //     float score = finalScores[i];
        //     if (score < MIN_CONFIDENCE) continue;
        //     int labelIdx = (int) finalLabels[i];
        //     String label = (labelIdx >= 0 && labelIdx < labels.size()) ? labels.get(labelIdx) : "?";
        //     float[] boxArr = finalBoxes[i];
        //     // boxArr: [ymin, xmin, ymax, xmax] normalized
        //     RectF box = new RectF(
        //         boxArr[1] * bitmap.getWidth(),
        //         boxArr[0] * bitmap.getHeight(),
        //         boxArr[3] * bitmap.getWidth(),
        //         boxArr[2] * bitmap.getHeight()
        //     );
        //     detections.add(new Detection(box, label, score));
        // }
        // Log detections
        StringBuilder sb = new StringBuilder();
        for (Detection d : detections) {
            sb.append(d.toString()).append("\n");
        }
        updateLastInferenceRunDetails(sb.toString());
        return true;
    }

    public List<Detection> getDetections() {
        return detections;
    }

    // Method 1: Create buffers with actual output shapes and inspect them
    // private void debugModelOutputs() {
    //     int numOutputs = tflite.getOutputTensorCount();
    //     Log.d(TAG, "=== MODEL OUTPUT DEBUG ===");

    //     for (int i = 0; i < numOutputs; i++) {
    //         int[] outputShape = tflite.getOutputTensor(i).shape();
    //         DataType outputDataType = tflite.getOutputTensor(i).dataType();
    //         String outputName = tflite.getOutputTensor(i).name();

    //         Log.d(TAG, String.format("Output %d (%s): shape=%s, type=%s",
    //             i, outputName, Arrays.toString(outputShape), outputDataType));

    //         // Calculate total elements
    //         int totalElements = 1;
    //         for (int dim : outputShape) {
    //             totalElements *= dim;
    //         }
    //         Log.d(TAG, "Total elements in output " + i + ": " + totalElements);
    //     }
    // }

    // Method 2: Use ByteBuffer to capture raw outputs
    private void runInferenceWithRawOutputs(TensorImage inputImage) {
        // Create ByteBuffers for raw output capture
        int numOutputs = tflite.getOutputTensorCount();
        ByteBuffer[] rawOutputs = new ByteBuffer[numOutputs];

        for (int i = 0; i < numOutputs; i++) {
            int[] shape = tflite.getOutputTensor(i).shape();
            DataType dataType = tflite.getOutputTensor(i).dataType();

            // Calculate buffer size
            int totalElements = 1;
            for (int dim : shape) {
                totalElements *= 25;
            }

            int bytesPerElement = (dataType == DataType.FLOAT32) ? 4 : 1;
            int bufferSize = totalElements * bytesPerElement;

            rawOutputs[i] = ByteBuffer.allocateDirect(bufferSize);
            rawOutputs[i].order(ByteOrder.nativeOrder());

            Log.d(TAG, String.format("Created buffer %d: size=%d bytes, elements=%d",
                i, bufferSize, totalElements));
        }

        // Setup inputs and outputs for inference
        Object[] inputs = {inputImage.getBuffer()};
        java.util.Map<Integer, Object> outputs = new java.util.HashMap<>();

        for (int i = 0; i < numOutputs; i++) {
            outputs.put(i, rawOutputs[i]);
        }

        try {
            Log.d(TAG, "Running inference with raw outputs...");
            tflite.runForMultipleInputsOutputs(inputs, outputs);

            // Now inspect the raw outputs
            for (int i = 0; i < numOutputs; i++) {
                inspectRawOutput(i, rawOutputs[i], tflite.getOutputTensor(i));
            }

        } catch (Exception e) {
            Log.e(TAG, "Inference failed: " + e.getMessage(), e);
        }
    }

    private void inspectRawOutput(int outputIndex, ByteBuffer buffer, Tensor tensor) {
        buffer.rewind(); // Reset position to beginning

        int[] shape = tensor.shape();
        DataType dataType = tensor.dataType();
        String name = tensor.name();

        Log.d(TAG, String.format("=== OUTPUT %d (%s) INSPECTION ===", outputIndex, name));
        Log.d(TAG, "Shape: " + Arrays.toString(shape));
        Log.d(TAG, "Data type: " + dataType);
        Log.d(TAG, "Buffer capacity: " + buffer.capacity() + " bytes");

        if (dataType == DataType.FLOAT32) {
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            int numElements = Math.min(floatBuffer.capacity(), 20); // Show first 20 elements

            Log.d(TAG, "First " + numElements + " float values:");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numElements; i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format("%.6f", floatBuffer.get(i)));
            }
            Log.d(TAG, sb.toString());

            // Show statistics
            floatBuffer.rewind();
            float min = Float.MAX_VALUE, max = Float.MIN_VALUE, sum = 0;
            int totalElements = floatBuffer.capacity();

            for (int i = 0; i < totalElements; i++) {
                float val = floatBuffer.get(i);
                min = Math.min(min, val);
                max = Math.max(max, val);
                sum += val;
            }

            Log.d(TAG, String.format("Stats - Total: %d, Min: %.6f, Max: %.6f, Mean: %.6f",
                totalElements, min, max, sum / totalElements));
        }
    }

    // Method 3: Alternative approach using TensorBuffer with actual shapes
    private void runInferenceWithCorrectBuffers(TensorImage inputImage) {
        try {
            // Don't assume shapes - use actual shapes from the model
            TensorBuffer[] outputBuffers = new TensorBuffer[3];

            for (int i = 0; i < 3; i++) {
                int[] actualShape = tflite.getOutputTensor(i).shape();
                DataType actualDataType = tflite.getOutputTensor(i).dataType();

                Log.d(TAG, String.format("Creating buffer %d with actual shape: %s",
                    i, Arrays.toString(actualShape)));

                outputBuffers[i] = TensorBuffer.createFixedSize(actualShape, actualDataType);
            }

            // Setup inference
            Object[] inputs = {inputImage.getBuffer()};
            java.util.Map<Integer, Object> outputs = new java.util.HashMap<>();
            outputs.put(0, outputBuffers[0].getBuffer());   // boxes
            outputs.put(1, outputBuffers[1].getBuffer());   // classes/labels
            outputs.put(2, outputBuffers[2].getBuffer());   // scores

            Log.d(TAG, "Running inference with correct buffer shapes...");
            tflite.runForMultipleInputsOutputs(inputs, outputs);

            // Now inspect the actual outputs
            for (int i = 0; i < 3; i++) {
                float[] outputArray = outputBuffers[i].getFloatArray();
                Log.d(TAG, String.format("Output %d array length: %d", i, outputArray.length));

                // Print first few values
                int numToPrint = Math.min(10, outputArray.length);
                StringBuilder sb = new StringBuilder();
                sb.append("First ").append(numToPrint).append(" values: ");
                for (int j = 0; j < numToPrint; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(String.format("%.4f", outputArray[j]));
                }
                Log.d(TAG, sb.toString());
            }

            // Process detections with actual output shapes
            processDetectionsWithActualShapes(outputBuffers);

        } catch (Exception e) {
            Log.e(TAG, "Inference with correct buffers failed: " + e.getMessage(), e);
        }
    }

    private void processDetectionsWithActualShapes(TensorBuffer[] outputBuffers) {
        float[] boxesArray = outputBuffers[0].getFloatArray();
        float[] labelsArray = outputBuffers[1].getFloatArray();
        float[] scoresArray = outputBuffers[2].getFloatArray();

        Log.d(TAG, "=== PROCESSING DETECTIONS ===");
        Log.d(TAG, "Boxes array length: " + boxesArray.length);
        Log.d(TAG, "Labels array length: " + labelsArray.length);
        Log.d(TAG, "Scores array length: " + scoresArray.length);

        // Determine number of detections based on actual output shapes
        int numDetections = Math.min(labelsArray.length, scoresArray.length);
        Log.d(TAG, "Number of detections: " + numDetections);

        // Check if boxes array makes sense
        if (boxesArray.length != numDetections * 4) {
            Log.w(TAG, String.format("Box array length (%d) doesn't match expected (%d * 4 = %d)",
                    boxesArray.length, numDetections, numDetections * 4));
        }

        // Process detections
        for (int i = 0; i < numDetections && i < boxesArray.length / 4; i++) {
            float score = scoresArray[i];
            if (score < MIN_CONFIDENCE) continue;

            int labelIdx = (int) labelsArray[i];
            String label = (labelIdx >= 0 && labelIdx < labels.size()) ? labels.get(labelIdx) : "Unknown";

            // Extract box coordinates
            float y1 = boxesArray[i * 4];
            float x1 = boxesArray[i * 4 + 1];
            float y2 = boxesArray[i * 4 + 2];
            float x2 = boxesArray[i * 4 + 3];

            Log.d(TAG, String.format("Detection %d: %s (%.3f) - Box: [%.3f, %.3f, %.3f, %.3f]",
                    i, label, score, x1, y1, x2, y2));
        }
    }

    //  Debug input tensor thoroughly
    private void debugInputTensor(TensorImage inputImage, Bitmap originalBitmap) {
        Log.d(TAG, "=== INPUT TENSOR DEBUG ===");

        // Check input tensor specifications
        Tensor inputTensor = tflite.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        DataType inputDataType = inputTensor.dataType();
        String inputName = inputTensor.name();

        Log.d(TAG, "Expected input shape: " + Arrays.toString(inputShape));
        Log.d(TAG, "Expected input type: " + inputDataType);
        Log.d(TAG, "Input tensor name: " + inputName);

        // Check actual input image details
        TensorBuffer tensorBuffer = inputImage.getTensorBuffer();
        int[] actualShape = tensorBuffer.getShape();
        DataType actualType = tensorBuffer.getDataType();

        Log.d(TAG, "Actual input shape: " + Arrays.toString(actualShape));
        Log.d(TAG, "Actual input type: " + actualType);
        Log.d(TAG, "Original bitmap size: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

        // Check if shapes match
        boolean shapesMatch = Arrays.equals(inputShape, actualShape);
        Log.d(TAG, "Input shapes match: " + shapesMatch);

        if (!shapesMatch) {
            Log.e(TAG, "INPUT SHAPE MISMATCH DETECTED!");
            Log.e(TAG, "Expected: " + Arrays.toString(inputShape));
            Log.e(TAG, "Actual: " + Arrays.toString(actualShape));
        }

        // Sample some input values
        float[] inputArray = tensorBuffer.getFloatArray();
        Log.d(TAG, "Input array length: " + inputArray.length);

        if (inputArray.length > 0) {
            // Show first 10 values
            StringBuilder sb = new StringBuilder("First 10 input values: ");
            for (int i = 0; i < Math.min(10, inputArray.length); i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format("%.3f", inputArray[i]));
            }
            Log.d(TAG, sb.toString());

            // Check value range
            float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
            for (float val : inputArray) {
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
            Log.d(TAG, String.format("Input value range: [%.3f, %.3f]", min, max));
        }
    }


    private void testSingleInference(TensorImage inputImage) {
        try {
            // Create minimal outputs for testing
            TensorBuffer boxesBuffer = TensorBuffer.createFixedSize(
                tflite.getOutputTensor(0).shape(),
                tflite.getOutputTensor(0).dataType()
            );
           TensorBuffer labelsBuffer = TensorBuffer.createFixedSize(
               tflite.getOutputTensor(1).shape(),
               tflite.getOutputTensor(1).dataType()
           );
           TensorBuffer scoresBuffer = TensorBuffer.createFixedSize(
               tflite.getOutputTensor(2).shape(),
               tflite.getOutputTensor(2).dataType()
           );

            Object[] inputs = {inputImage.getBuffer()};
            java.util.Map<Integer, Object> outputs = new java.util.HashMap<>();
            outputs.put(0, boxesBuffer.getBuffer());
           outputs.put(1, labelsBuffer.getBuffer());
           outputs.put(2, scoresBuffer.getBuffer());

            Log.d(TAG, ": Running inference...");
            tflite.runForMultipleInputsOutputs(inputs, outputs);
            Log.d(TAG, ": SUCCESS! Inference completed without errors.");

            // Print results
            float[] boxes = boxesBuffer.getFloatArray();
           float[] labels = labelsBuffer.getFloatArray();
           float[] scores = scoresBuffer.getFloatArray();

            Log.d(TAG, String.format("%s Results - Boxes: %d,",
              boxes.length));

            if (boxes.length > 0) {
                Log.d(TAG, String.format("%s Box: [%.3f, %.3f, %.3f, %.3f]",
                  boxes[0], boxes[1], boxes[2], boxes[3]));
            }
           if (labels.length > 0) {
               Log.d(TAG, String.format("%s Label: %.0f", labels[0]));
           }
           if (scores.length > 0) {
               Log.d(TAG, String.format("%s Score: %.3f", scores[0]));
           }

        } catch (Exception e) {
            Log.e(TAG,  " inference failed: " + e.getMessage());
        }
    }


    // Add this method to debug your models thoroughly
    private void debugModelStructure() {
        Log.d(TAG, "=== RPN MODEL DEBUG ===");
        Log.d(TAG, "RPN Input tensors: " + rpnInterpreter.getInputTensorCount());
        Log.d(TAG, "RPN Output tensors: " + rpnInterpreter.getOutputTensorCount());
        
        for (int i = 0; i < rpnInterpreter.getInputTensorCount(); i++) {
            Tensor tensor = rpnInterpreter.getInputTensor(i);
            Log.d(TAG, String.format("RPN Input %d: %s, shape=%s, quantized=%s", 
                i, tensor.dataType(), Arrays.toString(tensor.shape()), tensor.quantizationParams()));
        }
        
        for (int i = 0; i < rpnInterpreter.getOutputTensorCount(); i++) {
            Tensor tensor = rpnInterpreter.getOutputTensor(i);
            Log.d(TAG, String.format("RPN Output %d: %s, shape=%s", 
                i, tensor.dataType(), Arrays.toString(tensor.shape())));
        }
        
        Log.d(TAG, "=== ROI MODEL DEBUG ===");
        Log.d(TAG, "ROI Input tensors: " + roiInterpreter.getInputTensorCount());
        Log.d(TAG, "ROI Output tensors: " + roiInterpreter.getOutputTensorCount());
        
        for (int i = 0; i < roiInterpreter.getInputTensorCount(); i++) {
            Tensor tensor = roiInterpreter.getInputTensor(i);
            Log.d(TAG, String.format("ROI Input %d: %s, shape=%s, name=%s", 
                i, tensor.dataType(), Arrays.toString(tensor.shape()), tensor.name()));
        }
        
        for (int i = 0; i < roiInterpreter.getOutputTensorCount(); i++) {
            Tensor tensor = roiInterpreter.getOutputTensor(i);
            Log.d(TAG, String.format("ROI Output %d: %s, shape=%s, name=%s", 
                i, tensor.dataType(), Arrays.toString(tensor.shape()), tensor.name()));
        }
    }


    // create TensorBuffer directly with correct shape
    private TensorImage createInputWithBatchDimension(Bitmap bitmap) {
        Log.d(TAG, "=== CREATING INPUT WITH BATCH DIMENSION (Direct Approach) ===");
        
        try {
            // Get expected shape
            int[] expectedShape = tflite.getInputTensor(0).shape(); // [1, 384, 768, 3]
            
            if (expectedShape.length == 4) {
                int height = expectedShape[1];
                int width = expectedShape[2];
                int channels = expectedShape[3];
                
                // Resize bitmap to expected dimensions
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                
                // Create pixels array
                int[] pixels = new int[width * height];
                resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                
                // Create float array with batch dimension
                float[] inputArray = new float[1 * height * width * channels];
                


                // Convert pixels to float array (with batch dimension)
                int index = 0;
                for (int pixel : pixels) {

                    // Extract RGB values and normalize to [0, 1]
                    inputArray[index++] = ((pixel >> 16) & 0xFF) / 255.0f; // Red
                    inputArray[index++] = ((pixel >> 8) & 0xFF) / 255.0f;  // Green  
                    inputArray[index++] = (pixel & 0xFF) / 255.0f;         // Blue
                }
                
                // Create TensorBuffer with correct shape
                TensorBuffer tensorBuffer = TensorBuffer.createFixedSize(expectedShape, DataType.FLOAT32);
                tensorBuffer.loadArray(inputArray);
                
                // Create TensorImage
                TensorImage inputImage = new TensorImage(DataType.FLOAT32);
                inputImage.load(tensorBuffer);
                
                Log.d(TAG, "Successfully created input with shape: " + Arrays.toString(inputImage.getTensorBuffer().getShape()));

                inputArray = tensorBuffer.getFloatArray();
                float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
                for (float val : inputArray) {
                  min = Math.min(min, val);
                  max = Math.max(max, val);
                }
                Log.d(TAG, String.format("Input value range: [%.3f, %.3f]", min, max));

                debugInputTensor(inputImage, bitmap);
                return inputImage;
                
            } else {
                Log.e(TAG, "Unexpected input shape length: " + expectedShape.length);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating input with batch dimension: " + e.getMessage(), e);
        }
        
        return null;
    }
}