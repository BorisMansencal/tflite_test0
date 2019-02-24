package fr.mansencal.test0;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private Button mRunButton;
    private ImageView mImageView;
    private Bitmap mBmp;

    /** Tag for the {@link Log}. */
    private static final String TAG = "TEST0_MAINACTIVITY";

    private ImageClassifier mImageClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRunButton = (Button) findViewById(R.id.runButton);
        mImageView = (ImageView) findViewById(R.id.imageView);

        mRunButton.setOnClickListener(runListener);
    }


    private void classifyFrame(Bitmap bmp) {
        final long startTime = SystemClock.uptimeMillis();
        if (mImageClassifier == null) {
            try {
                Log.e(TAG, "image classifier creation started");
                final long startTime1 = SystemClock.uptimeMillis();
                mImageClassifier = new ImageClassifier(this);
                final long endTime1 = SystemClock.uptimeMillis();
                Log.d(TAG, "Timecost for ImageClassifier constructor: " + Long.toString(endTime1 - startTime1) + "ms");
                Log.e(TAG, "image classifier created.");
            } catch (IOException e) {
                Log.e(TAG, "Failed to initialize an image classifier.");
            }
        }

        if (mImageClassifier != null) {

            final int orig_width = bmp.getWidth();
            final int orig_height = bmp.getHeight();
            final int seg_width = mImageClassifier.getImageSizeX();
            final int seg_height = mImageClassifier.getImageSizeY();

            final boolean useBilinearFiltering = true;
            if (orig_width != seg_width || orig_height != seg_height) {
                bmp = Bitmap.createScaledBitmap(bmp, seg_width, seg_height, useBilinearFiltering);
            }

            Bitmap segBmp = mImageClassifier.classifyFrame(bmp);
            bmp.recycle();
            //Toast.makeText(context, textToShow, Toast.LENGTH_LONG).show();

            if (orig_width != seg_width || orig_height != seg_height) {
                segBmp = Bitmap.createScaledBitmap(segBmp, orig_width, orig_height, useBilinearFiltering);
            }

            mImageView.setImageBitmap(segBmp);
            mImageView.postInvalidate();
        }

        final long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "TOTAL Timecost to classifyFrame(): " + Long.toString(endTime - startTime) + "ms");
        Log.e(TAG, "classifyFrame done");

    }


    private View.OnClickListener runListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            //opts.inMutable = true;
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pizza512, opts);
            if (bmp != null) {
                classifyFrame(bmp);
            }
        }

    };

}
