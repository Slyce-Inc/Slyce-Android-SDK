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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.moodstocks.android.Configuration;

/** Class wrapping a {@link Camera} object */
@SuppressWarnings("deprecation")
public class CameraInstance extends Handler implements Camera.PreviewCallback {

  /** Logging TAG */
  private static final String TAG = "CameraInstance";

  /** Hardcoded minimum frame top-dimension */
  private static final int MIN_DIM = 480;
  /** Hardcoded maximum frame top-dimension */
  private static final int MAX_DIM = 1280;

  /** Interface to be notified of camera open status and available frames */
  protected static interface Listener {
    /**
     * Notified shortly after a call to {@link CameraInstance#openAsync(boolean, boolean)}
     * if opening the camera suceeded. This idincates that the preview can now be started.
     */
    public void onCameraOpenSuccess();
    /**
     * Notified shortly after a call to {@link CameraInstance#openAsync(boolean, boolean)}
     * if opening the camera failed.
     * @param e the {@link Exception} that caused the failure.
     */
    public void onCameraOpenException(Exception e);
    /**
     * Notified when a new preview frame is available, at 30fps from the moment
     * {@link CameraInstance#startPreview(SurfaceHolder)} is called.
     * @param data the NV21 data.
     * @param size the frame {@link Size}.
     * @param orientation the clockwise re-orientation to apply to the data.
     */
    public void onNewFrame(byte[] data, Size size, int orientation);
  }

  /** The {@link Listener} to notify. */
  private Listener listener;
  /** The {@link OpenThread} managing the camera opening */
  private OpenThread openThread = null;
  /** The Android {@link Camera} object */
  private Camera camera = null;
  /** The {@link PreviewParamsManager} handling the preview parameters */
  private PreviewParamsManager previewParamsManager = null;
  /** The {@link AutoFocusManager} handling the autofocus */
  private AutoFocusManager focusManager = null;

  /** Constructor.
   * @param listener the {@link Listener} to notify
   */
  protected CameraInstance(Listener listener) {
    this.listener = listener;
  }

  /** <b>Asynchronously</b> opens the {@link CameraInstance}.
   * <p> Either {@link Listener#onCameraOpenSuccess()} or {@link Listener#onCameraOpenException(Exception)}
   * will be called shortly after this call to inform the {@link Listener} of the success or
   * failure of the operation.</p>
   * @param back {@code true} to allow back-facing cameras (preferred over front-facing
   * if both are enabled), {@code false} to exclude them.
   * @param front {@code true} to allow front-facing cameras, {@code false} to
   * exclude them.
   */
  protected void openAsync(boolean back, boolean front) {
    this.openThread = new OpenThread(this, back, front);
    this.openThread.start();
  }

  /** <b>Synchronously</b> closes the {@link CameraInstance}.
   * <p>This operation will block until the {@link CameraInstance} is actually closed.</p>
   */
  protected void close() {
    if (this.openThread == null) {
      Log.e(TAG, "`close` called with null openThread -> no-op");
      return;
    }
    else if (this.openThread.isAlive()) {
      try {
        this.openThread.interrupt();
        this.openThread.join();
      } catch (InterruptedException e) {
        // This should not happen. Just in case:
        // - reset interrupt flag,
        // - log.
        this.openThread.interrupt();
        Log.e(TAG, "`close` received InterruptedException");
        e.printStackTrace();
      }
      return;
    }
    closeImpl();
  }

  /** Closes the underlying Android {@link Camera}. */
  private void closeImpl() {
    if (this.camera == null) {
      Log.e(TAG, "`closeImpl` called with null camera -> no-op");
      return;
    }
    this.camera.release();
    this.camera = null;
  }

  // Preview management

