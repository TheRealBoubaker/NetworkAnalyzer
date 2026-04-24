package com.networkanalyzer.ui;

import com.networkanalyzer.analysis.PacketService;
import com.networkanalyzer.model.HttpFrame;
import com.networkanalyzer.model.PacketStats;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.networkanalyzer.ui.UITheme.*;

/**
 * Module 2 – Packet Viewer (Iheb Abbes)
 * Replaced the Spark/web server with a pure Swing panel.
 */
public class PacketViewerPanel extends JPanel {

    private final PacketService      service;
    private       DefaultTableModel  tableModel;
    private       JTextField         ipField, periodField;
    private       JComboBox<String>  protoCombo, dirCombo;
    private       JLabel             lblCount, lblTableCount, lblBw, lblLoad, lblLatency, lblBer;
    private       Timer              liveTimer;

    public PacketViewerPanel(PacketService service) {
        this.service = service;
        setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildHeader(),   BorderLayout.NORTH);
        add(buildCenter(),   BorderLayout.CENTER);
        add(buildStatsBar(), BorderLayout.SOUTH);

        // Listen for new packets and refresh live
        service.addListener(p -> SwingUtilities.invokeLater(this::refreshTable));

        // Auto-refresh stats every 2s
        liveTimer = new Timer(2000, e -> refreshStats());
        liveTimer.start();

        refreshTable();
    }

    // ── Header ────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_DARK);
        outer.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));

        JLabel title = new JLabel("Packet Viewer");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        outer.add(title, BorderLayout.WEST);

        // Filter bar
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setBackground(BG_DARK);

        ipField    = styledField(12);
        ipField.setToolTipText("Filter by IP address");
        ((JTextField)ipField).putClientProperty("JTextField.placeholderText", "IP address...");

        protoCombo = styledCombo("All", "HTTP", "HTTPS", "TCP");
        dirCombo   = styledCombo("Both", "From", "To");
        periodField = styledField(4);
        periodField.setText("10");

        JButton btnFilter = primaryButton("Search");
        btnFilter.addActionListener(e -> refreshTable());

        JButton btnClear = dangerButton("Clear");
        btnClear.addActionListener(e -> { service.clear(); refreshTable(); });

        filters.add(mutedLabel("IP:"));       filters.add(ipField);
        filters.add(mutedLabel("Protocol:")); filters.add(protoCombo);
        filters.add(mutedLabel("Direction:")); filters.add(dirCombo);
        filters.add(mutedLabel("Period(s):")); filters.add(periodField);
        filters.add(btnFilter);
        filters.add(btnClear);

        outer.add(filters, BorderLayout.EAST);
        return outer;
    }

    // ── Center: packet table ──────────────────────────────────
    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));

        String[] cols = {"#","Timestamp","Src IP","Src Port","Dst IP","Dst Port","Method","URL","Size (B)"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);

        // Use DarkTableRenderer — forces colors regardless of Look & Feel
        DarkTableRenderer.applyTo(table, 6); // col 6 = Method

        int[] widths = {40,130,120,70,120,70,150,180,70};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(table);
        styleScroll(sp);

        lblTableCount = new JLabel("  0 packets");
        lblTableCount.setFont(FONT_SMALL);
        lblTableCount.setForeground(TEXT_MUTED);
        lblTableCount.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));

        p.add(sp, BorderLayout.CENTER);
        p.add(lblTableCount, BorderLayout.SOUTH);
        return p;
    }

    // ── Stats bar at bottom ───────────────────────────────────
    private JPanel buildStatsBar() {
        JPanel p = new JPanel(new GridLayout(1, 5, 1, 0));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));

        lblBw      = buildStatCard("Bandwidth",  "— kbps",  ACCENT_BLUE);
        lblLoad    = buildStatCard("Load",        "— pps",   ACCENT_GREEN);
        lblLatency = buildStatCard("Latency",     "— ms",    ACCENT_AMBER);
        lblBer     = buildStatCard("BER",         "—",       ACCENT_RED);
        lblCount   = buildStatCard("Packets",     "0",       TEXT_MUTED);

        // These are the value labels returned by buildStatCard wrapper
        p.add(wrapStat("Bandwidth",  lblBw));
        p.add(wrapStat("Load",       lblLoad));
        p.add(wrapStat("Avg Latency",lblLatency));
        p.add(wrapStat("BER",        lblBer));
        p.add(wrapStat("Total Pkts", lblCount));
        return p;
    }

    private JLabel buildStatCard(String title, String init, Color color) {
        JLabel lbl = new JLabel(init, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(color);
        return lbl;
    }

    private JPanel wrapStat(String title, JLabel valueLbl) {
        JPanel card = new JPanel(new BorderLayout(0,2));
        card.setBackground(BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(8,12,8,12)));

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(FONT_SMALL);
        t.setForeground(TEXT_MUTED);

        card.add(t,        BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        return card;
    }

    // ── Logic ─────────────────────────────────────────────────
    private void refreshTable() {
        String ip    = ipField.getText().trim();
        String proto = (String) protoCombo.getSelectedItem();
        String dir   = ((String) dirCombo.getSelectedItem()).toLowerCase();

        List<HttpFrame> packets = service.filterPackets(
            ip.isEmpty() ? null : ip,
            "All".equals(proto) ? null : proto,
            "both".equals(dir)  ? null : dir,
            500);

        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        for (HttpFrame f : packets) {
            tableModel.addRow(new Object[]{
                f.getNumero(),
                sdf.format(new Date(f.getTimestamp())),
                f.getSrcIp(),   f.getSrcPort(),
                f.getDstIp(),   f.getDstPort(),
                f.getMethode(), f.getUrl(),
                f.getTaille()
            });
        }
        lblTableCount.setText("  " + packets.size() + " packet(s)");
    }

    private void refreshStats() {
        int period;
        try { period = Integer.parseInt(periodField.getText().trim()); }
        catch (Exception e) { period = 10; }

        String ip    = ipField.getText().trim();
        String proto = (String) protoCombo.getSelectedItem();
        PacketStats s = service.computeStats(
            ip.isEmpty() ? null : ip,
            "All".equals(proto) ? null : proto,
            period);

        lblBw.setText(String.format("%.1f kbps", s.bandwidthKbps));
        lblLoad.setText(String.format("%.2f pps", s.loadPps));
        lblLatency.setText(String.format("%.1f ms", s.averageLatencyMs));
        lblBer.setText(String.format("%.6f", s.BER));
        lblCount.setText(String.valueOf(s.packetCount));
    }
}
