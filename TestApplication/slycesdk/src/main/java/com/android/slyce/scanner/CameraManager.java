/*
 * Copyright (c) 2015 Moodstocks SAS
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.android.slyce.scanner;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/** High-level class handling the camera */
public class CameraManager extends Handler implements CameraInstance.Listener,
                                                      CameraPreview.Listener,
                                                      CameraFrame.ReleaseListener {

  /** Interface to be notified of camera events */
  public static interface Listener {
    /**
     * Notified each time a new frame is available to check whether the {@link Listener}
     * needs a frame.
     * @return {@code true} if a frame is needed, {@code false} otherwise.
     */
    public boolean isListening();
    /**
     * Notified shortly after {@link CameraManager#start(boolean, boolean)} if opening
     * the camera fails
     * @param e the {@link Exception} that caused the failure.
     */
    public void onCameraOpenException(Exception e);
    /**
     * Notified each time a frame is available and the {@link Listener} required it
     * by returning {@code true} in {@link #isListening()}.
     * <p>This method is called <b>in a background thread</b>, so don't manipulate UI
     * directly from it!</p>
     * <p>The provided {@link CameraFrame} <b>must</b> be released through
     * {@link CameraFrame#release()} in order for another frame to be delivered.</p>
     * @param f the {@link CameraFrame}
     */
    public void onNewFrameInBackground(CameraFrame f);
  }

  /** The parent {@link Activity} */
  private Activity parent;
  /** The {@link Listener} to notify */
  private Listener listener;

  /** The underlying {@link com.moodstocks.android.camera.CameraInstance} */
  private CameraInstance camera = null;
  /** The underlying {@link CameraPreview} */
  private CameraPreview preview = null;
  /** The {@link BackgroundThread} used to provide frames in the background */
  private BackgroundThread thread = null;
  /** Flag indicating that a frame has been delivered and not yet released. */
  private boolean busy = false;

  /** Falsh is on/off */
  private boolean isFlashOn = false;

  // Constructor and lifecycle handling

  /** Enum to track the initialization status */
  private static final class InitStatus {
    /** The {@link CameraManager} has just been created */
    private static final int NONE      = 0;
    /** {@link #NONE} + {@link CameraManager#thread} started */
    private static final int THREAD_STARTED  = 1;
    /** {@link #THREAD_STARTED} + {@link CameraManager#camera} opened */
    private static final int CAMERA_OPENED   = 2;
    /** {@link #CAMERA_OPENED} + {@link CameraManager#preview} started */
    private static final int PREVIEW_STARTED = 3;
    /** {@link #PREVIEW_STARTED} + {@link CameraManager#camera} started */
    private static final int CAMERA_STARTED  = 4;
  }
  /** The current initialization status among {@link InitStatus}. */
  private int initStatus = InitStatus.NONE;

  /**
   * Constructor
   * @param parent the parent {@link Activity}
   * @param listener the {@link Listener} to notify
   * @param preview the {@link SurfaceView} to use for preview
   */
  public CameraManager(Activity parent, Listener listener, SurfaceView preview) {
    this.parent = parent;
    this.listener = listener;
    this.camera = new CameraInstance(this);
    this.preview = new CameraPreview(preview, this);
  }

  /** Takes care of all the dirty details in order to start the camera preview
   * on the provided {@link SurfaceView} and start delivering frames to the
   * specified {@link Listener}.
   * @param back whether a back-facing camera should be used (preferred over front-facing
   * if both are enabled}.
   * @param front whether a front-facing camera should be used.
   */
  public void start(boolean back, boolean front) {
    this.initStatus = InitStatus.THREAD_STARTED;
    this.thread = new BackgroundThread(this.listener);     // (1)
    this.thread.start();
    this.initStatus = InitStatus.CAMERA_OPENED;
    this.camera.openAsync(back, front);                    // (2)
    // Continues in onCameraOpenSuccess
  }

  /**
   * {@link com.moodstocks.android.camera.CameraInstance.Listener} implementation
   * @exclude
   */
  @Override
  public void onCameraOpenSuccess() {
    this.initStatus = InitStatus.PREVIEW_STARTED;
    this.preview.startAsync();                             // (3)
    // Continues in onPreviewStarts
  }

  /**
   * {@link com.moodstocks.android.camera.CameraInstance.Listener} implementation
   * @exclude
   */
  @Override
  public void onCameraOpenException(Exception e) {
    this.listener.onCameraOpenException(e);
  }

  /**
   * {@link CameraPreview.Listener} implementation
   * @exclude
   */
  @Override
  public void onPreviewStarts(SurfaceHolder holder, Size surfaceSize) {
    this.camera.stopPreview();
    this.camera.updatePreviewParameters(surfaceSize, isUIPortrait(), getUIOrientation());
    this.initStatus = InitStatus.CAMERA_STARTED;
    this.camera.startPreview(holder);                      // (4)
  }

  /** Stops the preview and stops delivering frames. */
  public void stop() {
    // Exact inverse order of what happens after `start`
    if (this.initStatus == InitStatus.CAMERA_STARTED)
      this.camera.stopPreview();                           // counterpart of (4)
    if (this.initStatus >= InitStatus.PREVIEW_STARTED)
      this.preview.stop();                                 // counterpart of (3)
    if (this.initStatus >= InitStatus.CAMERA_OPENED)
      this.camera.close();                                 // counterpart of (2)
    if (this.initStatus >= InitStatus.THREAD_STARTED) {
      this.thread.quit();                                  // counterpart of (1)
      this.thread = null;
    }
    this.initStatus = InitStatus.NONE;
  }

  /**
   * {@link com.moodstocks.android.camera.CameraInstance.Listener} implementation
   * @exclude
   */
  @Override
  public void onNewFrame(byte[] data, Size size, int orientation) {
    if (!this.listener.isListening() || this.busy){
      return;
    }

    this.busy = true;
    CameraFrame f = new CameraFrame(this, data, size, orientation);
    this.thread.sendFrameToBackground(f);
  }

  /**
   * {@link CameraFrame.ReleaseListener} implementation
   * @exclude
   */
  @Override
  public void onFrameReleased() {
    this.busy = false;
  }

  // Private methods

  /**
   * Asks the system if the UI is currently in portrait or landscape
   * @return {@code true} if in portrait, {@code false} if in landscape.
   */
  private boolean isUIPortrait() {
    Configuration config = this.parent.getResources().getConfiguration();
    return config.orientation == Configuration.ORIENTATION_PORTRAIT ? true : false;
  }

  /**
   * Asks the system the current UI orientation.
   * @return the current UI orientation.
   */
  private int getUIOrientation() {
    return this.parent.getWindowManager().getDefaultDisplay().getRotation();
  }

  /** The background {@link Thread} used to deliver frames */
  private static class BackgroundThread extends Thread {

    /** Logging TAG */
    private static final String TAG = "WorkerThread";

    /**
     * Message passing codes sent <b>to</b> the thread
     * @exclude
     */
    private static final class MsgCode {
      /** Process frame */
      private static final int FRAME = 0;
      /** Quit thread */
      private static final int QUIT  = 1;
    }

    /** The {@link Listener} to notify */
    private Listener listener;
    /** The {@link BackgroundThreadHandler} used for message passing */
    private BackgroundThreadHandler threadHandler;

    /**
     * Constructor
     * @param listener the {@link Listener} to notify
     */
    private BackgroundThread(Listener listener) {
      this.listener = listener;
    }

    /**
     * {@link Thread} implementation.
     * @exclude
     */
    @Override
    public void run() {
      Looper.prepare();
      this.threadHandler = new BackgroundThreadHandler(this);
      Looper.loop();
    }

    /**
     * Send a frame to this {@link BackgroundThread} so it is actually sent
     * to the background.
     * @param f the {@link CameraFrame} to send to the background.
     */
    private void sendFrameToBackground(CameraFrame f) {
      this.threadHandler.obtainMessage(MsgCode.FRAME, f).sendToTarget();
    }

    /**
     * Called once the {@link CameraFrame} is actually in the background
     * @param f the {@link CameraFrame}.
     */
    private void frameIsInBackground(CameraFrame f) {
      if (this.listener.isListening()){
        this.listener.onNewFrameInBackground(f);
      } else{
        f.release();
      }
    }

     /**
      * Quit the thread.
      * <p>This method blocks until the thread can actually be exited</p>
      */
    private void quit() {
      this.interrupt();
      this.threadHandler.obtainMessage(MsgCode.QUIT).sendToTarget();
      try {
        this.join();
      } catch (InterruptedException e) {
        // This should not happen. Just in case:
        // - reset interrupt flag,
        // - log.
        this.interrupt();
        Log.e(TAG, "`quit` received InterruptedException");
        e.printStackTrace();
      }
    }

    /** Implementation for {@link #quit()} */
    private void quitImpl() {
      Looper.myLooper().quit();
    }

    /** Class handling message passing between main and background thread */
    private static class BackgroundThreadHandler extends Handler {

      /** The parent {@link BackgroundThread} */
      private BackgroundThread thread;

      /** Constructor
       * @param thread the parent {@link BackgroundThread}
       */
      private BackgroundThreadHandler(BackgroundThread thread) {
        super();
        this.thread = thread;
      }

      /**
       * {@link Handler} implementation
       * @exclude
       */
      @Override
      public void handleMessage(Message msg) {
        switch (msg.what) {
          case MsgCode.FRAME:
            this.thread.frameIsInBackground((CameraFrame)msg.obj);
            break;
          case MsgCode.QUIT:
            this.thread.quitImpl();
            break;
          default:
            Log.e(TAG, "handleMessage: bad message received ("+msg.what+")");
            break;
        }
      }
    }
  }

  public boolean turnFlash(){

    Camera.Parameters params = camera.getCamera().getParameters();

    if(isFlashOn){
      params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
      isFlashOn = false;

    }else{
      params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
      isFlashOn = true;
    }

    params.getFlashMode();
    camera.getCamera().setParameters(params);

    // Return flash new state
    return isFlashOn;
  }

  public void requestFocus(boolean focusAtPoint, final Rect focusRect){
    camera.requestFocus(focusAtPoint, focusRect);
  }

}
