package com.growingc.zxing.simpleuse;

/**
 * Created by RB-cgy on 2016/9/27.
 */
public interface IDevice<T> {
    /**
     * 设备开启
     */
    void start();

    /**
     * 设备关闭
     */
    void close();

    /**
     * 卡信息读取成功
     *
     * @param bean
     */
    void onSuccess(T bean);

    /**
     * 卡信息读取失败
     *
     * @param errorString 失败信息
     */
    void onFailed(String errorString);
}
