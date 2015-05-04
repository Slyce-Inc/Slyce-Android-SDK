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

/** Simple class holding a frame as provided by the camera. */
public class CameraFrame {

  /**
   * Interface to be notified when the frame is released.
   */
  protected static interface ReleaseListener {
    /**
     * This callback will get notified as soon as {@link CameraFrame#release()} is called.
     */
    public void onFrameReleased();
  }

  /** The {@link ReleaseListener} to notify. */
  private ReleaseListener listener;
  /** The NV21 data */
  public byte[] data;
  /** The {@link com.moodstocks.android.camera.Size} of the frame */
  public Size size;
  /** The clockwise re-orientation to apply to the frame */
  public int orientation;

  /** Constructor.
   * @param listener is optional: if {@code null} is passed, no {@link ReleaseListener}
   * will be notified.
   * @param buf the NV21 data.
   * @param size the frame {@link com.moodstocks.android.camera.Size}.
   * @param ori the clockwise re-orientation to apply to the data.
   */
  public CameraFrame(ReleaseListener listener, byte[] buf, Size size, int ori) {
    this.listener = listener;
    this.data = buf;
    this.size = size.clone();
    this.orientation = ori;
  }

  /**
   * Releases the frame and notifies the associated {@link ReleaseListener},
   * if any.
   */
  public void release() {
    this.listener.onFrameReleased();
  }

}
