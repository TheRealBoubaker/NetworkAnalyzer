package com.packetviewer;

public class Main {
    public static void main(String[] args) {
        PacketService service = new PacketService();

        // Ici plus tard tu connectes la capture réelle (robot d'Anis) et tu pushes addPacket(...)
        // Pour le moment: dashboard fonctionne avec les paquets demo

        PacketWebServer.start(service);
        System.out.println("✅ Dashboard: http://localhost:8081");
    }
}