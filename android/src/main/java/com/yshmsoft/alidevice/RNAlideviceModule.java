
package com.yshmsoft.alidevice;

import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.linkkit.api.ILinkKitConnectListener;
import com.aliyun.alink.linkkit.api.IoTMqttClientConfig;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linkkit.api.LinkKitInitParams;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectUnscribeListener;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tools.AError;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class RNAlideviceModule extends ReactContextBaseJavaModule implements IConnectNotifyListener {

  private final ReactApplicationContext reactContext;
  public static String SUCCESS = "successed";
  public static String EVENT_CONNECT_CHANGE = "EVENT_CONNECT_CHANGE";
  public static String EVENT_DATA_NOTIFY = "EVENT_DATA_NOTIFY";



  public RNAlideviceModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNAlidevice";
  }

  /**
   * 初始化
   * @param pk
   * @param deviceName
   * @param deviceSecret
   * @param promise
   */
  @ReactMethod
  public void init(String pk, String deviceName, String deviceSecret, final Promise promise) {
    LinkKit.getInstance().registerOnPushListener(this);

    DeviceInfo deviceInfo = new DeviceInfo();
    deviceInfo.productKey = pk;
    deviceInfo.deviceName = deviceName;
    deviceInfo.deviceSecret = deviceSecret;


    Map<String, ValueWrapper> propertyValues = new HashMap<>();
    IoTMqttClientConfig clientConfig = new IoTMqttClientConfig(pk, deviceName, deviceSecret);
    LinkKitInitParams params = new LinkKitInitParams();
    params.deviceInfo = deviceInfo;
    params.propertyValues = propertyValues;
    params.mqttClientConfig = clientConfig;

    LinkKit.getInstance().init(reactContext, params, new ILinkKitConnectListener() {
      @Override
      public void onError(AError aError) {
        promise.reject("302","初始化失败",new Exception(aError.getMsg()));
      }

      @Override
      public void onInitDone(Object o) {
        promise.resolve(SUCCESS);
      }
    });
  }

  /**
   * 订阅
   * @param topic
   * @param promise
   */
  @ReactMethod
  public void subscribe(String topic, final Promise promise) {
    MqttSubscribeRequest request = new MqttSubscribeRequest();
    request.isSubscribe = true;
    request.topic = topic;

    LinkKit.getInstance().subscribe(request, new IConnectSubscribeListener() {
      @Override
      public void onSuccess() {
        promise.resolve(SUCCESS);
      }

      @Override
      public void onFailure(AError aError) {
        promise.reject("303", "订阅失败", new Exception(aError.getMsg()));
      }
    });
  }

  @ReactMethod
  public void publish(String topic, String content, int qos, final Promise promise) {
    MqttPublishRequest request = new MqttPublishRequest();
    request.topic = topic;
    request.isRPC = false;
    request.qos = qos;
    request.payloadObj = content;

    LinkKit.getInstance().publish(request, new IConnectSendListener(){
      @Override
      public void onResponse(ARequest aRequest, AResponse aResponse) {
        //发布成功
        promise.resolve(SUCCESS);
      }

      @Override
      public void onFailure(ARequest aRequest, AError aError) {
        //发布失败
        promise.reject("302", "发送数据失败", new Exception(aError.getMsg()));
      }
    });
  }



  /**
   * 取消订阅
   * @param topic
   * @param promise
   */
  @ReactMethod
  public void unsubscribe(String topic,final Promise promise) {
    MqttSubscribeRequest request = new MqttSubscribeRequest();
    request.isSubscribe = false;
    request.topic = topic;

    LinkKit.getInstance().unsubscribe(request, new IConnectUnscribeListener() {
      @Override
      public void onSuccess() {
        promise.resolve(SUCCESS);
      }

      @Override
      public void onFailure(AError aError) {
        promise.reject("304", "取消订阅失败", new Exception(aError.getMsg()));
      }
    });
  }


  /**
   * 反初始化
   * @param promise
   */
  @ReactMethod
  public void destory(Promise promise) {
    try {
      LinkKit.getInstance().unRegisterOnPushListener(this);
      LinkKit.getInstance().deinit();
    } catch (Exception e) {
      e.printStackTrace();
      promise.reject("301", "反初始化失败",e);
      return;
    }
    promise.resolve(SUCCESS);
  }




  private void sendEvnet(ReactContext reactContext, String eventName, @Nullable Object params) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName,params);
  }

  /**
   * 数据监听
   * @param connectId
   * @param topic
   * @param aMessage
   */
  @Override
  public void onNotify(String connectId, String topic, AMessage aMessage) {
    String downData = new String((byte[]) aMessage.data);
    WritableMap data = Arguments.createMap();
    data.putString("topic", topic);
    data.putString("data", downData);

    sendEvnet(reactContext, EVENT_DATA_NOTIFY, data);
  }

  @Override
  public boolean shouldHandle(String s, String s1) {
    return true;
  }

  /**
   * 连接状态监听
   * @param s
   * @param connectState
   */
  @Override
  public void onConnectStateChange(String s, ConnectState connectState) {
    String connectTip = null;
    if (connectState == ConnectState.CONNECTED) {
      //已连接
      connectTip = "已连接";
    } else if (connectState == ConnectState.DISCONNECTED || connectState == ConnectState.CONNECTFAIL) {
      // 未连接
      connectTip = "未连接";
    } else{
      connectTip = "连接中";
    }
    sendEvnet(reactContext,EVENT_CONNECT_CHANGE,null);
  }





}