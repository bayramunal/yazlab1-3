
package com.example.googlevision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;

    private static final String CLOUD_VISION_API_KEY = "**YOUR**API**KEY";

    @BindView(R.id.fotografSec)
    Button btnFotografSec;

    @BindView(R.id.fotografCek)
    Button btnFotografCek;

    @BindView(R.id.imageProgressBar)
    ProgressBar imageUploadProgress;

    @BindView(R.id.fotograf)
    ImageView secilenFotograf;

    @BindView(R.id.analizAktivite)
    Button btnAnalizAktivite;

    @BindView(R.id.sikistirmaAktivite)
    Button btnSikistirmaAktivite;

    @BindView(R.id.btnAnaliz)
    Button btnAnaliz;

    @BindView(R.id.btnSikistirma)
    Button btnSikistirma;

    LinearLayout layoutIslemSec;

    private Feature ozellik;
    private Bitmap bitmap;
    private String api = "OBJECT_LOCALIZATION";
    boolean flag = false;
    public static ArrayList<Nesneler> kirpilmisFotograflar = new ArrayList<>();
    public static StorageReference fotografDepo;
    private Uri imgUri;
    public static FirebaseDatabase firebaseVeritabani;
    public static DatabaseReference veritabaniReferans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutIslemSec = findViewById(R.id.layoutIslemSec);

        ButterKnife.bind(this);

        ozellik = new Feature();
        ozellik.setType(api);
        ozellik.setMaxResults(20);

        fotografDepo = FirebaseStorage.getInstance().getReference();
        firebaseVeritabani = FirebaseDatabase.getInstance();
        veritabaniReferans = firebaseVeritabani.getReference("photos");

        btnSikistirmaAktivite.setOnClickListener(this);
        btnSikistirma.setOnClickListener(this);
        btnAnaliz.setOnClickListener(this);
        btnFotografSec.setOnClickListener(this);
        btnFotografCek.setOnClickListener(this);
        btnAnalizAktivite.setOnClickListener(this);

    }

    private boolean internetBaglantiDurumu() {
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        else
            return false;
    }

    private String fotografUzantisiGetir(Uri uri) {
        ContentResolver mResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(mResolver.getType(uri));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (kameraIzniAl(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            btnFotografCek.setVisibility(View.VISIBLE);
        } else {
            btnFotografCek.setVisibility(View.INVISIBLE);
            istekOlustur(Manifest.permission.CAMERA);
        }
    }

    private int kameraIzniAl(String permission) {
        return ContextCompat.checkSelfPermission(this, permission);
    }

    private void istekOlustur(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, RECORD_REQUEST_CODE);
    }

    public void kameradanFotografAl(boolean flag) {
        if (flag == true) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (flag == true ) {
            if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
                bitmap = (Bitmap) data.getExtras().get("data");
                imgUri = data.getData();
                secilenFotograf.setImageBitmap(bitmap);
                layoutIslemSec.setVisibility(View.VISIBLE);
                btnAnaliz.setEnabled(true);
                btnSikistirma.setEnabled(true);
            }
            System.out.println("Image uri " + imgUri.toString());
        } else {
            if (resultCode == RESULT_OK) {
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(imageStream);
                    imgUri = data.getData();
                    secilenFotograf.setImageBitmap(bitmap);
                    layoutIslemSec.setVisibility(View.VISIBLE);
                    btnAnaliz.setEnabled(true);
                    btnSikistirma.setEnabled(true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RECORD_REQUEST_CODE) {
            if (grantResults.length == 0 && grantResults[0] == PackageManager.PERMISSION_DENIED
                    && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                finish();
            } else {
                btnFotografCek.setVisibility(View.VISIBLE);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void cloudVisionCalistir(final Bitmap bitmap, final Feature ozellik) {
        imageUploadProgress.setVisibility(View.VISIBLE);
        final List<Feature> ozellikListesi = new ArrayList<>();
        ozellikListesi.add(ozellik);

        final List<AnnotateImageRequest> fotografIstekListesi = new ArrayList<>();

        AnnotateImageRequest fotografIstegi = new AnnotateImageRequest();
        fotografIstegi.setFeatures(ozellikListesi);
        fotografIstegi.setImage(base64Fotograf(bitmap));
        fotografIstekListesi.add(fotografIstegi);


        new AsyncTask<Object, Void, String>() {

            @Override
            protected String doInBackground(Object... params) {
                try {

                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest istek = new BatchAnnotateImagesRequest();
                    istek.setRequests(fotografIstekListesi);

                    Vision.Images.Annotate visionIStek = vision.images().annotate(istek);
                    visionIStek.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse yanit = visionIStek.execute();
                    return servisYaniti(yanit);
                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
                return "Cloud Vision API isteği başarısız oldu. Detaylar için logları gözden geçirin.";
            }

            protected void onPostExecute(String sonuc) {
                System.out.println("rest : " + sonuc);
                imageUploadProgress.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    @NonNull
    private Image base64Fotograf(Bitmap bitmap) {
        Image base64EncodedImage = new Image();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        byte[] imageBytes = outputStream.toByteArray();

        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    ArrayList<int[]> koordinatListesi = new ArrayList<>();
    private String servisYaniti(BatchAnnotateImagesResponse yanit) {

        AnnotateImageResponse imageResponses = yanit.getResponses().get(0);
        System.out.println("TEST " + imageResponses.toString());

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(imageResponses.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String tespitYaniti = jsonObject.get("localizedObjectAnnotations").toString();
            JSONArray jsonTespitYaniti = new JSONArray(tespitYaniti);
            System.out.println("name : " + jsonTespitYaniti.getJSONObject(0).get("name")); // -> name field'ı alınıyor

            JSONArray jsonArray = new JSONArray(tespitYaniti);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonBoundingPoly = new JSONObject((new JSONObject(jsonArray.getJSONObject(i).toString())).get("boundingPoly").toString());
                JSONArray jsonArrayKoordinatlar = new JSONArray(jsonBoundingPoly.get("normalizedVertices").toString());
                for( int j = 0; j < jsonArrayKoordinatlar.length(); j++) {
                    String temp = jsonArrayKoordinatlar.getJSONObject(j).toString();
                    temp = temp.substring(1, temp.length() - 1);
                    String[] split = temp.split(",");
                    for (int k = 0; k < split.length; k++) {
                        split[k] = split[k].substring(4);
                    }
                    koordinatListesi.add(new int[]{(int)(Float.parseFloat(split[0])*bitmap.getWidth()),(int)(Float.parseFloat(split[1])*bitmap.getHeight())});
                }
            }

            for (int i = 0; i < koordinatListesi.size(); i = i + 4) {
                Bitmap subBitmap = Bitmap.createBitmap(bitmap, koordinatListesi.get(i)[0],
                        koordinatListesi.get(i)[1],
                        koordinatListesi.get(i+1)[0] - koordinatListesi.get(i)[0],
                        koordinatListesi.get(i+2)[1] - koordinatListesi.get(i)[1] );

                kirpilmisFotograflar.add(new Nesneler(subBitmap,
                        "",
                        0)
                );

            }

            for (int i = 0; i < jsonTespitYaniti.length(); i++) {
                kirpilmisFotograflar.get(i).setIsim(jsonTespitYaniti.getJSONObject(i).get("name").toString());
                kirpilmisFotograflar.get(i).setTahminOrani(Float.parseFloat(jsonTespitYaniti.getJSONObject(i).get("score").toString()));
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnAnalizAktivite.setEnabled(true);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

        List<EntityAnnotation> nesneAnotasyonu;
        nesneAnotasyonu = imageResponses.getLabelAnnotations();
        String message = formatAnnotation(nesneAnotasyonu);

        return message;
    }
    private String formatAnnotation(List<EntityAnnotation> nesneAnotasyonu) {
        String message = "";
        if (nesneAnotasyonu != null)
            for (EntityAnnotation nesne : nesneAnotasyonu)
                message = message + "    " + nesne.getDescription() + " " + nesne.getScore();
        else
            message = "Bir şey bulunamadı";

        return message;
    }



    private void fotografiBulutaKaydet(final String dosyaAdi, final String firstName, final String compress) {
        StorageReference firebaseFotograflar = fotografDepo.child(dosyaAdi);
        firebaseFotograflar.putFile(imgUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Toast.makeText(MainActivity.this, "Yükleme başarılı", Toast.LENGTH_SHORT).show();
                        imageUploadProgress.setVisibility(View.INVISIBLE);

                        Map<String, String> kayit = new HashMap<>();
                        kayit.put("orjinalUrl", taskSnapshot.getMetadata().getReference().getDownloadUrl().toString());
                        veritabaniReferans = firebaseVeritabani.getReference("photos/"+firstName+"-"+compress);
                        veritabaniReferans.setValue(kayit).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, "İşlem Başarılı", Toast.LENGTH_LONG).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Firebase hata " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });

                        btnSikistirmaAktivite.setEnabled(true);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        imageUploadProgress.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hataMesajiGoster (AlertDialog.Builder builder, String mesaj) {

        builder.setMessage(mesaj);
        builder.setCancelable(false);
        builder.setTitle("Hata!");
        builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.create().show();

    }
    String dosyaAdi;
    @Override
    public void onClick(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final LayoutInflater layoutInflaterAndroid = LayoutInflater.from(MainActivity.this);


        switch(view.getId()) {
            case R.id.btnSikistirma:

                if(imgUri != null) {
                    if (internetBaglantiDurumu()) {
                        final View alertGorunum = layoutInflaterAndroid.inflate(R.layout.custom_alert_dialog, null);
                        builder.setView(alertGorunum);
                        builder.setTitle("Fotoğraf sıkıştırma");
                        builder.setCancelable(false);
                        builder.setNeutralButton("Tamam", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                imageUploadProgress.setVisibility(View.VISIBLE);
                                EditText edtCompress = alertGorunum.findViewById(R.id.edtCompressRatio);
                                final int compress = Integer.parseInt(edtCompress.getText().toString());
                                final long firstName = System.currentTimeMillis();
                                dosyaAdi = firstName + "-" + compress +  "." + fotografUzantisiGetir(imgUri);
                                fotografiBulutaKaydet(dosyaAdi, String.valueOf(firstName), String.valueOf(compress));
                            }
                        });
                        builder.create().show();
                    } else {
                        hataMesajiGoster(builder, "Lütfen internat bağlantınızı kontrol ediniz");
                    }
                } else {
                    hataMesajiGoster(builder, "Lütfen bir fotoğraf seçiniz");
                }
                break;

            case R.id.btnAnaliz:

                if (imgUri != null) {
                    if (internetBaglantiDurumu()) {
                        cloudVisionCalistir(bitmap, ozellik);
                    } else {
                        hataMesajiGoster(builder, "Lütfen internat bağlantınızı kontrol ediniz");
                    }
                } else {
                    hataMesajiGoster(builder, "Lütfen bir fotoğraf seçiniz");
                }

                break;

            case R.id.fotografSec:
                flag = false;
                kameradanFotografAl(false);
                break;

            case R.id.fotografCek:
                flag = true;
                kameradanFotografAl(true);
                break;

            case R.id.analizAktivite:
                Intent intent = new Intent(MainActivity.this, NesnelerActivity.class);
                startActivity(intent);
                break;

            case R.id.sikistirmaAktivite:
                Intent intent2 = new Intent(MainActivity.this, FotografSikistirmaActivity.class);
                intent2.putExtra("dosyaAdi", dosyaAdi);
                startActivity(intent2);
                break;
        }
    }
}



