package com.jabin.mybutterknife;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jabin.mybk.annotation.BindView;
import com.jabin.mybk.annotation.OnClick;
import com.jabin.mybk.sdk.MyButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv)
    TextView tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyButterKnife.bind(this);
    }

    @OnClick(R.id.tv)
    public void tvClick(View view){
        Toast.makeText(this, "tvClick", Toast.LENGTH_LONG).show();
    }
}