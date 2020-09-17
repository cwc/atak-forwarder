package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;

import java.util.List;

public class StatusTabViewModel implements ChannelTracker.UpdateListener,
        CommandQueue.Listener,
        CommHardware.ConnectionStateListener,
        MeshtasticCommHardware.MessageAckNackListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + StatusTabViewModel.class.getSimpleName();

    private CommHardware mCommHardware;

    private MutableLiveData<List<UserInfo>> mUserInfoList = new MutableLiveData<>();
    private MutableLiveData<Integer> mMessageQueueSize = new MutableLiveData<>();
    private MutableLiveData<CommHardware.ConnectionState> mConnectionState = new MutableLiveData<>();
    private MutableLiveData<Integer> mTotalMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mErroredMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mDeliveredMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mErrorsInARow = new MutableLiveData<>();

    public StatusTabViewModel(ChannelTracker channelTracker,
                              MeshtasticCommHardware commHardware,
                              CommandQueue commandQueue) {
        mCommHardware = commHardware;

        mMessageQueueSize.setValue(0);
        mTotalMessages.setValue(0);
        mErroredMessages.setValue(0);
        mDeliveredMessages.setValue(0);
        mErrorsInARow.setValue(0);

        channelTracker.addUpdateListener(this);
        commandQueue.setListener(this);
        commHardware.addConnectionStateListener(this);
        commHardware.setMessageAckNackListener(this);
    }

    @Override
    public void onUpdated(List<UserInfo> userInfoList) {
        mUserInfoList.setValue(userInfoList);
    }

    @Override
    public void onMessageQueueSizeChanged(int size) {
        mMessageQueueSize.setValue(size);
    }

    @Override
    public void onConnectionStateChanged(CommHardware.ConnectionState connectionState) {
        mConnectionState.setValue(connectionState);
    }

    @NonNull
    public LiveData<List<UserInfo>> getUserInfoList() {
        return mUserInfoList;
    }

    @NonNull
    public LiveData<Integer> getMessageQueueSize() {
        return mMessageQueueSize;
    }

    @NonNull
    public LiveData<CommHardware.ConnectionState> getConnectionState() {
        return mConnectionState;
    }

    @NonNull
    public LiveData<Integer> getTotalMessage() {
        return mTotalMessages;
    }

    @NonNull
    public LiveData<Integer> getErroredMessages() {
        return mErroredMessages;
    }

    @NonNull
    public LiveData<Integer> getDeliveredMessages() {
        return mDeliveredMessages;
    }

    @NonNull
    public LiveData<Integer> getErrorsInARow() {
        return mErrorsInARow;
    }

    public void connect() {
        mCommHardware.connect();
    }

    public void broadcastDiscoveryMessage() {
        mCommHardware.broadcastDiscoveryMessage();
    }

    @Override
    public void onMessageAckNack(int msgId, boolean isAck) {
        mTotalMessages.setValue(mTotalMessages.getValue() + 1);
        if (isAck) {
            mErrorsInARow.setValue(0);
            mDeliveredMessages.setValue(mDeliveredMessages.getValue() + 1);
        } else {
            mErrorsInARow.setValue(mErrorsInARow.getValue() + 1);
            mErroredMessages.setValue(mErroredMessages.getValue() + 1);
        }
    }
}