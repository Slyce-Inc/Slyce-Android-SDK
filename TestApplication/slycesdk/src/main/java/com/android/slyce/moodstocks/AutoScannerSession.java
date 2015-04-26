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

package com.android.slyce.moodstocks;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceView;

import com.moodstocks.android.ManualScannerSession;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;
import com.moodstocks.android.advanced.Image;
import com.moodstocks.android.advanced.Tools;
import com.moodstocks.android.camera.CameraFrame;
import com.moodstocks.android.camera.CameraManager;
import com.moodstocks.android.core.Loader;

/**
 * The {@code AutoScannerSession} class allows you to easily setup a scanning session
 * that automatically processes camera frames in real-time and report successful on-device
 * scan results.
 * <p>If your collection of reference images does not exceed the on-device database capacity,
 * then this session provides the best user experience possible since it never introduces
 * network latencies. Otherwise you may want to use a {@link ManualScannerSession}.</p>
 * <p>Think of it as a convenient and configurable wrapper around the Moodstocks SDK, hiding
 * the messy details of setting up the camera preview and managing the frames.</p>
 */
public class AutoScannerSession extends Handler implements CameraManager.Listener {

  /** Logging TAG */
  private static final String TAG = "AutoScannerSession";

  static {
    Loader.load();
  }

  /** Interface to be notified of results and potential errors. */
  public static interface Listener {
    /**
     * This callback will get notified shortly after {@link AutoScannerSession#start()}
     * is called, in the (very rare) occasions where the camera cannot be opened.
     * <p>In such a case the user should be informed of the error. The best advice to give
     * your user is to reboot his/her device, as it's often the only way to fix the issue.</p>
     * @param e the Exception that was returned by the camera.
     */
    public void onCameraOpenFailed(Exception e);
    /**
     * Called immediately whenever a {@link Result} is found.
     * <p>The session is automatically paused using {@link AutoScannerSession#pause()}
     * right before this callback gets notified. It is your responsibility to
     * restart it using {@link AutoScannerSession#resume()} when needed. In a basic use
     * case, the result will be displayed to the user using a UI component, and
     * the session should be resumed as soon as this UI component is dismissed by
     * the user to let him/her start scanning again.</b>
     * @param r the found {@link Result}, guaranteed to be non-null.
     */
    public void onResult(Result r);
    /**
     * Debugging callback triggered in the event of an error being returned
     * during the processing of camera frames through offline image recognition
     * or barcode decoding.
     * <p>The session is <b>not</b> automatically paused before this callback
     * gets notified.</p>
     * @param debugMessage the warning description.
     */
    public void onWarning(String debugMessage);

    /** Called whenever snap method is called */
    public void onSnap(Bitmap bitmap);
  }

  /**
   * Advanced interface designed for experts users.
   * <p>In most cases, you'll only need to implement the {@link Listener} interface.</p>
   * <p>This interface allows to deliver the query frame that led to a result in addition
   * with the result itself. It may be useful for developers that would, for example, want
   * to perform extra processing on the matched video frames or their warped version.</p>
   */
  public static interface AdvancedListener extends Listener {
    /**
     * Similar to the {@link Listener#onResult(Result)} method.
     * <p>If you pass an {@code AdvancedListener} to the {@link AutoScannerSession}
     * constructor, this method will be called <b>instead of</b> the {@link Listener#onResult(Result)}
     * method in order to provide the video frame that led to the result.</p>
     * @param r the found {@link Result}, guaranteed to be non-null.
     * @param videoFrame the frame from the video stream that led to this result. Note that this
     * frame is <b>not suitable</b> for end-user displaying, but rather for custom post-processing.
     * You may want to use the {@link Result#warpImage(Bitmap)} or {@link Result#warpImage(Bitmap, float)}
     * methods with this video frame to get a warped version of the matched reference in the frame
     * before post-processing it.
     */
    public void onResult(Result r, Bitmap videoFrame);

  }

  /**
   * Internal return codes.
   * @exclude
   */
  private static final class ReturnCode {
    /** return an error */
    private static final int ERROR  = 0;
    /** return a result */
    private static final int RESULT = 1;

