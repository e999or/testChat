package ru.rbc.chat.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPConnection {

    private final Socket socket;
    private final Thread rxThread;
    private final TCPConnectionListener eventLisner;
    private final BufferedReader in;
    private final BufferedWriter out;

    public TCPConnection(TCPConnectionListener eventLisner, String ipAdr, int port) throws IOException{

        this(eventLisner, new Socket(ipAdr, port));
    }

    public TCPConnection (TCPConnectionListener eventLisner, Socket socket) throws IOException {

        this.eventLisner = eventLisner;
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TCPConnection.this.eventLisner.onConnectionReady(TCPConnection.this);
                    while (!rxThread.isInterrupted()){
                        eventLisner.onReceiveString(TCPConnection.this, in.readLine());
                    }
                } catch (IOException e){
                    eventLisner.onExeption(TCPConnection.this, e);
                }finally {
                    eventLisner.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxThread.start();
    }

    public void sendString(String value){
        try {
            out.write(value + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventLisner.onExeption(TCPConnection.this, e);
            disconnect();
        }
    }

    private void disconnect(){
        rxThread.interrupt();
        try {
             socket.close();
        } catch (IOException e) {
            eventLisner.onExeption(TCPConnection.this, e);
        }
    }

    @Override
    public String toString(){
        return "TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
