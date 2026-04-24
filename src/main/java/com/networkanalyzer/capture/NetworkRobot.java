package com.networkanalyzer.capture;

import org.pcap4j.core.PcapNetworkInterface;
import java.util.List;
import java.util.function.Consumer;

/**
 * Contract for packet capture implementations.
 */
public interface NetworkRobot {
    List<PcapNetworkInterface> listNetworkInterfaces() throws Exception;
    void capture(String ifaceName, int count, String bpfFilter,
                 Consumer<org.pcap4j.packet.Packet> callback) throws Exception;
    void captureForDuration(String ifaceName, int durationSeconds,
                            String bpfFilter,
                            Consumer<org.pcap4j.packet.Packet> callback);
    void stopCapture();
    String getHttpFilter();
}
