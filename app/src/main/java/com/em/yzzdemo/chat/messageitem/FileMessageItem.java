package com.em.yzzdemo.chat.messageitem;

import android.content.Context;

import com.em.yzzdemo.chat.ChatMessageAdapter;
import com.hyphenate.chat.EMMessage;

/**
 * Created by Geri on 2016/12/8.
 */
public class FileMessageItem extends MessageItem {
    public FileMessageItem(Context context, ChatMessageAdapter adapter, int viewType) {
        super(context,adapter,viewType);
    }

    @Override
    protected void onItemLongClick() {

    }

    @Override
    protected void onInflateView() {

    }

    @Override
    public void onSetupView(EMMessage message) {

    }
}
