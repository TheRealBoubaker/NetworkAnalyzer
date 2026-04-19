package org.example;

import model.HttpFrame;
import ui.SessionPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        List<HttpFrame> trames = creerDonneesTest();

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Session Viewer — Test");
            frame.setSize(1000, 650);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            SessionPanel sessionPanel = new SessionPanel(trames);
            frame.add(sessionPanel);

            frame.setVisible(true);
        });
    }

    private static List<HttpFrame> creerDonneesTest() {

        List<HttpFrame> trames = new ArrayList<>();
        long now = System.currentTimeMillis();

        trames.add(new HttpFrame(1,
                "192.168.1.10", "93.184.216.34",
                "AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02",
                54321, 80,
                "GET", "/index.html",
                450, now - 5000));

        trames.add(new HttpFrame(2,
                "93.184.216.34", "192.168.1.10",
                "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01",
                80, 54321,
                "HTTP/1.1 200 OK", "",
                1240, now - 4800));

        trames.add(new HttpFrame(3,
                "192.168.1.10", "93.184.216.34",
                "AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02",
                54321, 80,
                "GET", "/style.css",
                380, now - 4000));

        trames.add(new HttpFrame(4,
                "93.184.216.34", "192.168.1.10",
                "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01",
                80, 54321,
                "HTTP/1.1 200 OK", "",
                8900, now - 3800));

        trames.add(new HttpFrame(5,
                "192.168.1.10", "93.184.216.34",
                "AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02",
                54321, 80,
                "POST", "/login",
                620, now - 3000));

        trames.add(new HttpFrame(6,
                "93.184.216.34", "192.168.1.10",
                "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01",
                80, 54321,
                "HTTP/1.1 302 Found", "",
                210, now - 2800));

        trames.add(new HttpFrame(7,
                "192.168.1.10", "93.184.216.34",
                "AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02",
                54321, 80,
                "GET", "/dashboard",
                390, now - 2000));

        trames.add(new HttpFrame(8,
                "93.184.216.34", "192.168.1.10",
                "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01",
                80, 54321,
                "HTTP/1.1 200 OK", "",
                5400, now - 1800));

        return trames;
    }
}