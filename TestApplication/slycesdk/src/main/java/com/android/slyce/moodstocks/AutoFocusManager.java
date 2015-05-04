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

import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/** Class managing the camera autofocus by requesting an autofocus every 1.5s */
@SuppressWarnings("deprecation")
public class AutoFocusManager extends Handler implements Camera.AutoFocusCallback {

  /** The used {@link Camera} */
  private Camera camera;
  /** Flag indicating if the camera is currently focussed */
  private boolean is_focus = false;
  /** Flag indicating if the camera is currently focussing */
  private boolean focussing = false;

  /**
   * Internal message passing code.
   * @exclude
   */
  private static int FOCUS_REQUEST = 0;

  /** The 1.5s delay between autofocus request. */
  private static final long FOCUS_DELAY = 1500;

  /**
   * Constructor.
   * @param cam The {@link Camera} object on which to perform autofocus.
   */
  protected AutoFocusManager(Camera cam) {
    Parameters params = cam.getParameters();
    List<String> supported = params.getSupportedFocusModes();
    if (supported.contains(Parameters.FOCUS_MODE_AUTO)) {
      params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
    }
    cam.setParameters(params);
    this.camera = cam;
  }

  /** Starts requesting an autofocus every 1.5 seconds. */
  protected void start() {
    if (this.camera == null) return;
    safeCancelAutoFocus();
    safeAutoFocus();
    this.focussing = true;
  }

  /** Stops requesting an autofocus every 1.5 seconds. */
  protected void stop() {
    safeCancelAutoFocus();
    this.removeMessages(FOCUS_REQUEST);
  }

  /**
   * Method to get the current autofocus state.
   * @return true if the camera is currently focussed, false otherwise.
   */
  protected boolean isFocussed() {
    return (this.is_focus && !this.focussing);
  }

  /**
   * Manually require an autofocus.
   * This method bypasses the 1.5 seconds loop to perform an autofocus as soon as possible.
   */
  protected void requestFocus() {
    if (!this.focussing && !this.is_focus) {
      this.focussing = true;
      stop();
      start();
    }
  }

  /**
   * {@link Handler} implementation.
   * @exclude
   */
  @Override
  public void handleMessage(Message m) {
    if (m.what == FOCUS_REQUEST && this.camera != null) {
      safeCancelAutoFocus();
      safeAutoFocus();
      this.focussing = true;
    }
  }

  /**
   * {@link Camera.AutoFocusCallback Camera.AutoFocusCallback} implementation.
   * @exclude
   */
  @Override
  public void onAutoFocus(boolean success, Camera camera) {
    this.sendEmptyMessageDelayed(FOCUS_REQUEST, FOCUS_DELAY);
    this.focussing = false;
    this.is_focus = success;
  }

  /**
   * Wrapper around {@link Camera#cancelAutoFocus()}
   * method that suppresses any thrown {@link RuntimeException}.
   */
  private void safeCancelAutoFocus() {
    if (this.camera != null) {
      try {
        this.camera.cancelAutoFocus();
      } catch (RuntimeException e) {
        Log.e("AutoFocusManager", "Unexpected Runtime Exception while cancelling autofocus");
      }
    }
  }

  /**
   * Wrapper around {@link Camera#autoFocus(Camera.AutoFocusCallback)}
   * method that suppresses any thrown {@link RuntimeException}.
   */
  private void safeAutoFocus() {
    if (this.camera != null) {
      try {
        this.camera.autoFocus(this);
      } catch (RuntimeException e) {
        Log.e("AutoFocusManager", "Unexpected Runtime Exception while calling autofocus");
      }
    }
  }

}
