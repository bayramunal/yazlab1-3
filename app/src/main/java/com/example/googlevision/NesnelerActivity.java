
package com.example.googlevision;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import butterknife.BindView;

public class NesnelerActivity extends AppCompatActivity implements View.OnClickListener{


    @BindView(R.id.imgViewNesne)
    ImageView imgViewNesne;
    @BindView(R.id.btnOnceki)
    Button btnOnceki;
    @BindView(R.id.btnSonraki)
    Button btnSonraki;

    @BindView(R.id.fotografAdi)
    TextView txtFotografAdi;
    @BindView(R.id.txtTahminOrani)
    TextView txtTahminOrani;

    ArrayList<Nesneler> tespitEdilenler;
    int sayac = 0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_images);
        tespitEdilenler = MainActivity.kirpilmisFotograflar;
        System.out.println("size : " + tespitEdilenler.size());

        init();

        imgViewNesne.setImageBitmap(tespitEdilenler.get(0).getBitmap());
        textViewGuncelle(tespitEdilenler.get(sayac).getTahminOrani(), tespitEdilenler.get(sayac).getIsim());
        sayac++;

    }

    private void init() {
        imgViewNesne = findViewById(R.id.imgViewNesne);
        btnSonraki = findViewById(R.id.btnSonraki);
        btnOnceki = findViewById(R.id.btnOnceki);
        txtFotografAdi = findViewById(R.id.fotografAdi);
        txtTahminOrani = findViewById(R.id.txtTahminOrani);

        btnSonraki.setOnClickListener(this);
        btnOnceki.setOnClickListener(this);
    }

    public void textViewGuncelle(float predictRatio, String objectName) {
        txtTahminOrani.setText("Tahmin Oranı : %" + String.valueOf(predictRatio).substring(2,4) + "." + String.valueOf(predictRatio).substring(4,6));
        txtFotografAdi.setText("Nesne Adı : " + objectName);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnSonraki:
                sayac++;
                sayac = sayac % tespitEdilenler.size();
                imgViewNesne.setImageBitmap(tespitEdilenler.get((sayac)).getBitmap());
                textViewGuncelle(tespitEdilenler.get(sayac).getTahminOrani(), tespitEdilenler.get(sayac).getIsim());
                break;
            case R.id.btnOnceki:
                System.out.println("size : " + tespitEdilenler.size());
                System.out.println("count : " + sayac);
                if (sayac == 0)
                    sayac = tespitEdilenler.size() - 1;
                else
                    sayac--;
                textViewGuncelle(tespitEdilenler.get(sayac).getTahminOrani(), tespitEdilenler.get(sayac).getIsim());
                imgViewNesne.setImageBitmap(tespitEdilenler.get(sayac).getBitmap());
                break;
        }
    }
}




