package com.zzc.mqtt_connect_test;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.MediaPlayer.SEEK_CLOSEST_SYNC;

public class MainActivity extends AppCompatActivity {
    public static final String IP_ADDRESS = "ws://192.168.137.1:8083";
    public static final String clientId = "android_connection";

    public static Button btn_open,btn_close,btn_sub,btn_pub;
    public static EditText topic_sub,topic_pub;
    public static Spinner spinner_sub,spinner_pub;
    public static MqttClient mqttClient = null;
    String content = "{\"msg\":\"第一个测试程序开始划水水！！！\"}";

    boolean flag = false;
    boolean flagPlay = false;

    static TextView message_textView;
    Handler handler = null;

    int count = 0;

//    SurfaceView surface_view;
    VideoView surface_view;
    static MediaPlayer mediaPlayer = new MediaPlayer();

    int index = 0;
    int end = 0;

    ImageView image;

    File fileVedio=new File(Environment.getExternalStorageDirectory(),"9999.mp4");
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

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
//        surface_view = findViewById(R.id.surface_view);
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
    static class myHandler extends Handler {
        private Context context;
        public myHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0x0:
                    Toast.makeText(context.getApplicationContext(),msg.obj.toString(),Toast.LENGTH_SHORT).show();
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
            }
        }
    }

    // 发送信息
    public void sendMessage(String msg,int what) {
        Message message = Message.obtain();
        message.what = what;
        message.obj = msg;
        handler.sendMessage(message);
    }

//     EMQ 回调函数
    private class OnMessageCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable throwable) {
            Log.i("zzc","连接断开，可以做重连");
            try {
                mqttClient.reconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public synchronized void messageArrived(String topic, final MqttMessage message) throws Exception {
            if(topic.equals("testtopic/2")) {
                String path = System.currentTimeMillis()+".jpg";
                ThreadOutFile(message.getPayload(),path);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.i("zzc","deliveryComplete---------" + token.isComplete());
        }

    }

    public void ThreadShow(final String path) {
        new Thread(){
            @Override
            public void run() {
                synchronized (this) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Bitmap bitmap = BitmapFactory.decodeStream(openFileInput(path));
                                image.setImageBitmap(bitmap);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }.start();
    }

    public void ThreadOutFile(final byte[] bytes, final String path) {
        new Thread(){
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        FileOutputStream fileOutputStream = openFileOutput(path,MODE_PRIVATE);
                        fileOutputStream.write(bytes);
                        fileOutputStream.flush();
                        ThreadShow(path);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

//    public void ThreadOutFile(final byte[] bytes) {
//        new Thread(){
//            @Override
//            public void run() {
//                synchronized (this) {
//                    Log.i("hr","写入第 count="+count);
//                    FileOutputStream outputStream = null;
//                    try {
//                        byteArrayOutputStream.write(bytes);
//                        if(count%5==0 && count != 0) {
//                            outputStream = new FileOutputStream(fileVedio);
//                            outputStream.write(byteArrayOutputStream.toByteArray());
//                            byteArrayOutputStream.close();
//                            outputStream.flush();
//                            outputStream.close();
//                        }
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        Log.e("zzc","写入失败！！");
//                    }
//                }
//            }
//        }.start();
//    }

//    public void ShowPlay() {
//        new Thread(){
//            @Override
//            public void run() {
//                    synchronized (this) {
//                        Log.i("hr","显示第 count="+count);
//                        count++;
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if((count-1)%10 == 0 && (count-1)!=0) {
//                                    surface_view.setVideoPath(String.valueOf(fileVedio));
//                                    surface_view.start();
//                                }
//                            }
//                        });
//                    }
//                }
//        }.start();
//    }

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