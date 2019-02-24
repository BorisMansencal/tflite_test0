package fr.mansencal.test0;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;


public class ImageClassifier {

    /** Tag for the {@link Log}. */
    private static final String TAG = "TEST0_IMAGECLASSIFIER";

    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_PIXEL_SIZE = 3;

    private static final float IMAGE_MEAN = (float)(119.75100708007812);
    private static final float IMAGE_STD = (float)(49.6877326965332);


    /** Preallocated buffers for storing image data in. */
    private int[] intValues = new int[getImageSizeX() * getImageSizeY()];


    /** Options for configuring the Interpreter. */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel;

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite;

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    protected ByteBuffer imgData = null;

    /** A ByteBuffer to hold segmentation data, to be feed into Tensorflow Lite as outputs. */
    private ByteBuffer segData = null;


    /** holds a gpu delegate */
    Delegate gpuDelegate = null;

    ImageClassifier(Activity activity) throws IOException {
        tfliteModel = loadModelFile(activity);

        tfliteOptions.setUseNNAPI(false);

        tflite = new Interpreter(tfliteModel, tfliteOptions);

        imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                                * getImageSizeX()
                                * getImageSizeY()
                                * DIM_PIXEL_SIZE
                                * getNumBytesPerChannel());
        imgData.order(ByteOrder.nativeOrder());

        segData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                                * getImageSizeX()
                                * getImageSizeY()
                                * 1
                                * getNumBytesPerChannel());
        segData.order(ByteOrder.nativeOrder());

        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
    }

    /** Classifies a frame from the preview stream. */
    Bitmap classifyFrame(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            return bitmap;
        }
        convertBitmapToByteBuffer(bitmap);
        // Here's where the magic happens!!!
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime) + "ms");

        startTime = SystemClock.uptimeMillis();
        Bitmap segBmp = getSegBitmap();
        endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to getSegBitmap: " + Long.toString(endTime - startTime) + "ms");
        return segBmp;
    }

    protected void runInference() {
        tflite.run(imgData, segData);
    }

    private Bitmap getSegBitmap() {
        final int dim_x = getImageSizeX();
        final int dim_y = getImageSizeY();

        if (segData == null) {
            for (int i=0; i<dim_x*dim_y; i++)
                intValues[i] = (255 & 0xff) << 24 | (255 & 0xff) << 16; //0;
        }
        else {
            segData.rewind();

            long startTime = SystemClock.uptimeMillis();
            int pixel = 0;
            for (int i = 0; i < dim_x; ++i) {
                for (int j = 0; j < dim_y; ++j) {
                    final float val = segData.getFloat(); //pixel);
                    final int vali = (int) (val * 255 + 0.5);
                    final int color = (255 << 24) | (vali << 16) | (vali << 8) | (vali);
                    intValues[pixel] = color;
                    pixel++;
                }
            }
            long endTime = SystemClock.uptimeMillis();
            Log.d(TAG, "Timecost to put values from ByteBuffer into intValues in getSegBitmap: " + Long.toString(endTime - startTime) + "ms");
        }

        Bitmap bmp = Bitmap.createBitmap(intValues, dim_x, dim_y, Bitmap.Config.ARGB_8888);

        return bmp;
    }





    protected int getImageSizeX() {
        return 512;
    }

    protected int getImageSizeY() {
        return 512;
    }

    protected int getNumBytesPerChannel() {
        // a 32bit float value requires 4 bytes
        return 4;
    }

    protected String getModelPath() {
        return "graph.tflite";
    }


    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }

    protected void addPixelValue(int pixelValue) {
        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    }


}
