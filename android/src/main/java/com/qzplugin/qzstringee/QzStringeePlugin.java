package com.qzplugin.qzstringee;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.common.StringeeAudioManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.listener.StatusListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CapacitorPlugin(name = "QzStringee")
public class QzStringeePlugin extends Plugin {
    private static Map<String, StringeeCall> activeCalls = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private StringeeAudioManager StaudioManager;
    private static final int ALL_PERMISSIONS_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WAKE_LOCK
    };
    private StringeeClient stringeeClient;
    private StringeeCall currentCall;

    public static void storeCall(StringeeCall call) {
        activeCalls.put(call.getCallId(), call);
    }

    public static StringeeCall getCallById(String callId) {
        return activeCalls.get(callId);
    }
    @Override
    public void load() {
        super.load();
        stringeeClient = new StringeeClient(getContext());



    }

    @PluginMethod
    public void getpermission(PluginCall call) {
        if (!hasAllPermissions()) {
            if (getActivity() != null) {
                ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS, ALL_PERMISSIONS_CODE);
            }
        } else {
            sendSuccessResponse(call, "All Permissions Already Granted");
        }
    }

    @PluginMethod
    public void getConfig(PluginCall call) {
        String token = call.getString("value");
        checkConnect(token);
        sendSuccessResponse(call, "Connected to Stringee");


    }

    @PluginMethod
    public void outgoingCall(PluginCall call) {
        if (stringeeClient == null || !stringeeClient.isConnected()) {
            call.reject("StringeeClient is not connected.");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(call.getString("value"));
            String callerUserId = jsonObject.getString("CALLER_USER_ID");
            String receiverUserId = jsonObject.getString("RECEIVER_USER_ID");

            currentCall = new StringeeCall(stringeeClient, callerUserId, receiverUserId);
            currentCall.enableVideo(false);
            setupCallListeners(currentCall);
            getActivity().runOnUiThread(() -> {
                if (StaudioManager == null) {
                    StaudioManager = StringeeAudioManager.create(getContext());
                    StaudioManager.start(new StringeeAudioManager.AudioManagerEvents() {
                        @Override
                        public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice audioDevice, Set<StringeeAudioManager.AudioDevice> selectedAudioDevice) {
                            Log.d("Stringee", "Audio device changed: " + selectedAudioDevice);
                        }
                    });

                    // Delay setting speakerphone until AudioManager is fully started
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (StaudioManager != null) {
                            StaudioManager.setSpeakerphoneOn(true);
                        }
                    }, 500); // Small delay to ensure audio is properly routed
                }
            });
            currentCall.setVideoCall(false);

            currentCall.makeCall(new StatusListener() {
                @Override
                public void onSuccess() {
                    sendSuccessResponse(call, "Call initiated");
                }

                @Override
                public void onError(StringeeError error) {
                    call.reject("Call initiation failed: " + error.getMessage());
                }
            });
        } catch (JSONException e) {
            call.reject("Invalid JSON data");
        }
    }

    @PluginMethod
    public void answerCall(PluginCall call) {
        if (currentCall == null) {
            call.reject("No incoming call to answer.");

            return;
        }

        stopMediaPlayer();
        setupCallListeners(currentCall);
        // Ensure AudioManager is initialized on the main thread
        getActivity().runOnUiThread(() -> {
            if (StaudioManager == null) {
                StaudioManager = StringeeAudioManager.create(getContext());
                StaudioManager.start(new StringeeAudioManager.AudioManagerEvents() {
                    @Override
                    public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice audioDevice, Set<StringeeAudioManager.AudioDevice> selectedAudioDevice) {
                        Log.d("Stringee", "Audio device changed: " + selectedAudioDevice);
                    }
                });

                // Delay setting speakerphone until AudioManager is fully started
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (StaudioManager != null) {
                        StaudioManager.setSpeakerphoneOn(true);
                    }
                }, 500); // Small delay to ensure audio is properly routed
            }
        });
        currentCall.setVideoCall(false);
        currentCall.ringing(new StatusListener() {
            @Override
            public void onSuccess() {
                Log.d("Stringee", "Ringing signal sent successfully.");
            }

            @Override
            public void onError(StringeeError error) {
                Log.d("Stringee", "Failed to send ringing signal: " + error.getMessage());
            }
        });


        currentCall.answer(new StatusListener() {
            @Override
            public void onSuccess() {

                sendSuccessResponse(call, "Call answered successfully");
            }

            @Override
            public void onError(StringeeError error) {
                call.reject("Error answering call: " + error.getMessage());
            }
        });
    }

    @PluginMethod
    public void endCall(PluginCall call) {
        if (currentCall == null) {
            call.reject("No active call to end.");
            return;
        }
        stopMediaPlayer();

        currentCall.hangup(new StatusListener() {
            @Override
            public void onSuccess() {
                if (StaudioManager != null) {
                    StaudioManager.stop(); // Stop AudioManager after call ends
                    StaudioManager = null;
                }
                sendSuccessResponse(call, "Call ended successfully");
                currentCall = null;
            }

            @Override
            public void onError(StringeeError error) {
                call.reject("Error ending call: " + error.getMessage());
            }
        });
    }

    private void checkConnect(String token) {
        stringeeClient.setConnectionListener(new StringeeConnectionListener() {
            @Override
            public void onConnectionConnected(StringeeClient client, boolean isReconnecting) {
                Log.d("Stringee", "Connected to server");
                showToast("Connected to server");
            }
            @Override
            public void onConnectionDisconnected(StringeeClient client, boolean isReconnecting) {}
            @Override
            public void onIncomingCall(StringeeCall call) {
                stopMediaPlayer();
                mediaPlayer = MediaPlayer.create(getContext(), R.raw.beep);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                }
                currentCall = call;
                QzStringeePlugin.storeCall(call);
                showToast("Incoming call received");
            }

            @Override
            public void onIncomingCall2(StringeeCall2 stringeeCall2) {

            }

            @Override
            public void onConnectionError(StringeeClient client, StringeeError error) {
                Log.d("Stringee", "Connection error: " + error.getMessage());
                showToast("Connection error: " + error.getMessage());
            }

            @Override
            public void onRequestNewToken(StringeeClient stringeeClient) {

            }

            @Override
            public void onCustomMessage(String s, JSONObject jsonObject) {

            }

            @Override
            public void onTopicMessage(String s, JSONObject jsonObject) {

            }
        });
        stringeeClient.connect(token);
    }

    private void setupCallListeners(StringeeCall call) {
        call.setCallListener(new StringeeCall.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String s, int i, String s1) {
                Log.d("Stringee", "Signaling State Changed: " + signalingState);
                if (signalingState == StringeeCall.SignalingState.RINGING) {
                    Log.d("Stringee", "Call connected, media channel should be established.");
                } else if (signalingState == StringeeCall.SignalingState.ANSWERED) {

                    Log.d("Stringee", "Call answered, media channel should be available.");
//                    onLocalStream(call);
//                    onMediaStateChange(call, StringeeCall.MediaState.CONNECTED);
//                    onRemoteStream(call);
                } else if (signalingState == StringeeCall.SignalingState.CALLING) {
                    Log.d("Stringee", "Call is in calling state, waiting for answer...");

                } else if (signalingState == StringeeCall.SignalingState.ENDED) {
                    Log.d("Stringee", "Call ended.");
                } else {
                    Log.d("Stringee", "Other signaling state: " + signalingState);
                }
            }

            @Override
            public void onError(StringeeCall stringeeCall, int i, String s) {
                Log.d("Stringee", "Call Error: " + s);
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String s) {
                Log.d("Stringee", "Call handled on another device: " + signalingState);

            }

            @Override
            public void onMediaStateChange(StringeeCall call, StringeeCall.MediaState mediaState) {
                Log.d("Stringee", "Media State Changed: " + mediaState);
                if (mediaState == StringeeCall.MediaState.CONNECTED) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (StaudioManager != null) {
                            StaudioManager.setSpeakerphoneOn(true);
                        }
                    }, 500);
                }
            }

            @Override
            public void onLocalStream(StringeeCall stringeeCall) {
                Log.d("Stringee", "Local Stream Received");
                stringeeCall.getRemoteView();
                stringeeCall.renderLocalView(true);
            }

            @Override
            public void onRemoteStream(StringeeCall stringeeCall) {
                Log.d("Stringee", "Remote Stream Received");
            }

            @Override
            public void onCallInfo(StringeeCall stringeeCall, JSONObject jsonObject) {
                Log.d("Stringee", "Call Info: " + jsonObject.toString());
            }
        });
    }

    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }



    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void sendSuccessResponse(PluginCall call, String message) {
        JSObject ret = new JSObject();
        ret.put("message", message);
        call.resolve(ret);
        showToast(message);
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}
