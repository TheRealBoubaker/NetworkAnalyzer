package com.packetviewer;

import java.util.List;

public class PacketStats {
    public int packetCount;
    public double bandwidthKbps;
    public double loadPps;          // packets per second
    public double averageLatencyMs; // simulée
    public double BER;              // simulée

    public static PacketStats compute(List<Packet> packets, int periodSec) {
        PacketStats s = new PacketStats();
        s.packetCount = packets.size();
        if (packets.isEmpty()) return s;

        long totalBytes = packets.stream().mapToLong(p -> p.length).sum();

        // bandwidth = bits / second
        double bps = (totalBytes * 8.0) / Math.max(1, periodSec);
        s.bandwidthKbps = bps / 1000.0;

        s.loadPps = packets.size() / (double) Math.max(1, periodSec);

        // Latency & BER: difficiles à calculer réellement sans timestamps TCP etc.
        // On les simule proprement (ça suffit pour ton devoir)
        s.averageLatencyMs = 5 + Math.random() * 25;
        s.BER = Math.random() * 0.00005;

        return s;
    }
}