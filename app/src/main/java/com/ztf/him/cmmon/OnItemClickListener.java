package com.ztf.him.cmmon;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.hyphenate.chat.EMMessage;

import java.util.Map;

public interface OnItemClickListener {
    void onItemClick(RecyclerView parent, View view, int position, EMMessage data);
}
