package com.example.widget;

/**
 * Created by AItsuki on 2016/6/13.
 *
 */
public interface IRefreshHeader {

    /**
     * 鏉炬墜锛屽ご閮ㄩ殣钘忓悗浼氬洖璋冭繖涓柟娉�
     */
    void reset();

    /**
     * 涓嬫媺鍑哄ご閮ㄧ殑涓�鐬棿璋冪敤
     */
    void pull();

    /**
     * 姝ｅ湪鍒锋柊鐨勬椂鍊欒皟鐢�
     */
    void refreshing();

    /**
     * 澶撮儴婊氬姩鐨勬椂鍊欐寔缁皟鐢�
     * @param currentPos target褰撳墠鍋忕Щ楂樺害
     * @param lastPos   target涓婁竴娆＄殑鍋忕Щ楂樺害
     * @param refreshPos 鍙互鏉炬墜鍒锋柊鐨勯珮搴�
     * @param isTouch   鎵嬫寚鏄惁鎸変笅鐘舵�侊紙閫氳繃scroll鑷姩婊氬姩鏃堕渶瑕佸垽鏂級
     * @param state     褰撳墠鐘舵��
     */
    void onPositionChange(float currentPos, float lastPos, float refreshPos, boolean isTouch, State state);

    /**
     * 鍒锋柊鎴愬姛鐨勬椂鍊欒皟鐢�
     */
    void complete();
}
