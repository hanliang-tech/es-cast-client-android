package eskit.sdk.support.messenger.client.core;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class BaseHandlerThread extends HandlerThread {

    private Handler mUIHandler;
    private Handler mThreadHandler;
    private boolean mQuitting = false;

    public BaseHandlerThread(String name) {
        super(name);
    }

    public BaseHandlerThread(String name, int priority) {
        super(name, priority);
    }

    @Override
    public synchronized void start() {
        super.start();
        mUIHandler = new Handler(Looper.getMainLooper());
        mThreadHandler = new Handler(getLooper());
    }

    public void removeWork(Runnable r){
        mThreadHandler.removeCallbacks(r);
    }

    public void removeAllWork(){
        mThreadHandler.removeCallbacksAndMessages(null);
        mUIHandler.removeCallbacksAndMessages(null);
    }

    public void postWork(Runnable r){
        if(mQuitting) return;
        mThreadHandler.post(r);
    }

    public void postWork(Runnable r, long delayMillis){
        if(mQuitting) return;
        mThreadHandler.postDelayed(r, delayMillis);
    }

    public void postWorkOnUI(Runnable r){
        if(mQuitting) return;
        mUIHandler.post(r);
    }

    public void postWorkOnUI(Runnable r, long delayMillis) {
        if(mQuitting) return;
        mUIHandler.postDelayed(r, delayMillis);
    }

    @Override
    public boolean quit() {
        mQuitting = true;
        mUIHandler.removeCallbacksAndMessages(null);
        mThreadHandler.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return super.quitSafely();
        }
        else {
            mThreadHandler.post(BaseHandlerThread.super::quit);
        }
        return true;
    }
}