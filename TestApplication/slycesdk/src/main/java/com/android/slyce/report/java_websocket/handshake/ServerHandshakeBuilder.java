package com.android.slyce.report.java_websocket.handshake;

public interface ServerHandshakeBuilder extends HandshakeBuilder, ServerHandshake {
	public void setHttpStatus(short status);
	public void setHttpStatusMessage(String message);
}
