package com.networkanalyzer.model;

import java.util.List;

public class PacketStats {
    public int    packetCount;
    public double bandwidthKbps;
    public double loadPps;
    public double averageLatencyMs;
    public double BER;

    public static PacketStats compute(List<HttpFrame> packets, int periodSec) {
        PacketStats s = new PacketStats();
        s.packetCount = packets.size();
        if (packets.isEmpty()) return s;

        long totalBytes = packets.stream().mapToLong(HttpFrame::getTaille).sum();
        double bps = (totalBytes * 8.0) / Math.max(1, periodSec);
        s.bandwidthKbps   = bps / 1000.0;
        s.loadPps         = packets.size() / (double) Math.max(1, periodSec);
        s.averageLatencyMs = 5 + Math.random() * 25;
        s.BER             = Math.random() * 0.00005;
        return s;
    }
}
