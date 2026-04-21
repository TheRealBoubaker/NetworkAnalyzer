package com.networkanalyzer;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.Packet;
import java.util.List;

/**
 * Interface principale de la bibliothèque d'analyse réseau.
 * Définit le CONTRAT que toute implémentation doit respecter.
 */
public interface NetworkRobot {

    /**
     * Liste toutes les interfaces réseau disponibles sur la machine.
     * ex: eth0, wlan0, lo sur Linux / Wi-Fi, Ethernet sur Windows
     */
    List<PcapNetworkInterface> listNetworkInterfaces() throws Exception;

    /**
     * Capture un nombre fixe de paquets sur une interface donnée.
     * @param ifaceName  Nom de l'interface (ex: "eth0")
     * @param count      Nombre de paquets à capturer
     * @param bpfFilter  Filtre BPF (ex: "tcp port 80"), ou "" pour tout
     */
    void capture(String ifaceName, int count, String bpfFilter) throws Exception;

    /**
     * Capture des paquets pendant une durée définie en secondes.
     * N'est PAS bloquant grâce à un Thread séparé.
     * @param ifaceName       Nom de l'interface
     * @param durationSeconds Durée de capture en secondes
     * @param bpfFilter       Filtre BPF optionnel
     */
    void captureForDuration(String ifaceName, int durationSeconds, String bpfFilter);

    /**
     * Retourne le filtre BPF pour isoler uniquement le trafic HTTP.
     */
    String getHttpFilter();
}