  /**
   * Updates the {@link CameraInstance} to the current preview configuration.
   * <p>The preview must be stopped using {@link #stopPreview()} before calling this method.</p>
   * @param surfaceSize the new {@link Size} of the {@link SurfaceView} used for preview.
   * @param uiPortrait {@code true} if the UI is currently in portrait, {@code false} otherwise.
   * @param uiOrientation the current UI orientation, as returned by the system.
   */
  protected void updatePreviewParameters(Size surfaceSize, boolean uiPortrait, int uiOrientation) {
    if (this.camera == null) {
      Log.e(TAG, "`updatePreviewParameters` called with null camera -> no-op");
      return;
    }
    this.previewParamsManager.update(surfaceSize, uiPortrait, uiOrientation);
    Size frameSize = this.previewParamsManager.getFrameSize();
    int displayOrientation = this.previewParamsManager.getDisplayOrientation();
    Camera.Parameters params = this.camera.getParameters();
    params.setPreviewSize(frameSize.width, frameSize.height);
    this.camera.setParameters(params);
    this.camera.setDisplayOrientation(displayOrientation);
  }

  /**
   * Starts displaying the preview and notifying {@link Listener#onNewFrame(byte[], Size, int)}
   * @param holder the {@link SurfaceHolder} associated with the {@link SurfaceView}
   * in which to display the preview.
   */
  protected void startPreview(SurfaceHolder holder) {
    if (this.camera == null) {
      Log.e(TAG, "`startPreview` called with null camera -> no-op");
      return;
    }
    try {
      this.camera.setPreviewDisplay(holder);
      this.camera.setPreviewCallback(this);
      this.camera.startPreview();
      if (this.focusManager != null)
        this.focusManager.start(false, null);
    } catch (IOException e) {
      Log.e(TAG, "`startPreview` received IOException");
      e.printStackTrace();
    }
  }

  /**
   * Stops displaying the preview and notifying {@link Listener#onNewFrame(byte[], Size, int)}.
   */
  protected void stopPreview() {
    if (this.camera == null) {
      Log.e(TAG, "`stopPreview` called with null camera -> no-op");
      return;
    }
    if (this.focusManager != null)
      this.focusManager.stop();
    this.camera.setPreviewCallback(null);
    this.camera.stopPreview();
  }

  /** Requests autofocus */
  protected void requestFocus(boolean focusAtPoint, final Rect focusRect) {
    if (this.focusManager != null)
      this.focusManager.requestFocus(focusAtPoint, focusRect);
  }

  /**
   * Get autofocus status.
   * @return true if currently focussed, false otherwise.
   */
  protected boolean isFocussed() {
    if (this.focusManager != null)
      return this.focusManager.isFocussed();
    else
      return true;
  }

  /**
   * {@link Camera.PreviewCallback Camera.PreviewCallback} implementation.
   * @exclude
   */
  @Override
  public void onPreviewFrame(byte[] data, Camera camera) {
    this.listener.onNewFrame(data,
                             this.previewParamsManager.getFrameSize(),
                             this.previewParamsManager.getFrameOrientation());
  }

  // Private methods and inner classes

  /**
   * Message passing codes sent <b>from</b> the {@link OpenThread}.
   * @exclude
   */
  private static final class Msg {
    /** Successful open */
    public static final int OPEN  = 0;
    /** Failed open */
    public static final int FAIL  = 1;
  }

  /**
   * {@link Thread} used to open the {@link CameraInstance}.
   */
  private static class OpenThread extends Thread {

    /** The {@link CameraInstance} to open */
    private CameraInstance instance;
    /** Flag indicating whether the thread should try to open a back-facing camera */
    private boolean wantBack;
    /** Flag indicating whether the thread should try to open a front-facing camera */
    private boolean wantFront;

    /**
     * Constructor.
     * @param instance the {@link CameraInstance} to open.
     * @param back Flag indicating whether the thread should try to open a back-facing camera
     * @param front Flag indicating whether the thread should try to open a front-facing camera
     */
    private OpenThread(CameraInstance instance, boolean back, boolean front) {
      this.instance = instance;
      this.wantFront = front;
      this.wantBack = back;
    }

