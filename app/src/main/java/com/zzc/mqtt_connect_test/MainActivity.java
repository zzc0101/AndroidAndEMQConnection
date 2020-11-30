package com.zzc.mqtt_connect_test;

import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String IP_ADDRESS = "tcp://192.168.137.1:1883";
    public static final String clientId = "android_connection";

    public static Button btn_open,btn_close,btn_sub,btn_pub;
    public static EditText topic_sub,topic_pub;
    public static Spinner spinner_sub,spinner_pub;
    public static MqttClient mqttClient = null;
    String content = "{\"msg\":\"第一个测试程序开始划水水！！！\"}";

    static LinkedList<String> linkedList = new LinkedList<String>();

    static String filePath = Environment.getExternalStorageDirectory().getPath()+"/Download/";

    TextView message_textView;
    static Handler handler = null;

    ImageView image;

    static MyImageLoader mImageLoader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new myHandler(this);
        init();    // 控件初始化
        click();    // 点击事件监听

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager. PERMISSION_GRANTED) {
            ActivityCompat. requestPermissions( this, new String[]{Manifest.permission. WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE },
                    1000);
        }

        mImageLoader = new MyImageLoader();

    }

    // 点击事件监听
    private void click() {
        // 连接事件监听
        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.sendEmptyMessage(0x1);
                Connect_Emq();
            }
        });

        // 断开连接监听
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        if(mqttClient != null) {
                            try {
                                mqttClient.disconnect();
                                mqttClient = null;
                                sendMessage("断开连接",0x0);
                            } catch (MqttException e) {
                                e.printStackTrace();
                                Log.i("zzc","关闭异常！");
                                sendMessage("关闭失败！",0x0);
                            }
                        }
                    }
                }.start();
                handler.sendEmptyMessage(0x2);
            }
        });

        // 订阅事件监听
        btn_sub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        if(mqttClient == null) {
                            sendMessage("未连接上！",0x0);
                            return;
                        }
                        if(mqttClient.isConnected()) {
                            try {
                                mqttClient.subscribe("testtopic/2");
                            } catch (MqttException e) {
                                e.printStackTrace();
                                Log.i("zzc","主题订阅异常！");
                                sendMessage("订阅主题失败！！",0x0);
                            }
                        }
                    }
                }.start();
            }
        });

        // 发布事件监听
        btn_pub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        if(mqttClient == null) {
                            sendMessage("未连接上！",0x0);
                            return;
                        }
                        if(mqttClient.isConnected() && mqttClient!=null) {
                            MqttMessage message = new MqttMessage(content.getBytes());
                            message.setQos(Integer.parseInt(String.valueOf(spinner_sub.getSelectedItem())));
                            try {
                                mqttClient.publish(topic_pub.getText().toString().trim(), message);
                            } catch (MqttException e) {
                                e.printStackTrace();
                                Log.i("zzc","发布主题失败！");
                                sendMessage("主题发布失败！",0x0);
                            }
                        }
                    }
                }.start();
            }
        });
    }

    // 控件初始化
    private void init() {
        btn_open = findViewById(R.id.btn_open);
        btn_close = findViewById(R.id.btn_close);
        btn_sub = findViewById(R.id.btn_sub);
        btn_pub = findViewById(R.id.btn_pub);
        topic_sub = findViewById(R.id.topic_sub);
        topic_pub = findViewById(R.id.topic_pub);
        spinner_sub = findViewById(R.id.spinner_sub);
        spinner_pub = findViewById(R.id.spinner_pub);
        message_textView = findViewById(R.id.message_textView);
        image = findViewById(R.id.image);
    }

    public void Connect_Emq() {
        new Thread() {
            @Override
            public void run() {
                MemoryPersistence persistence = new MemoryPersistence();
                try {
                    mqttClient = new MqttClient(IP_ADDRESS,clientId,persistence);
                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    connOpts.setUserName("emqx_test");
                    connOpts.setPassword("emqx_test_password".toCharArray());
                    connOpts.setCleanSession(true);
                    mqttClient.setCallback(new OnMessageCallback());
                    mqttClient.connect(connOpts);
                    sendMessage("连接成功！",0x0);
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.i("zzc","MQTT连接异常");
                    sendMessage("连接失败！",0x0);
                    handler.sendEmptyMessage(0x2);
                }
            }
        }.start();
    }

    // Handler 处理
    class myHandler extends Handler {
        //创建一个类继承 Handler
        WeakReference<AppCompatActivity> mWeakReference;

        public myHandler(AppCompatActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0x0:
                    Toast.makeText(getApplicationContext(),msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                case 0x1:
                    btn_close.setEnabled(true);
                    btn_sub.setEnabled(true);
                    btn_pub.setEnabled(true);
                    topic_sub.setEnabled(true);
                    topic_pub.setEnabled(true);
                    spinner_sub.setEnabled(true);
                    spinner_pub.setEnabled(true);
                    btn_open.setEnabled(false);
                    break;
                case 0x2:
                    btn_open.setEnabled(true);
                    btn_sub.setEnabled(false);
                    btn_pub.setEnabled(false);
                    topic_sub.setEnabled(false);
                    topic_pub.setEnabled(false);
                    btn_close.setEnabled(false);
                    spinner_sub.setEnabled(false);
                    spinner_pub.setEnabled(false);
                    message_textView.setText("");
                    break;

                case 0x4:
                    Bitmap bitmap;
                    bitmap = BitmapFactory.decodeFile(msg.obj.toString());
                    image.setImageBitmap(bitmap);
                    break;

                case 0x5:
                    Bitmap bitmap1 = mImageLoader.getBitmap(msg.obj.toString());
                    if(bitmap1 != null) {
                        image.setImageBitmap(bitmap1);
                    }
                    break;
            }
        }
    }

    // 发送信息
    public static void sendMessage(String msg, int what) {
        Message message = Message.obtain();
        message.what = what;
        message.obj = msg;
        handler.sendMessage(message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1000)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                //同意申请权限
            } else
            {
                // 用户拒绝申请权限
                Toast.makeText(MainActivity.this,"请同意写操作来记录视频数据", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static String getTime() {
        return String.valueOf(System.currentTimeMillis());
    }

}