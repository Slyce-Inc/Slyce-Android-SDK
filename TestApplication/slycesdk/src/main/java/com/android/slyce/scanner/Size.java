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

import android.hardware.Camera;

/** Simple wrapper class around a width/height pair */
@SuppressWarnings("deprecation")
public class Size {

  /** The width */
  public int width;
  /** The height */
  public int height;

  /**
   * Empty constructor.
   * <p>Resulting {@link Size} object is not set, <i>i.e.</i> {@link #isSet()}
   * will return {@code false}.</p>
   */
  protected Size() {
    this.width = -1;
    this.height = -1;
  }

  /**
   * Basic constructor
   * @param w the width
   * @param h the height
   */
  protected Size(int w, int h) {
    this.width = w;
    this.height = h;
  }

  /**
   * Constructor from a {@link Camera.Size Camera.Size} object.
   * @param s the {@link Camera.Size Camera.Size} to clone.
   */
  protected Size(Camera.Size s) {
    this.width = s.width;
    this.height = s.height;
  }

  /**
   * Check if a {@link Size} object built through {@link #Size()} has been set.
   * @return {@code true} if set, {code false} otherwise.
   */
  protected boolean isSet() {
    return ((this.width >= 0) && (this.height >= 0));
  }

  /**
   * Clones a {@link Size} object
   * @return a deep copy of the {@link Size} object.
   */
  @Override
  protected Size clone() {
    return new Size(this.width, this.height);
  }

}
