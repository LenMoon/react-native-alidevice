
import { NativeModules } from 'react-native';

const { RNAlidevice } = NativeModules;

const _aliIot = RNAlidevice;
const alIotEventEmitter = new NativeEventEmitter(_aliIot);
const onConnecStateChange = (fn) => alIotEventEmitter.addListener(
'EVENT_CONNECT_CHANGE',
fn
)

const onDataNotify = (fn) => alIotEventEmitter.addListener(
'EVENT_DATA_NOTIFY',
fn
)

const init = (pk, deviceName, deviceSecret) => {
return _aliIot.init(pk, deviceName, deviceSecret);
}
const destory = () => _aliIot.destory();

const publish = (topic, content, qos) => _aliIot.publish(topic, content, qos);

const subscribe = (topic) => _aliIot.subscribe(topic);

const unsubscribe = (topic) => _aliIot.unsubscribe(topic);

export const AliDevice = {
init, destory, onConnecStateChange, onDataNotify, publish, subscribe, unsubscribe
}



export default AliDevice;
