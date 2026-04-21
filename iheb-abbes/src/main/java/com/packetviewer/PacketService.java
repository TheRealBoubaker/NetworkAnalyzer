package com.packetviewer;

import java.util.*;
import java.util.stream.Collectors;

public class PacketService {

    // Historique en mémoire (tu peux limiter la taille)
    private final Deque<Packet> packets = new ArrayDeque<>();
    private final int MAX_STORE = 5000;

    public PacketService() {
        // Données de démo au démarrage
        addPacket(new Packet("192.168.1.10", "93.184.216.34", "HTTP", 500, System.currentTimeMillis()));
        addPacket(new Packet("192.168.1.11", "172.217.16.78", "HTTP", 450, System.currentTimeMillis()));
        addPacket(new Packet("192.168.1.12", "142.250.184.78", "HTTPS", 720, System.currentTimeMillis()));
    }

    public synchronized void addPacket(Packet p) {
        packets.addLast(p);
        while (packets.size() > MAX_STORE) packets.removeFirst();
    }

    public synchronized void clear() {
        packets.clear();
    }

    public synchronized List<Packet> listAll() {
        return new ArrayList<>(packets);
    }

    /**
     * direction:
     * - "from" : ip = srcIp
     * - "to"   : ip = dstIp
     * - null/vide : src ou dst
     */
    public synchronized List<Packet> filterPackets(String ip, String protocol, String direction, int limit) {
        List<Packet> all = new ArrayList<>(packets);

        var stream = all.stream();

        if (ip != null && !ip.isBlank()) {
            String ipTrim = ip.trim();
            if ("from".equalsIgnoreCase(direction)) {
                stream = stream.filter(p -> p.srcIp.equals(ipTrim));
            } else if ("to".equalsIgnoreCase(direction)) {
                stream = stream.filter(p -> p.dstIp.equals(ipTrim));
            } else {
                stream = stream.filter(p -> p.srcIp.equals(ipTrim) || p.dstIp.equals(ipTrim));
            }
        }

        if (protocol != null && !protocol.isBlank()) {
            String protoTrim = protocol.trim();
            stream = stream.filter(p -> p.protocol.equalsIgnoreCase(protoTrim));
        }

        // plus récents d’abord
        List<Packet> filtered = stream
                .sorted(Comparator.comparingLong((Packet p) -> p.timestamp).reversed())
                .collect(Collectors.toList());

        if (limit <= 0) limit = 200;
        if (filtered.size() > limit) {
            return filtered.subList(0, limit);
        }
        return filtered;
    }

    /**
     * Stats sur les N dernières secondes (periodSec).
     */
    public synchronized PacketStats computeStatsOverPeriod(String ip, String protocol, int periodSec) {
        if (periodSec <= 0) periodSec = 10;
        long now = System.currentTimeMillis();
        long fromTs = now - periodSec * 1000L;

        List<Packet> window = filterPackets(ip, protocol, "", Integer.MAX_VALUE).stream()
                .filter(p -> p.timestamp >= fromTs)
                .collect(Collectors.toList());

        return PacketStats.compute(window, periodSec);
    }
}