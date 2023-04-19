package com.example.cameraapp;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Rational;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cameraapp.DB.Photos;
import com.example.cameraapp.DB.PhotosDB;
import com.example.cameraapp.ml.Model;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    PreviewView imageView;
    Button button;
    TextView textView;
    CardView cardView;
    PhotosDB db;

    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                startCamera(cameraFacing);
            }
        }
    });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = PhotosDB.getInstance(this.getApplicationContext());
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);
        cardView = findViewById(R.id.cardView);

        textView.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, list_of_photos.class);
            startActivity(intent);
        });

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);

        } else {
            startCamera(cameraFacing);
        }
    }

    public void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(imageView.getWidth(), imageView.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();

                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        //.setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .setTargetRotation(Surface.ROTATION_90)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                button.setOnClickListener(view -> {
                    takePicture(imageCapture);

                });
                preview.setSurfaceProvider(imageView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public void takePicture(ImageCapture imageCapture) {
        final File file = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(MainActivity.this, "Image saved at: " + file.getPath(), Toast.LENGTH_SHORT).show();
                        Photos photos = new Photos();
                        photos.path = file.getPath();
                        photos.result = "Nothing";
                        try {
                            Toast.makeText(MainActivity.this, "DAS IST " + processImage(file.getAbsoluteFile()), Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        db.photosDAO().insert(photos);
                    }
                });
                startCamera(cameraFacing);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                startCamera(cameraFacing);
            }
        });
    }


    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public String processImage(File image) throws IOException {
        try {
            Model model = Model.newInstance(getApplicationContext());
            Bitmap originalBitmap = null;
            FileInputStream stream = new FileInputStream(image);
            originalBitmap = BitmapFactory.decodeStream(stream);
            stream.close();

            // Обрезаем bitmap до соотношения сторон 3:4
            Bitmap resizedBitmap = null;
            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();
            float targetRatio = 3f / 4f;
            float originalRatio = (float) originalWidth / originalHeight;

            File file1 = new File(getExternalFilesDir(null), System.currentTimeMillis() + "Original" + ".jpg");

            // Открываем поток для записи изображения в файл
            OutputStream outputStream1 = new FileOutputStream(file1);

            // Конвертируем Bitmap в JPEG и записываем в поток
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream1);

            // Закрываем поток
            outputStream1.close();
            if (originalRatio > targetRatio) {
                // нужно обрезать по горизонтали
                int targetWidth = (int) (originalHeight * targetRatio);
                int startX = (originalWidth - targetWidth) / 2;
                resizedBitmap = Bitmap.createBitmap(originalBitmap, startX, 0, targetWidth, originalHeight);
            } else {
                // нужно обрезать по вертикали
                int targetHeight = (int) (originalWidth / targetRatio);
                int startY = (originalHeight - targetHeight) / 2;
                resizedBitmap = Bitmap.createBitmap(originalBitmap, 0, startY, originalWidth, targetHeight);
            }
            File file = new File(getExternalFilesDir(null), System.currentTimeMillis() + "beforeResize" + ".jpg");

            // Открываем поток для записи изображения в файл
            OutputStream outputStream = new FileOutputStream(file);

            // Конвертируем Bitmap в JPEG и записываем в поток
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            // Закрываем поток
            outputStream.close();
            resizedBitmap = Bitmap.createScaledBitmap(resizedBitmap, 90, 120, true);

            File file2 = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");

            // Открываем поток для записи изображения в файл
            OutputStream outputStream2 = new FileOutputStream(file2);

            // Конвертируем Bitmap в JPEG и записываем в поток
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream2);

            // Закрываем поток
            outputStream2.close();


            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 90, 120, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 90 * 120 * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[90 * 120];
            resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

            int pix = 0;
            for (int i = 0; i < 90; i++) {
                for (int j = 0; j < 120; j++) {
                    int val = intValues[pix++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            float[] confidence = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidence.length; i++) {
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Dress", "Hat", "Pants", "Shirt", "Shoes", "Shorts", "Skirt", "Sweater", "TShirt"};
            model.close();
            return classes[maxPos];
        } catch (IOException e) {
            // TODO Handle the exception
        }
        return "";
    }
}