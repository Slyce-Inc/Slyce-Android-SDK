package com.android.slyce.report.java_websocket.drafts;

import com.android.slyce.report.java_websocket.exceptions.InvalidHandshakeException;
import com.android.slyce.report.java_websocket.handshake.ClientHandshake;
import com.android.slyce.report.java_websocket.handshake.ClientHandshakeBuilder;

public class Draft_17 extends Draft_10 {
	@Override
	public HandshakeState acceptHandshakeAsServer( ClientHandshake handshakedata ) throws InvalidHandshakeException {
		int v = readVersion( handshakedata );
		if( v == 13 )
			return HandshakeState.MATCHED;
		return HandshakeState.NOT_MATCHED;
	}

	@Override
	public ClientHandshakeBuilder postProcessHandshakeRequestAsClient( ClientHandshakeBuilder request ) {
		super.postProcessHandshakeRequestAsClient( request );
		request.put( "Sec-WebSocket-Version", "13" );// overwriting the previous
		return request;
	}

	@Override
	public Draft copyInstance() {
		return new Draft_17();
	}

}
