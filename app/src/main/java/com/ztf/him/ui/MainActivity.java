package com.ztf.him.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMVoiceMessageBody;
import com.hyphenate.exceptions.HyphenateException;
import com.ztf.him.R;
import com.ztf.him.adapter.MainAdapter;
import com.ztf.him.cmmon.Player;
import com.ztf.him.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import io.rong.callkit.RongCallAction;
import io.rong.callkit.RongCallModule;
import io.rong.callkit.RongVoIPIntent;
import io.rong.imlib.model.Conversation;

public class MainActivity extends AppCompatActivity {
    private MainAdapter mAdapter;
    private Button sendText, sendVideo, sendAudio, callVideo;
    private List<EMMessage> mData = new ArrayList<>();
    private String filePath;
    private EditText nameInput, msgInput;
    private RecyclerView mainRv;

    /**
     * 消息监听
     */
    EMMessageListener msgListener = new EMMessageListener() {

        @Override
        public void onMessageReceived(List<EMMessage> messages) {
            String from = messages.get(0).getFrom();
            LogUtils.d("发送端" + from);
            //收到消息
            mData.addAll(messages);
            runOnUiThread(() -> {
                mAdapter.notifyDataSetChanged();
                movePosition();
                nameInput.setText(from);
            });
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
     *
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
        registerListener();
        initEvent();
    }

    /**
     * 事件
     */
    private void initEvent() {
        sendText.setOnClickListener(v -> {
            sendTextMessage();
        });
        sendVideo.setOnClickListener(v -> {
            sendVideoMessage();
        });
        sendAudio.setOnClickListener(this::sendAudioForUser);
        mAdapter.setOnItemClickListener((parent, view, position, data) -> {
            if (data.getType() == EMMessage.Type.VOICE) {
                EMVoiceMessageBody voiceBody = (EMVoiceMessageBody) data.getBody();
                //获取语音文件在服务器的地址
                String voiceRemoteUrl = voiceBody.getRemoteUrl();
                Player player = new Player(voiceRemoteUrl);
                player.play();
            }
        });
        callVideo.setOnClickListener(this::callVideo);
    }

    /**
     * 发送视频通话
     *
     * @param v 当前视图
     */
    private void callVideo(View v) {
        String name = getToChatUserName();
        if (name != null) {
            Intent intent = new Intent(RongVoIPIntent.RONG_INTENT_ACTION_VOIP_SINGLEVIDEO);
            intent.putExtra("conversationType", Conversation.ConversationType.PRIVATE.getName().toLowerCase(Locale.US));
            intent.putExtra("targetId", name);
            intent.putExtra("callAction", RongCallAction.ACTION_OUTGOING_CALL.getName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(getPackageName());
            getApplicationContext().startActivity(intent);
        }
    }

    /**
     * 语音通话
     *
     * @param view 当前视图
     */
    public void startTalk(View view) {
        String name = getToChatUserName();
        if (name != null) {
            Intent intent = new Intent(RongVoIPIntent.RONG_INTENT_ACTION_VOIP_SINGLEAUDIO);
            intent.putExtra("conversationType", Conversation.ConversationType.PRIVATE.getName().toLowerCase());
            intent.putExtra("targetId", name);
            intent.putExtra("callAction", RongCallAction.ACTION_OUTGOING_CALL.getName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(getPackageName());
            getApplicationContext().startActivity(intent);
        }
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
        String name = getToChatUserName();
        if (name != null) {
            filePath = Environment.getExternalStorageDirectory() + "/audio.wav";
            int color = getResources().getColor(R.color.colorPrimaryDark);
            int requestCode = 0;
            AndroidAudioRecorder.with(this)
                    .setFilePath(filePath)
                    .setColor(color)
                    .setRequestCode(requestCode)
                    .record();
        }
    }

    /**
     * activity执行结果
     *
     * @param requestCode 状态码
     * @param resultCode  结果码
     * @param data        数据
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
                movePosition();
            } else if (resultCode == RESULT_CANCELED) {
                // Oops! User has canceled the recording
                LogUtils.d("error");
            }
        }
    }

    private void movePosition(){
        mainRv.scrollToPosition(mAdapter.getItemCount()-1);
    }

    /**
     * 初始化组件
     */
    private void initView() {
        msgInput = (EditText) findViewById(R.id.msg_input);
        nameInput = (EditText) findViewById(R.id.name_input);
        callVideo = findViewById(R.id.call_video);
        mainRv = findViewById(R.id.main_rv);
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
        String name = getToChatUserName();
        if (name != null) {
            String content = msgInput.getText().toString();
            if (content.isEmpty()){
                Toast.makeText(this, "不能发送空消息", Toast.LENGTH_SHORT).show();
            }else {
                //创建一条文本消息，content为消息文字内容，toChatUsername为对方用户或者群聊的id，后文皆是如此
                EMMessage message = EMMessage.createTxtSendMessage(content, name);
                //发送消息
                EMClient.getInstance().chatManager().sendMessage(message);
                msgInput.setText("");
                mData.add(message);
                mAdapter.notifyDataSetChanged();
                movePosition();
            }
        }
    }

    /**
     * 获取接收方
     *
     * @return 接收方用户名
     */
    private String getToChatUserName() {
        String toChatUsername;
        if (nameInput.getText().toString().isEmpty()) {
            return null;
        } else {
            toChatUsername = nameInput.getText().toString();
            LogUtils.d(toChatUsername);
            return toChatUsername;
        }
    }

    /**
     * 销毁，移除监听
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
    }
}