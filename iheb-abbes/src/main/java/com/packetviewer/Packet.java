package com.packetviewer;

public class Packet {
    public String srcIp;
    public String dstIp;
    public String protocol;
    public int length;
    public long timestamp;

    public Packet(String srcIp, String dstIp, String protocol, int length, long timestamp) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.protocol = protocol;
        this.length = length;
        this.timestamp = timestamp;
    }
}