package org.rti.ttfinder.wrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.pytorch.LiteModuleLoader;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.DelegateFactory;
import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.CompatibilityList;

import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;

public abstract class TTInterpreter {

    // Constants used for the pipeline
    protected final static int SUB_IMAGE_SIZE = 448;
    protected final static int NUM_SUBIMAGES = 16;

    public boolean use_tflite = false;

    public String TAG ="TTInterpreterBase";

    /** Stage for TT interpretation */
    public enum Stage {
        SEGMENT,
        FEATURE_EXTRACT,
        CLASSIFY,
        EXTRACT_AND_CLASSIFY,
        DETECT,
    }

    /** The runtime device type used for executing one of the interpreters */
    public enum Device {
        CPU,
        NNAPI,
        GPU
    }

    /** Input image tensor buffer*/
    protected TensorImage inputImageBuffer;

    /** Output TensorBuffer. */
    protected TensorBuffer outputBuffer;
    protected TensorBuffer boxesBuffer;
    protected TensorBuffer scoresBuffer;
    protected TensorBuffer labelsBuffer;

    //private FlexDelegate flexDelegate = null;

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite;

    /** Image size along the x axis. */
    protected  int imageSizeX;

    /** Image size along the y axis. */
    protected int imageSizeY;

    protected long lastInferenceTimeCost;
    protected StringBuilder lastInferenceDetails;

    private static final float IMAGE_MEAN = 0.f; //127.0f;

    private static final float IMAGE_STD = 255.0f; //128.0f;

    private static final float PROBABILITY_MEAN = 0.0f;

    private static final float PROBABILITY_STD = 1.0f;

    protected TensorOperator getPreprocessNormalizeOp()
    {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }

    protected TensorOperator getPostprocessNormalizeOp() {
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    /* Pytorch variables*/
    protected Module torchlite;

    protected Tensor inputImgTorch;
    protected Tensor outputImgTorch;

    /**
     * Creates an interpreter with the provided status
     *
     * @param activity The current Activity.
     * @param stage The model to use for classification.
     * @param device The device to use for classification.
     * @param numThreads The number of threads to use for classification.
     * @return An interpreter with the desired configuration.
     */
    public static TTInterpreter create(Context activity, Stage stage, Device device, int numThreads,
                                       boolean use_tflite )
            throws IOException {
        if (stage == Stage.SEGMENT) {
            return new TTSegmentInterpreter(activity, device, numThreads, use_tflite);
        } else if (stage == Stage.FEATURE_EXTRACT) {
            return new TTFeatureExtractInterpreter(activity, device, numThreads);
        } else if (stage == Stage.CLASSIFY) {
            return new TTClassifierInterpreter(activity, device, numThreads);
        } else if (stage == Stage.EXTRACT_AND_CLASSIFY) {
            return new TTClassifierWithFeaturesInterpreter(activity, device, numThreads);
        } else if (stage == Stage.DETECT) {
            return new TTDetectionInterpreter(activity, device, numThreads);

        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected TTInterpreter(Context activity, Device device, int numThreads, boolean use_tflite)
            throws IOException {
        this.use_tflite = use_tflite;
        if (use_tflite == true) {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(activity, getModelPath());
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            switch (device) {
                case NNAPI:
                    /* Optional NNAPI delegate for accleration. */
                    NnApiDelegate nnApiDelegate = new NnApiDelegate();
                    tfliteOptions.addDelegate(nnApiDelegate);
                    tfliteOptions.setNumThreads(numThreads);
                    break;
//                case GPU:
//                    CompatibilityList compatList = new CompatibilityList();
//                    GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
////                    delegateOptions.setQuantizedModelsAllowed(false);
//                    /* Optional GPU delegate for acceleration. */
//                    GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
//                    tfliteOptions.addDelegate(gpuDelegate);
//                    break;
                case CPU:
                    tfliteOptions.setUseXNNPACK(true);
                    tfliteOptions.setNumThreads(numThreads);
                    break;
//                case FLEX:
//                    flexDelegate = new FlexDelegate();
//                    tfliteOptions.addDelegate(flexDelegate);
//                    tflite.setNumThreads(numThreads);
//                    break;
            }
            tflite = new Interpreter(tfliteModel, tfliteOptions);
            // Reads type and shape of input and output tensors, respectively.
            int inputTensorIndex = 0;
            int[] imageShape = tflite.getInputTensor(inputTensorIndex).shape(); // {1, height, width, 3}
            imageSizeY = imageShape[1];
            imageSizeX = imageShape[2];
            DataType imageDataType = tflite.getInputTensor(inputTensorIndex).dataType();
            // Creates the input tensor.
            Log.v(TAG, "input data type: " + imageDataType);
            for (int j : imageShape) Log.v(TAG, j + ", ");

            inputImageBuffer = new TensorImage(imageDataType);

            int outputTensorIndex = 0;
            int[] outputShape =
                    tflite.getOutputTensor(outputTensorIndex).shape(); // {1, NUM_CLASSES}
            DataType outputDataType = tflite.getOutputTensor(outputTensorIndex).dataType();
            outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);
            lastInferenceDetails = new StringBuilder();
            lastInferenceTimeCost = 0;

            Log.d(TAG, "Created the Tensorflow Lite Interpreter");
        }
        else{
            /* Create a pytorch interpreter */
            try {
                torchlite = LiteModuleLoader.load(assetFilePath(activity, getModelPath()));
                lastInferenceDetails = new StringBuilder();
                lastInferenceTimeCost = 0;

                Log.d(TAG, "Created the Pytorch Lite Interpreter");
            }catch(IOException e){
                Log.e("PytorchHelloWorld", "Error reading assets", e);
                torchlite = null;
            }
        }
    }

    /** Gets the name of the model file stored in Assets. */
    protected abstract String getModelPath();

    /** Run inference  */
    public abstract boolean runInference(Bitmap bitmap);

    /** Get outputs */
    public TensorBuffer getOutputBuffer()
    {
        return outputBuffer;
    }

    public long getLastTmeCostForInference() {
        return lastInferenceTimeCost;
    }

    public String getLastInferenceRunDetails() {
        return lastInferenceDetails.toString();
    }

    protected void updateLastInferenceRunDetails(final String msg) {
        lastInferenceDetails.append(TAG);
        lastInferenceDetails.append(" :: ");
        lastInferenceDetails.append(msg);
        lastInferenceDetails.append("\n");
    }

    protected void logSectionTimeCost(final String sectionDescription, final long sectionTimeCost ) {
        lastInferenceTimeCost +=sectionTimeCost;
        String msg = "Time cost to " + sectionDescription + " :: " + sectionTimeCost + "ms";
        updateLastInferenceRunDetails(msg);
        Log.v(TAG, msg);
    }

    private String assetFilePath(Context context, String assetName) throws IOException {
        Log.v(TAG, "Getting asset file path: " + assetName);
        File file = new File(context.getFilesDir(), assetName);
//        if (file.exists() && file.length() > 0) {
//            Log.v(TAG, "Found the model: " + file.getAbsolutePath().toString());
//            return file.getAbsolutePath();
//        }
        Log.v(TAG, "Didn't fine it easily: " + context.getFilesDir().toString());
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            Log.v(TAG, "Found the model: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        }
    }

    public boolean isUse_tflite() {
        return use_tflite;
    }

    public void setUse_tflite(boolean use_tflite) {
        this.use_tflite = use_tflite;
    }
}

