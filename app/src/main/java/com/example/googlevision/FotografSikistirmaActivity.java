
package com.example.googlevision;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FotografSikistirmaActivity extends AppCompatActivity {

    private TextView txtFotografAdi, txtOrjinalBoyut, txtKopyaBoyut;
    private ImageView imgOrjinal, imgKopya;
    private StorageReference fotograflarDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compress_image);
        Intent intent = getIntent();
        final String dosyaAdi = intent.getStringExtra("dosyaAdi");

        init();

        txtFotografAdi.setText(dosyaAdi);
        System.out.println("dosyaAdi : " + dosyaAdi);
        dosyaBoyutlariniYazdir(dosyaAdi, txtOrjinalBoyut, imgOrjinal);
        dosyaBoyutlariniYazdir("skstr_" + dosyaAdi, txtKopyaBoyut, imgKopya);
    }

    private void init() {
        fotograflarDatabase = FirebaseStorage.getInstance().getReference();
        imgOrjinal = findViewById(R.id.orjinalImg);
        imgKopya = findViewById(R.id.compressedImage);

        txtFotografAdi = findViewById(R.id.textName);
        txtOrjinalBoyut = findViewById(R.id.orjinalImgText);
        txtKopyaBoyut = findViewById(R.id.compressedImageText);
    }

    private void dosyaBoyutlariniYazdir(final String dosyaAdi, final TextView textView, final ImageView imageView) {
        StorageReference ref = fotograflarDatabase.child(dosyaAdi);
        try {
            final File dosya = File.createTempFile("Images", "jpg");
            ref.getFile(dosya).addOnSuccessListener(new OnSuccessListener< FileDownloadTask.TaskSnapshot >() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap btmp = BitmapFactory.decodeFile(dosya.getAbsolutePath());
                    imageView.setImageBitmap(btmp);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    btmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    String boyut = String.valueOf((dosya.length()/(float)1024));
                    int noktaIndex = boyut.indexOf(".");
                    boyut = boyut.substring(0, noktaIndex + 3);
                    textView.setText("Boyut : " + boyut + " KB");
                    System.out.println("success deneme : " + dosya.length());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(FotografSikistirmaActivity.this, "error : " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




