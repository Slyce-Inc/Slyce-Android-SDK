package com.android.slyce.report.java_websocket.handshake;

public interface ServerHandshake extends Handshakedata {
	public short getHttpStatus();
	public String getHttpStatusMessage();
}
