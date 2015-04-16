package com.android.slyce.async.dns;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Created by koush on 10/20/13.
 */
public class Dns {
    public static com.android.slyce.async.future.Future<com.android.slyce.async.dns.DnsResponse> lookup(String host) {
        return lookup(com.android.slyce.async.AsyncServer.getDefault(), host, false, null);
    }

    private static int setFlag(int flags, int value, int offset) {
        return flags | (value << offset);
    }

    private static int setQuery(int flags) {
        return setFlag(flags, 0, 0);
    }

    private static int setRecursion(int flags) {
        return setFlag(flags, 1, 8);
    }

    private static void addName(ByteBuffer bb, String name) {
        String[] parts = name.split("\\.");
        for (String part: parts) {
            bb.put((byte)part.length());
            bb.put(part.getBytes());
        }
        bb.put((byte)0);
    }

    public static com.android.slyce.async.future.Future<com.android.slyce.async.dns.DnsResponse> lookup(com.android.slyce.async.AsyncServer server, String host) {
        return lookup(server, host, false, null);
    }

    public static com.android.slyce.async.future.Cancellable multicastLookup(com.android.slyce.async.AsyncServer server, String host, com.android.slyce.async.future.FutureCallback<com.android.slyce.async.dns.DnsResponse> callback) {
        return lookup(server, host, true, callback);
    }

    public static com.android.slyce.async.future.Cancellable multicastLookup(String host, com.android.slyce.async.future.FutureCallback<com.android.slyce.async.dns.DnsResponse> callback) {
        return multicastLookup(com.android.slyce.async.AsyncServer.getDefault(), host, callback);
    }

    public static com.android.slyce.async.future.Future<com.android.slyce.async.dns.DnsResponse> lookup(com.android.slyce.async.AsyncServer server, String host, final boolean multicast, final com.android.slyce.async.future.FutureCallback<com.android.slyce.async.dns.DnsResponse> callback) {
        ByteBuffer packet = com.android.slyce.async.ByteBufferList.obtain(1024).order(ByteOrder.BIG_ENDIAN);
        short id = (short)new Random().nextInt();
        short flags = (short)setQuery(0);
        if (!multicast)
            flags = (short)setRecursion(flags);

        packet.putShort(id);
        packet.putShort(flags);
        // number questions
        packet.putShort(multicast ? (short)1 : (short)2);
        // number answer rr
        packet.putShort((short)0);
        // number authority rr
        packet.putShort((short)0);
        // number additional rr
        packet.putShort((short)0);

        addName(packet, host);
        // query
        packet.putShort(multicast ? (short)12 : (short)1);
        // request internet address
        packet.putShort((short)1);

        if (!multicast) {
            addName(packet, host);
            // AAAA query
            packet.putShort((short) 28);
            // request internet address
            packet.putShort((short)1);
        }

        packet.flip();


        try {
            final com.android.slyce.async.AsyncDatagramSocket dgram;
            // todo, use the dns server...
            if (!multicast) {
                dgram = server.connectDatagram(new InetSocketAddress("8.8.8.8", 53));
            }
            else {
//                System.out.println("multicast dns...");
                dgram = com.android.slyce.async.AsyncServer.getDefault().openDatagram(new InetSocketAddress(5353), true);
                Field field = DatagramSocket.class.getDeclaredField("impl");
                field.setAccessible(true);
                Object impl = field.get(dgram.getSocket());
                Method method = impl.getClass().getMethod("join", InetAddress.class);
                method.setAccessible(true);
                method.invoke(impl, InetAddress.getByName("224.0.0.251"));
                ((DatagramSocket)dgram.getSocket()).setBroadcast(true);
            }
            final com.android.slyce.async.future.SimpleFuture<com.android.slyce.async.dns.DnsResponse> ret = new com.android.slyce.async.future.SimpleFuture<com.android.slyce.async.dns.DnsResponse>() {
                @Override
                protected void cleanup() {
                    super.cleanup();
//                    System.out.println("multicast dns cleanup...");
                    dgram.close();
                }
            };
            dgram.setDataCallback(new com.android.slyce.async.callback.DataCallback() {
                @Override
                public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
                    try {
//                        System.out.println(dgram.getRemoteAddress());
                        com.android.slyce.async.dns.DnsResponse response = com.android.slyce.async.dns.DnsResponse.parse(bb);
//                        System.out.println(response);
                        response.source = dgram.getRemoteAddress();

                        if (!multicast) {
                            dgram.close();
                            ret.setComplete(response);
                        }
                        else {
                            callback.onCompleted(null, response);
                        }
                    }
                    catch (Exception e) {
                    }
                    bb.recycle();
                }
            });
            if (!multicast)
                dgram.write(new com.android.slyce.async.ByteBufferList(packet));
            else
                dgram.send(new InetSocketAddress("224.0.0.251", 5353), packet);
            return ret;
        }
        catch (Exception e) {
            com.android.slyce.async.future.SimpleFuture<com.android.slyce.async.dns.DnsResponse> ret = new com.android.slyce.async.future.SimpleFuture<com.android.slyce.async.dns.DnsResponse>();
            ret.setComplete(e);
            if (multicast)
                callback.onCompleted(e, null);
            return ret;
        }
    }
}
