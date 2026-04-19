package com.networkanalyzer;

import org.pcap4j.core.*;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import java.util.List;
import java.util.concurrent.*;

/**
 * Implémentation concrète de NetworkRobot utilisant Pcap4j.
 * Pcap4j est un wrapper Java autour de libpcap (Linux/macOS)
 * et WinPcap/Npcap (Windows).
 */
public class Pcap4jRobot implements NetworkRobot {

    // Taille du buffer de capture en octets (64 Ko)
    private static final int SNAP_LEN = 65536;

    // Timeout entre chaque appel interne de pcap en millisecondes
    private static final int READ_TIMEOUT_MS = 10;

    // --------------------------------------------------------
    // 1. Lister les interfaces réseau
    // --------------------------------------------------------
    @Override
    public List<PcapNetworkInterface> listNetworkInterfaces() throws Exception {
        List<PcapNetworkInterface> nifs = Pcaps.findAllDevs();

        if (nifs == null || nifs.isEmpty()) {
            throw new RuntimeException(
                "Aucune interface trouvée. Vérifiez vos droits (sudo/admin) " +
                "et que Npcap est installé."
            );
        }

        System.out.println("=== Interfaces réseau disponibles ===");
        for (int i = 0; i < nifs.size(); i++) {
            PcapNetworkInterface nif = nifs.get(i);
            System.out.printf("[%d] %s  →  %s%n",
                i,
                nif.getName(),
                nif.getDescription() != null ? nif.getDescription() : "sans description"
            );
        }
        return nifs;
    }

    // --------------------------------------------------------
    // 2. Capture instantanée (n paquets)
    // --------------------------------------------------------

    /**
     * PcapHandle = le "robinet" ouvert sur le réseau.
     * PacketListener = callback appelé automatiquement pour chaque paquet.
     */
    @Override
    public void capture(String ifaceName, int count, String bpfFilter) throws Exception {

        // Trouver l'interface par son nom
        PcapNetworkInterface nif = Pcaps.getDevByName(ifaceName);
        if (nif == null) {
            throw new IllegalArgumentException("Interface introuvable : " + ifaceName);
        }

        // Ouvrir le handle (le robinet réseau)
        PcapHandle handle = nif.openLive(
            SNAP_LEN,
            PromiscuousMode.PROMISCUOUS,
            READ_TIMEOUT_MS
        );

        // Appliquer le filtre BPF si fourni
        if (bpfFilter != null && !bpfFilter.isEmpty()) {
            handle.setFilter(bpfFilter, BpfProgram.BpfCompileMode.OPTIMIZE);
        }

        // Définir le callback appelé pour chaque paquet capturé
        PacketListener listener = packet -> {
            System.out.println("─── Paquet capturé ───");
            System.out.println(packet);
        };

        System.out.printf("Capture de %d paquet(s) sur %s [filtre: \"%s\"]%n",
            count, ifaceName, bpfFilter.isEmpty() ? "aucun" : bpfFilter);

        try {
            handle.loop(count, listener);
        } finally {
            handle.close(); // Toujours fermer le handle
            System.out.println("Handle fermé proprement.");
        }
    }

    // --------------------------------------------------------
    // 3. Capture timer (non bloquant via Thread)
    // --------------------------------------------------------

    /**
     * POURQUOI UN THREAD ?
     * handle.loop(-1, listener) bloque indéfiniment.
     * On le lance dans un Thread séparé, et on planifie
     * un handle.breakLoop() après durationSeconds secondes.
     * Le programme principal reste libre pendant la capture.
     */
    @Override
    public void captureForDuration(String ifaceName, int durationSeconds, String bpfFilter) {

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        PcapHandle[] handleRef = new PcapHandle[1];

        // Thread de capture (tourne en arrière-plan)
        Thread captureThread = new Thread(() -> {
            try {
                PcapNetworkInterface nif = Pcaps.getDevByName(ifaceName);
                if (nif == null) throw new IllegalArgumentException("Interface introuvable : " + ifaceName);

                handleRef[0] = nif.openLive(SNAP_LEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT_MS);

                if (bpfFilter != null && !bpfFilter.isEmpty()) {
                    handleRef[0].setFilter(bpfFilter, BpfProgram.BpfCompileMode.OPTIMIZE);
                }

                PacketListener listener = packet ->
                    System.out.printf("[%s] %s | %d octets%n",
                        handleRef[0].getTimestamp(),
                        packet.getClass().getSimpleName(),
                        packet.length()
                    );

                System.out.printf("Capture démarrée sur %s pour %ds...%n",
                    ifaceName, durationSeconds);

                handleRef[0].loop(-1, listener); // Capture infinie jusqu'à breakLoop()

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Erreur capture : " + e.getMessage());
            } finally {
                if (handleRef[0] != null && handleRef[0].isOpen()) {
                    handleRef[0].close();
                }
                scheduler.shutdown();
                System.out.println("Capture terminée.");
            }
        }, "pcap-capture-thread");

        // Tâche d'arrêt planifiée après durationSeconds
        scheduler.schedule(() -> {
            System.out.println("Timeout atteint : arrêt...");
            if (handleRef[0] != null && handleRef[0].isOpen()) {
                try {
                    handleRef[0].breakLoop();
                } catch (NotOpenException e) {
                    System.err.println("Handle déjà fermé.");
                }
            }
        }, durationSeconds, TimeUnit.SECONDS);

        captureThread.start(); // Lance sans bloquer le main
    }

    // --------------------------------------------------------
    // 4. Filtre BPF pour HTTP
    // --------------------------------------------------------
    @Override
    public String getHttpFilter() {
        // Capture tout le trafic TCP sur les ports HTTP classiques
        return "tcp port 80 or tcp port 8080";
    }
}