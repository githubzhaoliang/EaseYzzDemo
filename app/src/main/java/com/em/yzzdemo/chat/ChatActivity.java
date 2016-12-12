package com.em.yzzdemo.chat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.em.yzzdemo.BaseActivity;
import com.em.yzzdemo.R;
import com.em.yzzdemo.utils.ConstantsUtils;
import com.em.yzzdemo.utils.DateUtil;
import com.em.yzzdemo.utils.FileUtil;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Geri on 2016/11/27.
 */

public class ChatActivity extends BaseActivity implements View.OnClickListener {
    @BindView(R.id.chat_toolbar)
    Toolbar toolbar;
    @BindView(R.id.chat_recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.iv_emoji)
    ImageView emojiView;
    @BindView(R.id.ed_context)
    EditText mContextView;
    @BindView(R.id.iv_voice)
    ImageView mVoiceView;
    @BindView(R.id.iv_send)
    ImageView mSendView;
    private ChatMorePopWindow morePopWindow;
    private EMConversation.EMConversationType mConversationType;
    private EMConversation mConversation;
    private String chatId;
    // 是否发送原图
    private boolean isOrigin = true;
    private String id;
    private String userName;
    private EMGroup group;
    private EMMessageListener mMessageListener;
    private ChatMessageAdapter mAdapter;
    //图片
    private Uri mCameraImageUri = null;
    private File saveFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        mActivity = this;

