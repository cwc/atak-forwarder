package com.paulmandal.atak.forwarder.comm.commhardware;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.CallSuper;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.UpdateChannelCommand;
import com.paulmandal.atak.forwarder.channel.UserInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class CommHardware {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + CommHardware.class.getSimpleName();

    protected static final String BCAST_MARKER = "ATAKBCAST";

    private static final int DELAY_BETWEEN_POLLING_FOR_MESSAGES_MS = Config.DELAY_BETWEEN_POLLING_FOR_MESSAGES_MS;

    public enum ConnectionState {
        UNPAIRED,
        DISCONNECTED,
        CONNECTED
    }

    public interface MessageListener {
        void onMessageReceived(byte[] message);
    }

    public interface ConnectionStateListener {
        void onConnectionStateChanged(ConnectionState connectionState);
    }

    private Handler mHandler;

    private final CommandQueue mCommandQueue;
    private QueuedCommandFactory mQueuedCommandFactory;

    private List<ConnectionStateListener> mConnectionStateListeners = new CopyOnWriteArrayList<>();
    private List<MessageListener> mMessageListeners = new CopyOnWriteArrayList<>();

    private Thread mMessageWorkerThread;

    private ConnectionState mConnectionState;
    private boolean mDestroyed = false;

    private UserInfo mSelfInfo;

    public CommHardware(Handler uiThreadHandler,
                        CommandQueue commandQueue,
                        QueuedCommandFactory queuedCommandFactory,
                        UserInfo selfInfo) {
        mHandler = uiThreadHandler;
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mSelfInfo = selfInfo;

        startWorkerThreads();
    }

    /**
     * External API
     */
    public void broadcastDiscoveryMessage() {
        broadcastDiscoveryMessage(false);
    }

    public void updateChannelSettings(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        mCommandQueue.queueCommand(mQueuedCommandFactory.createUpdateChannelCommand(channelName, psk, modemConfig));
    }

    public void connect() {
        if (mConnectionState == ConnectionState.CONNECTED) {
            Log.d(TAG, "connect: already connected");
            return;
        }

        mCommandQueue.queueCommand(mQueuedCommandFactory.createScanForCommDeviceCommand());
    }

    @CallSuper
    public void destroy() {
        mDestroyed = true;
    }
    /**
     * Listener Management
     */
    public void addMessageListener(MessageListener listener) {
        mMessageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        mMessageListeners.remove(listener);
    }

    public void addConnectionStateListener(ConnectionStateListener listener) {
        mConnectionStateListeners.add(listener);
    }

    public void removeConnectionStateListener(ConnectionStateListener listener) {
        mConnectionStateListeners.remove(listener);
    }

    /**
     * Internal API
     */
    @CallSuper
    protected void startWorkerThreads() {
        mMessageWorkerThread = new Thread(() -> {
            while (!mDestroyed) {
                sleepForDelay(DELAY_BETWEEN_POLLING_FOR_MESSAGES_MS);

                QueuedCommand queuedCommand = mCommandQueue.popHighestPriorityCommand(mConnectionState == ConnectionState.CONNECTED);

                if (queuedCommand == null) {
                    continue;
                }

                switch (queuedCommand.commandType) {
                    case SCAN_FOR_COMM_DEVICE:
                        handleScanForCommDevice();
                        break;
                    case BROADCAST_DISCOVERY_MSG:
                        handleBroadcastDiscoveryMessage((BroadcastDiscoveryCommand) queuedCommand);
                        break;
                    case UPDATE_CHANNEL:
                        handleUpdateChannel((UpdateChannelCommand) queuedCommand);
                        break;
                    case SEND_TO_CHANNEL:
                    case SEND_TO_INDIVIDUAL:
                        handleSendMessage((SendMessageCommand) queuedCommand);
                        break;
                }
            }
        });
        mMessageWorkerThread.setName("CommHardware.MessageWorker");
        mMessageWorkerThread.start();
    }

    protected boolean isDestroyed() {
        return mDestroyed;
    }

    protected void setConnectionState(ConnectionState connectionState) {
        mConnectionState = connectionState;
    }

    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    protected UserInfo getSelfInfo() {
        return mSelfInfo;
    }

    protected void queueCommand(QueuedCommand queuedCommand) {
        mCommandQueue.queueCommand(queuedCommand);
    }

    protected void broadcastDiscoveryMessage(boolean initialDiscoveryMessage) {
        String broadcastData = BCAST_MARKER + "," + getSelfInfo().meshId + "," + getSelfInfo().atakUid + "," + getSelfInfo().callsign + "," + (initialDiscoveryMessage ? 1 : 0);
        mCommandQueue.queueCommand(mQueuedCommandFactory.createBroadcastDiscoveryCommand(broadcastData.getBytes()));
    }

    protected void notifyMessageListeners(byte[] message) {
        for (MessageListener listener : mMessageListeners) {
            mHandler.post(() -> listener.onMessageReceived(message));
        }
    }

    protected void notifyConnectionStateListeners(ConnectionState connectionState) {
        for (ConnectionStateListener connectionStateListener : mConnectionStateListeners) {
            mHandler.post(() -> connectionStateListener.onConnectionStateChanged(connectionState));
        }
    }
    /**
     * Utils
     */
    protected void sleepForDelay(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * For subclasses to implement
     */
    protected abstract void handleScanForCommDevice();
    protected abstract void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand);
    protected abstract void handleUpdateChannel(UpdateChannelCommand updateChannelCommand);
    protected abstract void handleSendMessage(SendMessageCommand sendMessageCommand);
}
