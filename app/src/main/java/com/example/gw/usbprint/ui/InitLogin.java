package com.example.gw.usbprint.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gw.usbprint.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class InitLogin extends AppCompatActivity implements View.OnClickListener{
    private TextView tvCard,tvLogin,tvRegister,faceLogin;
    private ImageView bg;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init_login);
        init();

    }

    public void init(){
        EventBus.getDefault().register(this);

        bg = (ImageView)findViewById(R.id.bg);


        tvCard = (TextView)findViewById(R.id.tvCard);
        tvLogin = (TextView)findViewById(R.id.tvLogin);
        tvRegister = (TextView)findViewById(R.id.tvRegister);
        faceLogin = (TextView)findViewById(R.id.faceLogin);
        tvCard.setOnClickListener(this);
        tvLogin.setOnClickListener(this);
        tvRegister.setOnClickListener(this);
        faceLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.tvCard :
                 intent =  new Intent(InitLogin.this, LoginActivity.class);
                intent.putExtra("flag", 0);
                startActivity(intent);
                break;
            case R.id.tvLogin :
                intent =  new Intent(InitLogin.this, LoginActivity.class);
                intent.putExtra("flag", 1);
                startActivity(intent);
                break;
            case R.id.tvRegister :
                 intent = new Intent(InitLogin.this, TBSWebViewActivity.class);
                intent.putExtra("flag", 1);
                startActivity(intent);
                break;
            case R.id.faceLogin:
                 intent = new Intent(InitLogin.this,RegisterAndRecognizeActivity.class);
                intent.putExtra("flag",false);
                startActivity(intent);
                break;

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void finish(SuccessEvent successEvent){

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
