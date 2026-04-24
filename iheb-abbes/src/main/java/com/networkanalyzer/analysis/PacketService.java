package com.networkanalyzer.analysis;

import com.networkanalyzer.model.HttpFrame;
import com.networkanalyzer.model.PacketStats;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Central in-memory store for all captured packets.
 * All modules share one instance of this class.
 */
public class PacketService {

    private final Deque<HttpFrame>          packets   = new ArrayDeque<>();
    private final int                       MAX_STORE = 5000;
    private final List<Consumer<HttpFrame>> listeners = new CopyOnWriteArrayList<>();

    private int packetCounter = 0;

    // Demo mode: generates simulated traffic so charts stay alive
    private ScheduledExecutorService demoScheduler;
    private boolean demoMode = false;

    private static final String[][] DEMO_HOSTS = {
        {"192.168.1.10", "93.184.216.34",  "AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02"},
        {"192.168.1.11", "172.217.16.78",  "AA:BB:CC:DD:EE:03", "AA:BB:CC:DD:EE:04"},
        {"192.168.1.12", "142.250.184.78", "AA:BB:CC:DD:EE:05", "AA:BB:CC:DD:EE:06"},
        {"192.168.1.13", "8.8.8.8",        "AA:BB:CC:DD:EE:07", "AA:BB:CC:DD:EE:08"},
    };
    private static final String[][] DEMO_REQUESTS = {
        {"GET",  "/index.html",   "350"},
        {"GET",  "/style.css",    "120"},
        {"POST", "/api/login",    "620"},
        {"GET",  "/dashboard",    "390"},
        {"GET",  "/api/data",     "850"},
        {"POST", "/api/upload",  "4200"},
        {"HTTP/1.1 200 OK", "",  "1240"},
        {"HTTP/1.1 200 OK", "",  "8900"},
        {"HTTP/1.1 302 Found","","210"},
    };

    public PacketService() {
        // Seed with demo packets that have NOW timestamps so charts show data immediately
        seedDemoPackets(20);
        // Start continuously generating demo packets (simulate live traffic)
        startDemoMode();
    }

    /** Seed a batch of packets spread across the last `periodSec` seconds. */
    private void seedDemoPackets(int count) {
        long now = System.currentTimeMillis();
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            long ts = now - rnd.nextInt(30) * 1000L; // spread over last 30s
            addDemoPacket(ts, rnd);
        }
    }

    /** Start a scheduler that injects 1-3 new demo packets every second. */
    public void startDemoMode() {
        if (demoMode) return;
        demoMode = true;
        demoScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "demo-traffic");
            t.setDaemon(true);
            return t;
        });
        Random rnd = new Random();
        demoScheduler.scheduleAtFixedRate(() -> {
            int burst = 1 + rnd.nextInt(3); // 1–3 packets per tick
            long now  = System.currentTimeMillis();
            for (int i = 0; i < burst; i++) addDemoPacket(now, rnd);
        }, 0, 800, TimeUnit.MILLISECONDS);
    }

    public void stopDemoMode() {
        if (demoScheduler != null) demoScheduler.shutdownNow();
        demoMode = false;
    }

    private void addDemoPacket(long ts, Random rnd) {
        String[] host = DEMO_HOSTS[rnd.nextInt(DEMO_HOSTS.length)];
        String[] req  = DEMO_REQUESTS[rnd.nextInt(DEMO_REQUESTS.length)];
        int size      = Integer.parseInt(req[2]) + rnd.nextInt(500);
        boolean reply = rnd.nextBoolean();
        addPacket(new HttpFrame(
            nextId(),
            reply ? host[1] : host[0],
            reply ? host[0] : host[1],
            reply ? host[3] : host[2],
            reply ? host[2] : host[3],
            reply ? 80 : 40000 + rnd.nextInt(10000),
            reply ? 40000 + rnd.nextInt(10000) : 80,
            req[0], req[1], size, ts
        ));
    }

    private synchronized int nextId() { return ++packetCounter; }

    public synchronized void addPacket(HttpFrame p) {
        packets.addLast(p);
        while (packets.size() > MAX_STORE) packets.removeFirst();
        listeners.forEach(l -> l.accept(p));
    }

    public synchronized void clear() { packets.clear(); }

    public synchronized List<HttpFrame> listAll() {
        return new ArrayList<>(packets);
    }

    /** Register a callback invoked each time a new packet arrives. */
    public void addListener(Consumer<HttpFrame> listener) {
        listeners.add(listener);
    }

    public synchronized List<HttpFrame> filterPackets(
            String ip, String protocol, String direction, int limit) {

        var stream = new ArrayList<>(packets).stream();

        if (ip != null && !ip.isBlank()) {
            String t = ip.trim();
            if      ("from".equalsIgnoreCase(direction)) stream = stream.filter(p -> p.getSrcIp().equals(t));
            else if ("to".equalsIgnoreCase(direction))   stream = stream.filter(p -> p.getDstIp().equals(t));
            else stream = stream.filter(p -> p.getSrcIp().equals(t) || p.getDstIp().equals(t));
        }
        if (protocol != null && !protocol.isBlank()) {
            String pt = protocol.trim();
            stream = stream.filter(p -> p.getMethode().toLowerCase().contains(pt.toLowerCase())
                                     || pt.equalsIgnoreCase("HTTP"));
        }

        List<HttpFrame> filtered = stream
            .sorted(Comparator.comparingLong(HttpFrame::getTimestamp).reversed())
            .collect(Collectors.toList());

        if (limit <= 0) limit = 500;
        return filtered.size() > limit ? filtered.subList(0, limit) : filtered;
    }

    public synchronized PacketStats computeStats(String ip, String protocol, int periodSec) {
        if (periodSec <= 0) periodSec = 30;
        long from = System.currentTimeMillis() - periodSec * 1000L;
        List<HttpFrame> window = filterPackets(ip, protocol, "", Integer.MAX_VALUE)
            .stream().filter(p -> p.getTimestamp() >= from).collect(Collectors.toList());
        return PacketStats.compute(window, periodSec);
    }
}
