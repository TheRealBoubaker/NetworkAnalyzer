package com.networkanalyzer;

import org.pcap4j.core.PcapNetworkInterface;
import java.util.List;

/**
 * Classe de démonstration.
 * Lance avec : sudo mvn exec:java (droits admin requis pour libpcap)
 */
public class Main {

    public static void main(String[] args) throws Exception {

        NetworkRobot robot = new Pcap4jRobot();

        // 1. Lister les interfaces disponibles
        List<PcapNetworkInterface> nifs = robot.listNetworkInterfaces();
        String iface = nifs.get(0).getName();

        // 2. Capture instantanée de 5 paquets HTTP
        System.out.println("\n>> Capture instantanée (5 paquets HTTP) :");
        robot.capture(iface, 5, robot.getHttpFilter());

        // 3. Capture timer 10 secondes (non bloquant)
        System.out.println("\n>> Capture timer 10s :");
        robot.captureForDuration(iface, 10, robot.getHttpFilter());

        // Preuve que le main n'est PAS bloqué
        for (int i = 1; i <= 3; i++) {
            System.out.println("Main libre : tick " + i);
            Thread.sleep(2000);
        }
    }
}