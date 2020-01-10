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
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


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
    }

    private void launchCamera() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSION);
        }
    }

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

    private Bitmap getResizedBitmap(Bitmap bitmap, int bitmapWidth, int bitmapHeight) {
        return Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, true);
    }

    private int[] toMatrix(Bitmap bitmap){
        int[] matrix = new int[bitmap.getHeight()*bitmap.getWidth()];
        bitmap.getPixels(matrix, 0, bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        return matrix;
    }

    private void loadImage(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    private void scanImage(){
        int[] matrix = toMatrix(toGrayscale(getResizedBitmap(imageBitmap, 28, 28)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            //load bitmap from data intent
            Bundle extras = data.getExtras();
            assert extras != null;
            imageBitmap = (Bitmap) extras.get("data");
            assert imageBitmap != null;
            loadImage(imageBitmap);
        } else {
            //TODO: handle error
        }
    }

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
