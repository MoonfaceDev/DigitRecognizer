package com.moonface.digitrecognizer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 0;
    private static final int REQUEST_PERMISSION = 1;
    private ImageView imageView;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FloatingActionButton cameraButton = findViewById(R.id.camera_button);
        MaterialButton scanButton = findViewById(R.id.scan_button);
        imageView = findViewById(R.id.digit_image);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCamera();
            }
        });
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanImage();
            }
        });
    }

    //launches the camera intent
    private void launchCamera() {
        //checks if app has camera permission
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            //launches camera intent
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        } else {
            //requests permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
        }
    }

    //converts bitmap to grayscale (0-255)
    Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int height = bmpOriginal.getHeight();
        int width = bmpOriginal.getWidth();

        Bitmap grayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(grayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return grayscale;
    }

    //resize bitmap to specified dimensions
    private Bitmap getResizedBitmap(@NonNull Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, 28, 28, true);
    }

    //converts bitmap to array of doubles
    private double[] toMatrix(@NonNull Bitmap bitmap){
        int[] matrix = new int[bitmap.getHeight()*bitmap.getWidth()];
        bitmap.getPixels(matrix, 0, bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        double[] d_matrix = new double[matrix.length];
        for(int i=0; i<matrix.length; i++){
            d_matrix[i] = matrix[i];
        }
        return d_matrix;
    }

    private @NonNull double[][] dot(double[][] a, double[][] b){
        if(a.length != b[0].length){
            return new double[0][0];
        }
        int height = b.length;
        int width = a[0].length;
        double[][] out = new double[height][width];
        for(int i=0; i<height; i++){
            for(int j=0; j<width; j++){
                out[i][j] = dotCell(a,b,i,j);
            }
        }
        return out;
    }

    private double dotCell(double[][] a, double[][] b, int i, int j) {
        double c = 0;
        for (int k = 0; k < b.length; k++) {
            c += a[i][k] * b[k][j];
        }
        return c;
    }

    private double[] sigmoid(double[] a){
        double[] b = a.clone();
        for(int i=0; i<=a.length; i++){
            b[i] = 1/(1+Math.exp(-a[i]));
        }
        return b;
    }

    private double[] add(double[] a, double[] b){
        double[] c = a.clone();
        for(int i=0; i<a.length; i++){
            c[i] += b[i];
        }
        return c;
    }

    private int maxIndex(double[] a){
        double max = 0;
        int maxIndex = 0;
        for(int i=0; i<a.length; i++){
            if(a[i] > max){
                max = a[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    //loads bitmap into the image view
    private void loadImage(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    //scans the digit image and displays the result
    private void scanImage(){
        if(imageBitmap != null) {
            double[] matrix = toMatrix(toGrayscale(getResizedBitmap(imageBitmap)));
            double[][][] weights = importWeights();
            double[] biases = importBiases();
            assert weights != null;
            assert biases != null;
            double[] hidden = sigmoid(add(dot(new double[][]{matrix}, weights[0])[0], new double[]{biases[0]}));
            double[] output = sigmoid(add(dot(new double[][]{hidden}, weights[1])[0], new double[]{biases[1]}));
            int result = maxIndex(output);
            TextView resultView = findViewById(R.id.result_text);
            resultView.setText(String.valueOf(result));
        }
    }

    private double[][][] importWeights(){
        try {
            InputStream inputStream0 = getAssets().open("weights0.csv");
            InputStream inputStream1 = getAssets().open("weights1.csv");

            BufferedReader reader0 = new BufferedReader(
                    new InputStreamReader(inputStream0, Charset.forName("UTF-8")));
            BufferedReader reader1 = new BufferedReader(
                    new InputStreamReader(inputStream1, Charset.forName("UTF-8")));

            List<String[]> lines0 = new ArrayList<>();
            String line0;
            while((line0 = reader0.readLine()) != null){
                lines0.add(line0.split(","));
                Log.d("WEIGHTS", line0);
            }
            List<String[]> lines1 = new ArrayList<>();
            String line1;
            while((line1 = reader1.readLine()) != null){
                lines1.add(line1.split(","));
            }

            double[][] weights0 = new double[784][30];
            for(int i=0; i<lines0.size()-1; i++){
                for(int j=0; j<lines0.get(i).length-1; j++){
                    weights0[i][j] = Double.parseDouble(lines0.get(i+1)[j+1]);
                }
            }
            double[][] weights1 = new double[30][10];
            for(int i=0; i<lines1.size()-1; i++){
                for(int j=0; j<lines1.get(i).length-1; j++){
                    weights1[i][j] = Double.parseDouble(lines1.get(i+1)[j+1]);
                }
            }

            return new double[][][]{weights0,weights1};

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private double[] importBiases(){
        try {
            InputStream inputStream = getAssets().open("biases.csv");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, Charset.forName("UTF-8")));

            double[] biases = new double[2];
            biases[0] = Double.parseDouble(reader.readLine().split(",")[0]);
            biases[1] = Double.parseDouble(reader.readLine().split(",")[0]);

            return biases;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //handle camera intent result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            //load bitmap from data intent
            Bundle extras = data.getExtras();
            assert extras != null;
            imageBitmap = (Bitmap) extras.get("data");
            assert imageBitmap != null;
            //load image into imageView
            loadImage(imageBitmap);
        } else {
            //TODO: handle error
        }
    }

    //launch camera after got permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                //TODO: handle error
            }
        }
    }
}