    private static final int SNAP = 2;
  }

  /**
   * Constant mask to check if the required
   * {@link Result.Type}s contains barcodes.
   * @exclude
   */
  private static final int BARCODE_MASK = Result.Type.EAN8 | Result.Type.EAN13 | Result.Type.DATAMATRIX | Result.Type.QRCODE;

  /** The {@link CameraManager} used by this session */
  private CameraManager cameraManager;
  /** The {@link Scanner} used by this session */
  private Scanner scanner;
  /** The {@link Listener} to notify. */
  private Listener listener;

  /** The {@link CameraFrame} currently being processed, if any */
  private CameraFrame currentFrame = null;
  /** Flag indicating whether the session is started or not */
  private boolean started = false;
  /** Flag indicating whether the session is paused or not */
  private boolean paused  = false;

  /** The default bitwise-or separated {@link Result.Type}s to use. */
  private int types   = Result.Type.IMAGE;
  /** The default bitwise-or separated {@link Scanner.SearchOption}s to use. */
  private int options   = Scanner.SearchOption.DEFAULT;
  /** The default bitwise-or separated {@link Result.Extra}s to compute. */
  private int extras  = Result.Extra.NONE;
  /** Flag indicating whether the listener is an {@link AdvancedListener} */
  private boolean advancedListener = false;

  private boolean isSnap = false;

  /**
   * Constructor.
   * @param parent the parent {@link Activity}.
   * @param scanner the {@link Scanner} instance on which this session relies. It must
   * be and remain opened during the whole time this session is in use.
   * @param listener the {@link Listener} to notify of results and errors.
   * @param preview the {@link SurfaceView} into which the camera preview will
   * be displayed.
   */
  public AutoScannerSession(Activity parent, Scanner scanner, Listener listener, SurfaceView preview) {
    this.cameraManager = new CameraManager(parent, this, preview);
    this.scanner = scanner;
    this.listener = listener;
    if (listener instanceof AdvancedListener)
      this.advancedListener = true;
  }

  /**
   * Selects which image recognition and barcode decoding operations will be performed
   * on each frame by this session, according to the desired result types.
   * <p>Default value is {@link Result.Type#IMAGE} only.</p>
   * @param types the bitwise-OR separated list of {@link Result.Type} to search for.
   */
  public void setResultTypes(int types) {
    this.types = types;
  }

  /**
   * Selects the {@link Scanner.SearchOption}s to use.
   * <p>Default value is {@link Scanner.SearchOption#DEFAULT}.</p>
   * @param options the bitwise-OR separated list of {@link Scanner.SearchOption}s to use.
   */
  public void setSearchOptions(int options) {
    this.options = options;
  }

  /**
   * Selects which {@link Result.Extra}s should be computed and attached
   * to the returned results.
   * <p>Default value is {@link Result.Extra#NONE}.</p>
   * @param extras the bitwise-OR separated list of {@link Result.Extra} to compute.
   */
  public void setResultExtras(int extras) {
    this.extras = extras;
  }

  /**
   * Starts the camera preview and starts processing the frames.
   * <p><b>WARNING: </b>the {@link #start()}/{@link #stop()} lifecycle is <b>independent</b>
   * from the {@link #pause()}/{@link #resume()} lifecycle, <i>i.e.</i> calling {@link #start()}
   * does <b>not</b> automatically call {@link #resume()}.<p>
   */
  public void start() {
    // TODO: expose front/back choice?
    this.cameraManager.start(true, true);
    this.started = true;
  }

  /** Stops processing the frames, keeping the camera preview alive. */
  public void pause() {
    this.paused = true;
  }

  /** Resumes processing the frames after a call to {@link #pause()}. */
  public void resume() {
    this.paused = false;
  }

  /** Stops processing the frames and stops camera preview. */
  public void stop() {
    this.started = false;
    this.cameraManager.stop();
  }

  // CameraManager.Listener

  /**
   * {@link CameraManager.Listener CameraManager.Listener} implementation
   * @exclude
   */
  @Override
  public boolean isListening() {
    return (this.started && !this.paused);
  }

