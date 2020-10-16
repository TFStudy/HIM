package com.ztf.him.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.ztf.him.R;
import com.ztf.him.app.App;
import com.ztf.him.bean.TokenBean;
import com.ztf.him.utils.LogUtils;

import java.security.MessageDigest;
import java.util.Objects;

import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;

public class LoginActivity extends AppCompatActivity {

    public static String name;
    private EditText accountInput;
    private EditText passwordInput;
    private Button btnLogin;
    private Button btnRegister;
    private ImageView ootLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        App.setDebug(false);
        initView();
        initEvent();
    }

    private String getText() {
        String name = accountInput.getText().toString();
        String word = passwordInput.getText().toString();
        if (name.isEmpty()) {
            return null;
        }
        if (word.isEmpty()) {
            return null;
        }
        return name + ":" + word;
    }

    private void initEvent() {
        accountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                name = accountInput.getText().toString();
            }
        });
        ootLogin.setOnClickListener(v -> {
            outLogin();
        });
        btnLogin.setOnClickListener(v -> {
            String text = getText();
            LogUtils.d(text);
            if (text != null) {
                LogUtils.d("login");
                rxRegister(text);
                login(text);
            }
        });
        btnRegister.setOnClickListener(v -> {
            String text = getText();
            if (text != null) {
                LogUtils.d("register");
                register(text);
            }
        });
    }

    /**
     * 退出登录用户
     */
    private void outLogin() {
        new Thread(() -> {
            EMClient.getInstance().logout(true, new EMCallBack() {

                @Override
                public void onSuccess() {
                    // TODO Auto-generated method stub
                    LogUtils.d("用户退出成功");
                }

                @Override
                public void onProgress(int progress, String status) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onError(int code, String message) {
                    // TODO Auto-generated method stub
                    LogUtils.d("用户退出失败");
                }
            });
        }).start();
    }

    private void register(String text) {
        String[] split = text.split(":");
        new Thread(() -> {
            try {
                EMClient.getInstance().createAccount(split[0], split[1]);
                LogUtils.d("register success");
                Toast.makeText(LoginActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
            } catch (HyphenateException e) {
                LogUtils.d(e.getMessage());
                if (Objects.equals(e.getMessage(), "User already exist")) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "该账号已注册，请直接登录", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    /**
     * 登录
     */
    private void login(String text) {
        String[] split = text.split(":");
        EMClient.getInstance().login(split[0], split[1], new EMCallBack() {//回调
            @Override
            public void onSuccess() {
                EMClient.getInstance().groupManager().loadAllGroups();
                EMClient.getInstance().chatManager().loadAllConversations();
                LogUtils.d("登录聊天服务器成功！" + split[0]);
                rxLogin();
            }

            @Override
            public void onProgress(int progress, String status) {

            }

            @Override
            public void onError(int code, String message) {
                LogUtils.d("登录聊天服务器失败！" + message);
                if (Objects.equals(message, "User is already login")) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "用户已登录", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "用户不存在，请注册", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
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
     * 登录融信
     */
    private void rxLogin() {
        SharedPreferences sharedPreferences = getSharedPreferences("data", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", "");
        if (getApplicationInfo().packageName.equals(App.getCurProcessName(getApplicationContext()))) {
            RongIM.connect(token, new RongIMClient.ConnectCallback() {
                @Override
                public void onSuccess(String s) {
                    LogUtils.d("--onSuccess" + s);
                    LogUtils.d("登录成功");
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(RongIMClient.ConnectionErrorCode connectionErrorCode) {
                    LogUtils.d("登录失败" + connectionErrorCode.getValue());
                }

                @Override
                public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {

                }

            });
        }
    }


    /**
     * 注册融信
     *
     * @param text
     */
    private void rxRegister(String text) {
        String name = text.split(":")[0];
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
                            String token = tokenBean.getToken();
                            SharedPreferences sharedPreferences = getSharedPreferences("data", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("token", token);
                            editor.apply();
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        LogUtils.d("--onError register" + response.message());
                    }
                });
    }

    private void initView() {
        ootLogin = (ImageView) findViewById(R.id.ootLogin);
        accountInput = (EditText) findViewById(R.id.account_input);
        passwordInput = (EditText) findViewById(R.id.password_input);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnRegister = (Button) findViewById(R.id.btn_register);
    }
}