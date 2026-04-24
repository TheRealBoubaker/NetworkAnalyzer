package com.networkanalyzer.capture;

import org.pcap4j.core.*;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Pcap4jRobot implements NetworkRobot {

    private static final int SNAP_LEN       = 65536;
    private static final int READ_TIMEOUT_MS = 10;

    private volatile PcapHandle activeHandle;
    private volatile boolean     running = false;

    @Override
    public List<PcapNetworkInterface> listNetworkInterfaces() throws Exception {
        List<PcapNetworkInterface> nifs = Pcaps.findAllDevs();
        if (nifs == null || nifs.isEmpty()) {
            throw new RuntimeException(
                "No interfaces found. Check admin/sudo rights and that Npcap is installed.");
        }
        return nifs;
    }

    @Override
    public void capture(String ifaceName, int count, String bpfFilter,
                        Consumer<org.pcap4j.packet.Packet> callback) throws Exception {
        PcapNetworkInterface nif = Pcaps.getDevByName(ifaceName);
        if (nif == null) throw new IllegalArgumentException("Interface not found: " + ifaceName);

        activeHandle = nif.openLive(SNAP_LEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT_MS);
        applyFilter(bpfFilter);
        running = true;

        try {
            activeHandle.loop(count, (PacketListener) packet -> {
                if (running) callback.accept(packet);
            });
        } finally {
            closeHandle();
        }
    }

    @Override
    public void captureForDuration(String ifaceName, int durationSeconds,
                                   String bpfFilter,
                                   Consumer<org.pcap4j.packet.Packet> callback) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Thread captureThread = new Thread(() -> {
            try {
                PcapNetworkInterface nif = Pcaps.getDevByName(ifaceName);
                if (nif == null) throw new IllegalArgumentException("Interface not found: " + ifaceName);

                activeHandle = nif.openLive(SNAP_LEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT_MS);
                applyFilter(bpfFilter);
                running = true;

                activeHandle.loop(-1, (PacketListener) packet -> {
                    if (running) callback.accept(packet);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Capture error: " + e.getMessage());
            } finally {
                closeHandle();
                scheduler.shutdown();
            }
        }, "pcap-capture-thread");

        scheduler.schedule(() -> stopCapture(), durationSeconds, TimeUnit.SECONDS);
        captureThread.start();
    }

    @Override
    public void stopCapture() {
        running = false;
        if (activeHandle != null && activeHandle.isOpen()) {
            try { activeHandle.breakLoop(); } catch (NotOpenException ignored) {}
        }
    }

    @Override
    public String getHttpFilter() {
        return "tcp port 80 or tcp port 8080";
    }

    private void applyFilter(String filter) throws Exception {
        if (filter != null && !filter.isBlank()) {
            activeHandle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
        }
    }

    private void closeHandle() {
        if (activeHandle != null && activeHandle.isOpen()) {
            activeHandle.close();
        }
    }
}