    /**
     * {@link Thread#run()} implementation.
     * @exclude
     */
    @Override
    public void run() {
      // Step 1:
      Camera.CameraInfo info = new Camera.CameraInfo();
      int nbCameras = Camera.getNumberOfCameras();
      int back_id = -1;
      int back_ori = -1;
      int front_id = -1;
      int front_ori = -1;
      for (int i = 0; i < nbCameras; ++i) {
        Camera.getCameraInfo(i, info);
        if (back_id < 0 && info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
          back_id = i;
          back_ori = info.orientation;
        }
        if (front_id < 0 && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
          front_id = i;
          front_ori = info.orientation;
        }
      }
      int id = -1;
      int ori = -1;
      boolean front = false;
      if (this.wantBack) {
        if (back_id >= 0) {
          id = back_id;
          ori = back_ori;
        }
      }
      else if (this.wantFront) {
        if (id < 0 && front_id >= 0) {
          id = front_id;
          ori = front_ori;
          front = true;
        }
      }
      // Step 2: try opening it
      Exception e = null;
      if (id < 0) {
        String errMsg = "";
        if (this.wantFront && !this.wantBack)
          errMsg = "front-facing ";
        else if (this.wantBack && !this.wantFront)
          errMsg = "back-facing ";
        e = new Exception("No available "+errMsg+"camera");
      }
      else {
        try {
          Camera camera = Camera.open(id);
          if (camera != null) {
            this.instance.init(camera, front, ori);
          }
          else {
            e = new Exception("Opening camera failed (unknown reason)");
          }
        } catch (Exception ex) {
          e = ex;
        }
      }
      // Step 3: transmit results
      if (isInterrupted()) {
        if (e == null) {
          this.instance.closeImpl();
        }
      }
      else {
        if (e != null) {
          this.instance.obtainMessage(Msg.FAIL, e).sendToTarget();
        }
        else {
          this.instance.obtainMessage(Msg.OPEN).sendToTarget();
        }
      }
    }
  }

  /**
   * Performs initialization tasks on the camera object once its properties are
   * known.
   * @param camera the {@link Camera} object.
   * @param front whether it it front-facing or not.
   * @param ori the {@link Camera.CameraInfo#orientation CameraInfo.orientation}
   * of this {@link Camera} object.
   */
  private void init(Camera camera, boolean front, int ori) {
    this.camera = camera;
    this.previewParamsManager = new PreviewParamsManager(front, ori);
    Camera.Parameters params = this.camera.getParameters();
    // Force NV21
    params.setPreviewFormat(ImageFormat.NV21);
    // If on Google Glass, lock FPS:
    if (Configuration.platform == Configuration.Platform.GOOGLE_GLASS)
      params.setPreviewFpsRange(10000, 10000);
    // Get and trim available preview sizes
    List<Camera.Size> sizes = params.getSupportedPreviewSizes();
    List<Size> availablePreviewSizes = new ArrayList<Size>();
    for (Camera.Size s : sizes) {
      if ( s.width > MAX_DIM || s.height > MAX_DIM ||
          (s.width < MIN_DIM && s.height < MIN_DIM) )
        continue;
      availablePreviewSizes.add(new Size(s));
    }
    this.previewParamsManager.setAvailablePreviewSizes(availablePreviewSizes);
    // Apply!
    this.camera.setParameters(params);
  }

  /**
   * {@link Handler} implementation
   * @exclude
   */
  @Override
  public void handleMessage(Message msg) {
    switch (msg.what) {
      case Msg.OPEN:
        if (Configuration.platform != Configuration.Platform.GOOGLE_GLASS)
          this.focusManager = new AutoFocusManager(this.camera);
        else
          this.focusManager = null;
        this.listener.onCameraOpenSuccess();
        break;
      case Msg.FAIL:
        this.listener.onCameraOpenException((Exception)msg.obj);
        break;
      default:
        Log.e(TAG, "handleMessage: bad message received ("+msg.what+")");
        break;
    }
  }

  /** Inner class managing the different preview parameters. */
  private static class PreviewParamsManager {

    /** Whether the current {@link Camera} is front-facing or not */
    private boolean front;
    /** the {@link Camera.CameraInfo#orientation CameraInfo.orientation} */
    private int orientation;
    /** The available preview sizes */
    private List<Size> availablePreviewSizes;

    /** Cached landscape best frame size */
    private Size cachedLand = new Size();
    /** Cached portrait best frame size */
    private Size cachedPort = new Size();

    /** Current frame size */
    private Size frameSize;
    /** Current frame orientation, <i>i.e.</i> the clockwise re-orientation
     * to apply to the camera frame data.
     */
    private int frameOrientation;
    /** The current re-orientation to apply to the preview */
    private int displayOrientation;

