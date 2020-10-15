package com.ztf.him.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMVoiceMessageBody;
import com.hyphenate.chat.adapter.message.EMAMessage;
import com.hyphenate.exceptions.EMServiceNotReadyException;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.ztf.him.R;
import com.ztf.him.adapter.MainAdapter;
import com.ztf.him.app.App;
import com.ztf.him.bean.TokenBean;
import com.ztf.him.cmmon.OnItemClickListener;
import com.ztf.him.cmmon.Player;
import com.ztf.him.utils.LogUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import io.rong.callkit.RongCallAction;
import io.rong.callkit.RongCallModule;
import io.rong.callkit.RongVoIPIntent;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

public class MainActivity extends AppCompatActivity {
    public static String name = "ztf147";
    private MainAdapter mAdapter;
    private Button sendText, sendVideo, sendAudio, callVideo;
    private List<EMMessage> mData = new ArrayList<>();
    private boolean isLogin = false;
    private String filePath;
    private String token;

    /**
     * 消息监听
     */
    EMMessageListener msgListener = new EMMessageListener() {

        @Override
        public void onMessageReceived(List<EMMessage> messages) {
            //收到消息
            mData.addAll(messages);
            runOnUiThread(() -> mAdapter.notifyDataSetChanged());
        }

        @Override
        public void onCmdMessageReceived(List<EMMessage> messages) {
            //收到透传消息
        }

        @Override
        public void onMessageRead(List<EMMessage> messages) {
            //收到已读回执
        }

        @Override
        public void onMessageDelivered(List<EMMessage> message) {
            //收到已送达回执
        }

        @Override
        public void onMessageRecalled(List<EMMessage> messages) {
            //消息被撤回
        }

        @Override
        public void onMessageChanged(EMMessage message, Object change) {
            //消息状态变动
        }
    };

    /**
     * 创建
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RongCallModule rongCallModule = new RongCallModule();
        rongCallModule.onCreate(this);
        rongCallModule.onViewCreated();
        initView();
        login();
        registerListener();
        initEvent();
    }

    /**
     * 事件
     */
    private void initEvent() {
        sendText.setOnClickListener(v -> {
            if (isLogin) {
                sendTextMessage();
            }
        });
        sendVideo.setOnClickListener(v -> {
            if (isLogin) {
                sendVideoMessage();
            }
        });
        sendAudio.setOnClickListener(v -> {
            if (isLogin) {
                sendAudioForUser(v);
            }
        });
        mAdapter.setOnItemClickListener((parent, view, position, data) -> {
            if (data.getType() == EMMessage.Type.VOICE) {
                EMVoiceMessageBody voiceBody = (EMVoiceMessageBody) data.getBody();
                //获取语音文件在服务器的地址
                String voiceRemoteUrl = voiceBody.getRemoteUrl();
                Player player = new Player(voiceRemoteUrl);
                player.play();
            }
        });
        callVideo.setOnClickListener(v -> {
            if (isLogin) {
                callVideo(v);
            }
        });
    }