        initView();

    }

    private void initView() {
        chatId = getIntent().getStringExtra(ConstantsUtils.CHAT_ID);
        mConversationType = (EMConversation.EMConversationType) getIntent().getExtras().get(ConstantsUtils.CHAT_TYPE);
        mConversation = EMClient.getInstance().chatManager().getConversation(chatId, mConversationType, true);
        id = mConversation.conversationId();
        userName = mConversation.getUserName();
        //如果是群组设置群组名称，否则设置单聊名称
        if (mConversation.getType().equals(EMConversation.EMConversationType.GroupChat)) {
            group = EMClient.getInstance().groupManager().getGroup(id);
            if(mConversation.isGroup() && group != null){
                //设置title
                toolbar.setTitle(group.getGroupName());
            }
        } else {
            toolbar.setTitle(userName);
        }
        //声明toolbar
        setSupportActionBar(toolbar);
        //设置back图标
        toolbar.setNavigationIcon(R.mipmap.arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        //toolbar字体颜色
        toolbar.setTitleTextColor(0xffffffff);
        toolbar.setOnMenuItemClickListener(onMenuItemClick);
        //输入框的监听
        mContextView.addTextChangedListener(textWatcher);
        mSendView.setOnClickListener(this);
        //设置消息监听
        setMessageListener();

        mAdapter = new ChatMessageAdapter(mActivity,id);
        LinearLayoutManager manager = new LinearLayoutManager(mActivity);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(mAdapter);


    }



    //输入框的监听
    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String text = mContextView.getText().toString().trim();
            if(TextUtils.isEmpty(text)){
                mVoiceView.setVisibility(View.VISIBLE);
                mSendView.setVisibility(View.GONE);
            }else{
                mVoiceView.setVisibility(View.GONE);
                mSendView.setVisibility(View.VISIBLE);
            }

        }
    };
    //各种点击事件
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            //发送文本消息
            case R.id.iv_send:
                String text = mContextView.getText().toString().trim();
                if(TextUtils.isEmpty(text)){
                    Toast.makeText(this, "输入内容为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendTextMessage(text);
                mContextView.setText("");
                mAdapter.refreshMessageData();
                mAdapter.notifyDataSetChanged();
                break;
            //发送语音消息
            case R.id.iv_voice:

                break;
            //发送emoji表情
            case R.id.iv_emoji:

                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //设定菜单各按钮的点击动作
    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_call:
                    Toast.makeText(mActivity, "call", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.action_video:
                    Toast.makeText(mActivity, "video", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.action_more:
                    Toast.makeText(mActivity, "more", Toast.LENGTH_SHORT).show();
                    morePopWindow = new ChatMorePopWindow(mActivity, itemsOnClick);
                    morePopWindow.showPopupWindow(toolbar);
                    break;
            }
            return true;
        }
    };

    //为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            morePopWindow.dismiss();
//            morePopWindow.backgroundAlpha(mActivity,1f);
            switch (v.getId()) {
                case R.id.item_camera:
                    if (Build.VERSION.SDK_INT >= 23) {
                        int cameraPermission = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA);
                        if(cameraPermission != PackageManager.PERMISSION_GRANTED){
                            ActivityCompat.requestPermissions(mActivity,new String[]{Manifest.permission.CAMERA},
                                    ConstantsUtils.REQUEST_CODE_ASK_CAMERA);
                            return;
                        }else{
                            openCamera();
                        }
                    } else {
                        openCamera();
                    }
                    break;
                case R.id.item_picture:
                    openGallery();
                    break;
                case R.id.item_location:

                    break;
                case R.id.item_file:

                    break;
            }
        }

    };

    /**
     * 发送文本消息
     */
    private void sendTextMessage(String content) {
        // 创建一条文本消息
        EMMessage textMessage = EMMessage.createTxtSendMessage(content, id);
        sendMessage(textMessage);
    }


    //发送图片消息
    private void sendImageMessage(String path) {
        /**
         * 根据图片路径创建一条图片消息，需要三个参数，
         * path     图片路径
         * isOrigin 是否发送原图
         * mChatId  接收者
         */
        EMMessage imgMessage = EMMessage.createImageSendMessage(path, isOrigin, id);
        sendMessage(imgMessage);
    }

    /**
     * 发送语音消息
     *
     * @param path 语音文件的路径
     */
    private void sendVoiceMessage(String path, int time) {
        EMMessage voiceMessage = EMMessage.createVoiceSendMessage(path, time, id);
        sendMessage(voiceMessage);

    }

    /**
     * 发送位置信息
     */
    private void sendLocationMessage(double latitude, double longitude, String locationAddress) {
        //latitude为纬度，longitude为经度，locationAddress为具体位置内容
        EMMessage locationmessage = EMMessage.createLocationSendMessage(latitude, longitude, locationAddress, id);
        sendMessage(locationmessage);

    }


    /**
     * 最终调用发送信息方法
     *
     * @param message 需要发送的消息
     */
    private void sendMessage(final EMMessage message) {

        // 设置不同的会话类型
        if (mConversationType == EMConversation.EMConversationType.Chat) {
            message.setChatType(EMMessage.ChatType.Chat);
        } else if (mConversationType == EMConversation.EMConversationType.GroupChat) {
            message.setChatType(EMMessage.ChatType.GroupChat);
        } else if (mConversationType == EMConversation.EMConversationType.ChatRoom) {
            message.setChatType(EMMessage.ChatType.ChatRoom);
        }
        /**
         *  调用sdk的消息发送方法发送消息，发送消息时要尽早的设置消息监听，防止消息状态已经回调，
         *  但是自己没有注册监听，导致检测不到消息状态的变化
         *  所以这里在发送之前先设置消息的状态回调
         */
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                Log.i("message", "发送成功");
            }

            @Override
            public void onError(final int i, final String s) {
                Log.i("message", "发送失败");
            }

            @Override
            public void onProgress(int i, String s) {

            }
        });
        // 发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
    }

    /**
     * 打开相机去拍摄图片发送
     */
    private void openCamera() {
        // 定义拍照后图片保存的路径以及文件名
        String imagePath =
                FileUtil.getDCIM() + "IMG" + DateUtil.getDateTimeNoSpacing() + ".jpg";
        // 激活相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断存储卡是否可以用，可用进行存储
        if (FileUtil.hasSdcard()) {
            // 根据文件路径解析成Uri
            mCameraImageUri = Uri.fromFile(new File(imagePath));
            // 将Uri设置为媒体输出的目标，目的就是为了等下拍照保存在自己设定的路径
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraImageUri);
        }
        // 根据 Intent 启动一个带有返回值的 Activity，这里启动的就是相机，返回选择图片的地址
        mActivity.startActivityForResult(intent, ConstantsUtils.REQUEST_CODE_CAMERA);
    }

    /**
     * 打开系统图库，去进行选择图片
     */
    private void openGallery() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            // 设置intent要选择的文件类型，这里用设置为image 图片类型
            intent.setType("image/*");
        } else {
            // 在Android 系统版本大于19 上，调用系统选择图片方法稍有不同
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        mActivity.startActivityForResult(intent, ConstantsUtils.REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case ConstantsUtils.REQUEST_CODE_CAMERA:
                // 相机拍摄的图片
                sendImageMessage(mCameraImageUri.getPath());
                mAdapter.refreshMessageData();
                mAdapter.notifyDataSetChanged();
                break;
            case ConstantsUtils.REQUEST_CODE_GALLERY:
                // 图库选择的图片，选择图片后返回获取返回的图片路径，然后发送图片
                if (data != null) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumns = {MediaStore.Images.Media.DATA};
                    Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                    c.moveToFirst();
                    int columnIndex = c.getColumnIndex(filePathColumns[0]);
                    String image = c.getString(columnIndex);
                    c.close();
                    sendImageMessage(image);
                    mAdapter.refreshMessageData();
                    mAdapter.notifyDataSetChanged();
                }
                break;

//            case ConstantsUtils.REQUEST_CODE_LOCATION:
//                double latitude = data.getDoubleExtra("latitude", 0);
//                double longitude = data.getDoubleExtra("longitude", 0);
//                String locationAddress = data.getStringExtra("addrStr");
//                if (locationAddress != null && !locationAddress.equals("")) {
//                    sendLocationMessage(latitude, longitude, locationAddress);
//                    mAdapter.refreshMessageData();
//                    mAdapter.notifyDataSetChanged();
//                } else {
//                    Toast.makeText(ChatActivity.this, "unable_to_get_loaction", Toast.LENGTH_SHORT).show();
//                }
//                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ConstantsUtils.REQUEST_CODE_ASK_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    openCamera();
                } else {
                    // Permission Denied
                    Toast.makeText(mActivity, "CALL_PHONE Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 注册消息监听
     */
    @Override
    protected void onStart() {
        super.onStart();
        EMClient.getInstance().chatManager().addMessageListener(mMessageListener);
    }
    /**
     * 接收消息的监听
     */
    private void setMessageListener() {
        mMessageListener = new EMMessageListener() {
            @Override
            public void onMessageReceived(List<EMMessage> list) {
                boolean isNotify = false;
                // 循环遍历当前收到的消息
                for (EMMessage message : list) {
                    String username = "";
                    if (mConversationType == EMConversation.EMConversationType.Chat) {
                        username = message.getFrom();
                    } else {
                        username = message.getTo();
                    }
                    // 判断消息是否是当前会话的消息
                    if (chatId.equals(username)) {
                        mAdapter.refreshMessageData();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });

                    }
                }
            }
            @Override
            public void onCmdMessageReceived(List<EMMessage> list) {

            }

            @Override
            public void onMessageReadAckReceived(List<EMMessage> list) {

            }

            @Override
            public void onMessageDeliveryAckReceived(List<EMMessage> list) {

            }

            @Override
            public void onMessageChanged(EMMessage emMessage, Object o) {

            }
        };
    }

    /**
     * 移除消监听
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatManager().removeMessageListener(mMessageListener);
    }
}
