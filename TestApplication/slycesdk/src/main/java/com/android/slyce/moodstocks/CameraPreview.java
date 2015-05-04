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

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.android.slyce.moodstocks.*;

/** Wrapper around a {@link SurfaceView} used for previewing the camera */
public class CameraPreview implements SurfaceHolder.Callback {

  /** Interface to be notified when the preview actually starts <b> or re-starts </b> */
  public static interface Listener {
    /** Notified every time the preview starts or re-starts (for example due to a
     * change in the UI orientation).
     * @param holder the {@link SurfaceHolder} associated to the current {@link SurfaceView}.
     * @param s the {@link com.moodstocks.android.camera.Size} of the {@link SurfaceView}.
     */
    public void onPreviewStarts(SurfaceHolder holder, Size s);
  }

  /** The {@link SurfaceView} used for preview */
  private SurfaceView preview;
  /** The {@link Listener} to notify */
  private Listener listener;
  /** The {@link SurfaceHolder} associated with {@link #preview} */
  private SurfaceHolder holder;

  /**
   * Constructor
   * @param preview the {@link SurfaceView} to use for preview
   * @param listener the {@link Listener} to notify
   */
  @SuppressWarnings("deprecation")
  protected CameraPreview(SurfaceView preview, Listener listener) {
    this.preview = preview;
    this.listener = listener;
    preview.setVisibility(View.INVISIBLE);
    this.holder = preview.getHolder();
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB)
      this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  /** <b>Asynchronously</b> starts the preview.
   * <p>The {@link Listener} will be notified as soon as the preview is ready.</p>
   */
  protected void startAsync() {
    this.holder.addCallback(this);
    this.preview.setVisibility(View.VISIBLE);
  }

  /** <b>Synchronously</b> stop the preview. */
  protected void stop() {
    this.preview.setVisibility(View.INVISIBLE);
    this.holder.removeCallback(this);
  }

  // Interfaces implementation

  /**
   * {@link SurfaceHolder.Callback SurfaceHolder.Callback} implementation
   * @exclude
   */
  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    // void implementation
  }

  /**
   * {@link SurfaceHolder.Callback SurfaceHolder.Callback} implementation
   * @exclude
   */
  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    this.listener.onPreviewStarts(holder, new Size(width, height));
  }

  /**
   * {@link SurfaceHolder.Callback SurfaceHolder.Callback} implementation
   * @exclude
   */
  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    // void implementation
  }

}
