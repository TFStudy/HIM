package com.ztf.him.adapter;

import android.content.Context;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hyphenate.chat.EMMessage;
import com.ztf.him.R;
import com.ztf.him.cmmon.OnItemClickListener;
import com.ztf.him.ui.MainActivity;

import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private List<EMMessage> mDatas;
    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SEND = 1;
    private RecyclerView rv;

    private OnItemClickListener onItemClickListener;//声明一下这个接口

    //提供setter方法
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public MainAdapter(Context context, List<EMMessage> datas, RecyclerView rv) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        mDatas = datas;
        this.rv = rv;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SEND) {
            view = mLayoutInflater.inflate(R.layout.item_chat_send, parent, false);
            view.setOnClickListener(this);
            return new ChatSendViewHolder(view);
        } else {
            view = mLayoutInflater.inflate(R.layout.item_chat_receive, parent, false);
            view.setOnClickListener(this);
            return new ChatReceiveViewHolder(view);
        }

    }

    @Override
    public int getItemViewType(int position) {
        EMMessage message = mDatas.get(position);
        if (message.getFrom().equals(MainActivity.name)) {
            return TYPE_SEND;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        EMMessage chatMsg = mDatas.get(position);
        /**
         * 判断是否是文本信息
         */
        if(!(chatMsg.getType() == EMMessage.Type.VOICE)){
            String s = getString(chatMsg);
            if (holder instanceof ChatSendViewHolder) {
                ((ChatSendViewHolder) holder).tv_send.setVisibility(View.VISIBLE);
                ((ChatSendViewHolder) holder).send_sound.setVisibility(View.GONE);
                ((ChatSendViewHolder) holder).tv_send.setText(s);
            }
            if (holder instanceof ChatReceiveViewHolder) {
                ((ChatReceiveViewHolder) holder).tv_receive.setVisibility(View.VISIBLE);
                ((ChatReceiveViewHolder) holder).receive_sound.setVisibility(View.GONE);
                ((ChatReceiveViewHolder) holder).tv_receive.setText(s);
            }
        }

    }

    private String getString(EMMessage chatMsg) {
        String msg = chatMsg.getBody().toString().split(":")[1];
        StringBuilder builder = new StringBuilder(msg);
        StringBuilder newStr = builder.replace(0, 1, " ");
        StringBuilder content = newStr.replace(newStr.length() - 1, newStr.length(), " ");
        return content.toString().replaceAll(" +", "");
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public void onClick(View v) {
        //根据RecyclerView获得当前View的位置
        int position = rv.getChildAdapterPosition(v);
        //程序执行到此，会去执行具体实现的onItemClick()方法
        if (onItemClickListener!=null){
            onItemClickListener.onItemClick(rv,v,position,mDatas.get(position));
        }
    }

    static class ChatSendViewHolder extends RecyclerView.ViewHolder {
        TextView tv_send;
        ImageView send_sound;

        ChatSendViewHolder(View view) {
            super(view);
            tv_send = (TextView) view.findViewById(R.id.tv_chat);
            send_sound = (ImageView) view.findViewById(R.id.send_sound);
        }
    }

    static class ChatReceiveViewHolder extends RecyclerView.ViewHolder {
        TextView tv_receive;
        ImageView receive_sound;

        ChatReceiveViewHolder(View view) {
            super(view);
            tv_receive = (TextView) view.findViewById(R.id.tv_chat);
            receive_sound = (ImageView) view.findViewById(R.id.receive_sound);
        }
    }
}