    /**
     * 发送视频通话
     *
     * @param v 当前视图
     */
    private void callVideo(View v) {
        Intent intent = new Intent(RongVoIPIntent.RONG_INTENT_ACTION_VOIP_SINGLEVIDEO);
        intent.putExtra("conversationType", Conversation.ConversationType.PRIVATE.getName().toLowerCase(Locale.US));
        intent.putExtra("targetId", getToChatUserName());
        intent.putExtra("callAction", RongCallAction.ACTION_OUTGOING_CALL.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.setPackage(getPackageName());
        getApplicationContext().startActivity(intent);
        EMMessage message = EMMessage.createVideoSendMessage("", "", 0, getToChatUserName());
        mData.add(message);
    }

    /**
     * 注册融信
     */
    private void register() {
        //生成时间戳
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        //生成随机数  不超过 18 个字符。
        String nonce = String.valueOf(Math.random() * 1000000);
        //生成签名
        //Signature (数据签名)计算方法：将系统分配的 App Secret、Nonce (随机数)、Timestamp (时间戳)三个字符串按先后顺序拼接成一个字符串并进行 SHA1 哈希计算
        String signature = sha1("iGcnUwhblnQ5UP" + nonce + timestamp);

        OkGo.<String>post("https://api-cn.ronghub.com/user/getToken.json")
                .headers("App-Key", "k51hidwqkvqib")
                .headers("Timestamp", timestamp)
                .headers("Nonce", nonce)
                .headers("Signature", signature)
                .headers("Content-Type", "application/x-www-form-urlencoded")
                .params("userId", name)
                .params("name", name)
                .params("portraitUri", "")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        LogUtils.d("--onSuccess register" + response.body());
                        if (response.code() == 200) {
                            Gson gson = new Gson();
                            TokenBean tokenBean = gson.fromJson(response.body(), TokenBean.class);
                            token = tokenBean.getToken();
                            rxLogin();
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        LogUtils.d("--onError register" + response.message());
                    }
                });
    }

    /**
     * 语音通话
     *
     * @param view 当前视图
     */
    public void startTalk(View view) {
        Intent intent = new Intent(RongVoIPIntent.RONG_INTENT_ACTION_VOIP_SINGLEAUDIO);
        intent.putExtra("conversationType", Conversation.ConversationType.PRIVATE.getName().toLowerCase());
        intent.putExtra("targetId", getToChatUserName());
        intent.putExtra("callAction", RongCallAction.ACTION_OUTGOING_CALL.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(getPackageName());
        getApplicationContext().startActivity(intent);
//        RongCallKit.startSingleCall(this, "002", RongCallKit.CallMediaType.CALL_MEDIA_TYPE_VIDEO);
    }

    /**
     * 登录融信
     */
    private void rxLogin() {
        if (getApplicationInfo().packageName.equals(App.getCurProcessName(getApplicationContext()))) {
            RongIM.connect(token, new RongIMClient.ConnectCallback() {

                @Override
                public void onSuccess(String s) {
                    LogUtils.d("--onSuccess" + s);
                    Toast.makeText(MainActivity.this, "成功", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(RongIMClient.ConnectionErrorCode connectionErrorCode) {

                }

                @Override
                public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {

                }

            });
        }
    }

    /**
     * 签名
     *
     * @param data 将系统分配的 App Secret、Nonce (随机数)、Timestamp (时间戳)三个字符串按先后顺序拼接成一个字符串并进行 SHA1 哈希计算
     * @return 签名数据
     */
    private static String sha1(String data) {
        StringBuilder buf = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(data.getBytes());
            byte[] bits = md.digest();
            for (int bit : bits) {
                int a = bit;
                if (a < 0) a += 256;
                if (a < 16) buf.append("0");
                buf.append(Integer.toHexString(a));
            }
        } catch (Exception ignored) {
        }
        return buf.toString();
    }

    /**
     * 发送语音通话
     *
     * @param v 当前视图
     */
    private void sendAudioForUser(View v) {
        startTalk(v);
    }

    /**
     * 发送语音
     */
    private void sendVideoMessage() {
        filePath = Environment.getExternalStorageDirectory() + "/audio.wav";
        int color = getResources().getColor(R.color.colorPrimaryDark);
        int requestCode = 0;
        AndroidAudioRecorder.with(this)
                .setFilePath(filePath)
                .setColor(color)
                .setRequestCode(requestCode)
                .record();
    }

    /**
     * activity执行结果
     * @param requestCode 状态码
     * @param resultCode 结果码
     * @param data 数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                Uri uri = Uri.parse(filePath);
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(this, uri);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int duration = mediaPlayer.getDuration();
                EMMessage message = EMMessage.createVoiceSendMessage(uri, duration, getToChatUserName());
                EMClient.getInstance().chatManager().sendMessage(message);
                mData.add(message);
                mAdapter.notifyDataSetChanged();
            } else if (resultCode == RESULT_CANCELED) {
                // Oops! User has canceled the recording
                LogUtils.d("error");
            }
        }
    }

    /**
     * 初始化组件，及是否打开log(测试打开，上线关闭)
     */
    private void initView() {
        App.setDebug(false);
        callVideo = findViewById(R.id.call_video);
        RecyclerView mainRv = findViewById(R.id.main_rv);
        mAdapter = new MainAdapter(this, mData, mainRv);
        mainRv.setAdapter(mAdapter);
        mainRv.setLayoutManager(new LinearLayoutManager(this));
        sendText = findViewById(R.id.send_text);
        sendVideo = findViewById(R.id.send_video);
        sendAudio = findViewById(R.id.send_audio);
    }

    /**
     * 注册监听
     */
    private void registerListener() {
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
    }

    /**
     * 发送文本信息
     */
    private void sendTextMessage() {
        String content = "hello";
        String toChatUsername = getToChatUserName();
        //创建一条文本消息，content为消息文字内容，toChatUsername为对方用户或者群聊的id，后文皆是如此
        EMMessage message = EMMessage.createTxtSendMessage(content, toChatUsername);
        //发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
        mData.add(message);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 获取接收方
     *
     * @return 接收方用户名
     */
    private String getToChatUserName() {
        String toChatUsername;
        if (name.equals("ztf123")) {
            toChatUsername = "ztf147";
        } else {
            toChatUsername = "ztf123";
        }
        return toChatUsername;
    }

    /**
     * 登录
     */
    private void login() {
        register();
        EMClient.getInstance().login(name, "123", new EMCallBack() {//回调
            @Override
            public void onSuccess() {
                EMClient.getInstance().groupManager().loadAllGroups();
                EMClient.getInstance().chatManager().loadAllConversations();
                LogUtils.d("登录聊天服务器成功！" + name);
                isLogin = true;
            }

            @Override
            public void onProgress(int progress, String status) {

            }

            @Override
            public void onError(int code, String message) {
                LogUtils.d("登录聊天服务器失败！" + message);
            }
        });
    }

    /**
     * 销毁，移除监听，退出登录
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
        EMClient.getInstance().logout(true);
    }
}