    /**
     * Constructor
     * @param front whether the camera is front-facing or not
     * @param orientation the {@link Camera.CameraInfo#orientation CameraInfo.orientation}
     */
    protected PreviewParamsManager(boolean front, int orientation) {
      this.front = front;
      this.orientation = orientation;
    }

    /**
     * Setter for {@link #availablePreviewSizes}.
     * @param availablePreviewSizes the list of available preview sizes among which
     * to choose the frame size.
     */
    protected void setAvailablePreviewSizes(List<Size> availablePreviewSizes) {
      this.availablePreviewSizes = availablePreviewSizes;
    }

    /**
     * Updates the preview parameters to the current preview configuration.
   * @param surfaceSize the new {@link Size} of the {@link SurfaceView} used for preview.
   * @param uiPortrait {@code true} if the UI is currently in portrait, {@code false} otherwise.
   * @param uiOrientation the current UI orientation, as returned by the system.
     */
    protected void update(Size surfaceSize, boolean uiPortrait, int uiOrientation) {
      // Size
      if (uiPortrait) {
        if (!this.cachedPort.isSet())
          this.cachedPort = findBestPreviewSize(uiPortrait, surfaceSize);
        this.frameSize = this.cachedPort;
      }
      else {
        if (!this.cachedLand.isSet())
          this.cachedLand = findBestPreviewSize(uiPortrait, surfaceSize);
        this.frameSize = this.cachedLand;
      }
      // Display and frame orientations
      int degrees = 0;
      switch (uiOrientation) {
          case Surface.ROTATION_0: degrees = 0; break;
          case Surface.ROTATION_90: degrees = 90; break;
          case Surface.ROTATION_180: degrees = 180; break;
          case Surface.ROTATION_270: degrees = 270; break;
      }
      if (this.front) {
          this.displayOrientation = (this.orientation + degrees) % 360;
          this.frameOrientation = this.displayOrientation;
          this.displayOrientation = (360 - this.displayOrientation) % 360;  // compensate the mirror
      } else {  // back-facing
          this.displayOrientation = (this.orientation - degrees + 360) % 360;
          this.frameOrientation = this.displayOrientation;
      }
    }

    /**
     * Getter for {@link #displayOrientation}
     * @return {@link #displayOrientation}
     */
    protected int getDisplayOrientation() {
      return this.displayOrientation;
    }

    /**
     * Getter for {@link #frameOrientation}
     * @return {@link #frameOrientation}
     */
    protected int getFrameOrientation() {
      return this.frameOrientation;
    }

    /**
     * Getter for {@link #frameSize}
     * @return {@link #frameSize}
     */
    protected Size getFrameSize() {
      return this.frameSize;
    }

    /**
     * Computes the best frame size to use.
     * @param portrait whether the UI is currently in portrait or not
     * @param surfaceSize the {@link Size} of the {@link SurfaceView} currently
     * used for preview.
     * @return the best frame size to use.
     */
    private Size findBestPreviewSize(boolean portrait, Size surfaceSize) {
      float surfaceRatio = portrait ?
          (float)surfaceSize.height/surfaceSize.width :
          (float)surfaceSize.width /surfaceSize.height;
      Size bestSize = new Size();
      for (Size s : this.availablePreviewSizes) {
        int w = s.width;
        int h = s.height;
        if (w > 1280 || h > 1280) continue;
        float r = (float)w/h;
        if (((r-surfaceRatio)*(r-surfaceRatio))/(surfaceRatio*surfaceRatio) < 0.01 && w > bestSize.width) {
          bestSize = s;
        }
      }
      // Nothing found with good ratio? take biggest under 1280.
      // Should rarely (never?) happen.
      if (!bestSize.isSet()) {
        for (Size s : this.availablePreviewSizes) {
          int w = s.width;
          int h = s.height;
          if (w > 1280 || h > 1280) continue;
          if (w > bestSize.width) {
            bestSize = s;
          }
        }
      }
      return bestSize;
    }

  }

  public Camera getCamera(){
    return camera;
  }
}