  /**
   * {@link CameraManager.Listener CameraManager.Listener} implementation
   * @exclude
   */
  @Override
  public void onCameraOpenException(Exception e) {
    this.listener.onCameraOpenFailed(e);
  }

  /**
   * {@link CameraManager.Listener CameraManager.Listener} implementation
   * @exclude
   */
  @Override
  public void onNewFrameInBackground(CameraFrame f) {
    this.currentFrame = f;

    Result result = null;
    MoodstocksError error = null;
    Image qry = null;

    try {
      qry = new Image(f.data, f.size.width, f.size.height, f.orientation);
    } catch (MoodstocksError e) {
      error = e;
    }

    if (qry != null) {
      try {
        result = offlineImpl(qry);
      } catch (MoodstocksError e) {
        error = e;
      }
      qry.release();
    }

    if (error != null)
      this.obtainMessage(ReturnCode.ERROR, error).sendToTarget();
    else {
      Bitmap query = null;
      if ((result != null) && this.advancedListener) {
        query = Tools.convertNV21ToBitmap(f.data, f.size.width, f.size.height, f.orientation);
      }
      this.obtainMessage(ReturnCode.RESULT, new Pair<Result,Bitmap>(result, query)).sendToTarget();

      if(isSnap){
        isSnap = false;
        query = Tools.convertNV21ToBitmap(f.data, f.size.width, f.size.height, f.orientation);
        this.obtainMessage(ReturnCode.SNAP, query).sendToTarget();
      }
    }
  }

  // Private methods

  /**
   * Low-level method to perform the client-side search and barcode decoding.
   * @param query the query {@link Image}
   * @return the {@link Result} if any,
   * {@code null} otherwise.
   * @throws MoodstocksError if any error occurs.
   */
  private Result offlineImpl(Image query)
      throws MoodstocksError {
    Result result = null;
    if ((this.types & Result.Type.IMAGE) != 0) {
      result = this.scanner.search(query, this.options, this.extras);
    }
    if (result == null && (this.types & BARCODE_MASK) != 0) {
      result = this.scanner.decode(query, this.types, this.extras);
    }
    return result;
  }

  // Message passing + "safe" wrapper around Listener

  /**
   * {@link Handler} implementation
   * @exclude
   */
  @Override
  @SuppressWarnings("unchecked")
  public void handleMessage(Message msg) {
    switch (msg.what) {
      case ReturnCode.ERROR:
        MoodstocksError e = (MoodstocksError)msg.obj;
        transmitWarning(e.getMessage());
        break;
      case ReturnCode.RESULT:
        Pair<Result,Bitmap> pair = (Pair<Result,Bitmap>)msg.obj;
        Result res = pair.first;
        Bitmap bmp = pair.second;
        if (res != null) {
          transmitResult(res, bmp);
        }
        break;

      case ReturnCode.SNAP:

        Bitmap currentBitmap = (Bitmap) msg.obj;
        transmitBitmap(currentBitmap);

        break;
      default:
        Log.e(TAG, "handleMessage: bad message received ("+msg.what+")");
        break;
    }
    this.currentFrame.release();
  }

  /**
   * Calls {@link Listener#onWarning(String)}
   * @param s the warning message
   */
  private void transmitWarning(String s) {
    if (this.started && !this.paused)
      this.listener.onWarning(s);
  }

  /**
   * Pauses the session and calls {@link Listener#onResult(Result)} or
   * {@link AdvancedListener#onResult(Result, Bitmap)}
   * @param r the {@link Result} (can be null)
   * @param bmp the {@link Bitmap} frame (can be null)
   */
  private void transmitResult(Result r, Bitmap bmp) {
    if (this.started && !this.paused) {
      pause();
      if (this.advancedListener)
        ((AdvancedListener)this.listener).onResult(r, bmp);
      else
        this.listener.onResult(r);
    }
  }

  private void transmitBitmap(Bitmap bitmap){
    this.listener.onSnap(bitmap);
  }

  public void snap(){
    isSnap = true;
  }

}
