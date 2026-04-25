package com.networkanalyzer.ui;

import com.networkanalyzer.analysis.MermaidGenerator;
import com.networkanalyzer.analysis.PacketService;
import com.networkanalyzer.analysis.SessionAnalyser;
import com.networkanalyzer.model.HttpFrame;
import com.networkanalyzer.model.PacketStats;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.networkanalyzer.ui.UITheme.*;

public class SessionViewerPanel extends JPanel {

    private final PacketService     service;
    private JTextField              champIpA, champIpB, champDebut, champFin;
    private DefaultTableModel       tableModel;
    private SequenceDiagramPanel    diagramPanel;
    private JTextArea               zoneReqRep;
    private JLabel                  lblStats;

    public SessionViewerPanel(PacketService service) {
        this.service = service;
        setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildTabs(),      BorderLayout.CENTER);
    }

    // ── Header with filter bar ────────────────────────────────
    private JPanel buildHeader() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG_DARK);
        outer.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));

        JLabel title = new JLabel("Session Viewer");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        outer.add(title, BorderLayout.NORTH);

        // Filter row
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filters.setBackground(BG_PANEL);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(6,8,6,8)));

        champIpA   = styledField(13);
        champIpB   = styledField(13);
        champDebut = styledField(9);  champDebut.setText("00:00:00");
        champFin   = styledField(9);  champFin.setText("23:59:59");

        JButton btnSearch = successButton("Search Session");
        btnSearch.addActionListener(e -> rechercherSession());

        JButton btnFill = primaryButton("Auto-fill from packets");
        btnFill.setBackground(BG_CARD);
        btnFill.addActionListener(e -> autoFill());

        filters.add(mutedLabel("IP A:")); filters.add(champIpA);
        filters.add(mutedLabel("IP B:")); filters.add(champIpB);
        filters.add(mutedLabel("Start HH:mm:ss:")); filters.add(champDebut);
        filters.add(mutedLabel("End HH:mm:ss:"));   filters.add(champFin);
        filters.add(btnSearch);
        filters.add(btnFill);

        lblStats = new JLabel("");
        lblStats.setFont(FONT_SMALL);
        lblStats.setForeground(ACCENT_GREEN);
        filters.add(lblStats);

        outer.add(filters, BorderLayout.SOUTH);
        return outer;
    }

    // ── Tabbed pane ───────────────────────────────────────────
    private JTabbedPane buildTabs() {
        // Force tab colors before construction so Metal LnF can't override them
        UIManager.put("TabbedPane.background",           BG_PANEL);
        UIManager.put("TabbedPane.foreground",           TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected",             BG_CARD);
        UIManager.put("TabbedPane.selectedForeground",   TEXT_PRIMARY);
        UIManager.put("TabbedPane.unselectedBackground", BG_PANEL);
        UIManager.put("TabbedPane.unselectedForeground", TEXT_MUTED);
        UIManager.put("TabbedPane.tabInsets",            new Insets(6, 14, 6, 14));
        UIManager.put("TabbedPane.contentBorderInsets",  new Insets(2, 0, 0, 0));
        UIManager.put("TabbedPane.focus",                BG_CARD);
        UIManager.put("TabbedPane.borderHightlightColor",BORDER_COLOR);
        UIManager.put("TabbedPane.darkShadow",           BG_DARK);
        UIManager.put("TabbedPane.shadow",               BORDER_COLOR);
        UIManager.put("TabbedPane.highlight",            BG_CARD);
        UIManager.put("TabbedPane.light",                BG_PANEL);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG_PANEL);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setFont(FONT_HEADER);
        tabs.setOpaque(true);

        tabs.addTab("Session Packets",       buildPacketsTab());
        tabs.addTab("Sequence Diagram",      buildDiagramTab());
        tabs.addTab("Requests / Responses",  buildReqRepTab());
        return tabs;
    }

    // Tab 1 – packet table
    private JPanel buildPacketsTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);

        String[] cols = {"#","Src IP","Src Port","Dst IP","Dst Port","Method","URL","Size (B)"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        styleTable(table);
        DarkTableRenderer.applyTo(table, 5); // col 5 = Method
        int[] w = {40,120,70,120,70,150,200,80};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        JScrollPane sp = new JScrollPane(table);
        styleScroll(sp);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // Tab 2 – sequence diagram (pure Swing, no browser!)
    private JPanel buildDiagramTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);

        diagramPanel = new SequenceDiagramPanel();

        JScrollPane sp = new JScrollPane(diagramPanel);
        styleScroll(sp);

        JLabel hint = mutedLabel("  Run a search to see the sequence diagram here (no browser needed)");
        hint.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));

        p.add(hint, BorderLayout.NORTH);
        p.add(sp,   BorderLayout.CENTER);
        return p;
    }

    // Tab 3 – request/response text
    private JPanel buildReqRepTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);

        zoneReqRep = new JTextArea();
        zoneReqRep.setBackground(new Color(15,23,42));
        zoneReqRep.setForeground(new Color(167,243,208));
        zoneReqRep.setFont(FONT_MONO);
        zoneReqRep.setEditable(false);

        JScrollPane sp = new JScrollPane(zoneReqRep);
        styleScroll(sp);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Logic ─────────────────────────────────────────────────
    private void rechercherSession() {
        String ipA    = champIpA.getText().trim();
        String ipB    = champIpB.getText().trim();
        long   startMs = heureVersMs(champDebut.getText().trim());
        long   endMs   = heureVersMs(champFin.getText().trim());

        if (ipA.isEmpty() || ipB.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both IP A and IP B.", "Missing Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<HttpFrame> paquets = SessionAnalyser.getPaquetsSession(
                service.listAll(), ipA, ipB, startMs, endMs);

        // Tab 1 – fill table
        tableModel.setRowCount(0);
        for (HttpFrame f : paquets) {
            tableModel.addRow(new Object[]{
                    f.getNumero(), f.getSrcIp(), f.getSrcPort(),
                    f.getDstIp(), f.getDstPort(), f.getMethode(), f.getUrl(), f.getTaille()
            });
        }

        // Tab 2 – draw sequence diagram in Swing
        diagramPanel.setPackets(paquets, ipA, ipB);
        diagramPanel.repaint();

        // Tab 3 – req/response text
        zoneReqRep.setText(SessionAnalyser.getRequetesReponses(paquets));
        zoneReqRep.setCaretPosition(0);

        // Stats
        if (paquets.isEmpty()) {
            lblStats.setText("No packets found between " + ipA + " and " + ipB);
            lblStats.setForeground(ACCENT_RED);
        } else {
            PacketStats s = PacketStats.compute(paquets, 10);
            lblStats.setText(String.format(
                    "  %d packets  |  %.1f kbps  |  %.2f pps  |  %.1f ms latency",
                    paquets.size(), s.bandwidthKbps, s.loadPps, s.averageLatencyMs));
            lblStats.setForeground(ACCENT_GREEN);
        }
    }

    /** Auto-fill IP fields with the most common pair in captured packets */
    private void autoFill() {
        List<HttpFrame> all = service.listAll();
        if (all.isEmpty()) return;
        HttpFrame first = all.get(0);
        champIpA.setText(first.getSrcIp());
        champIpB.setText(first.getDstIp());
    }

    private long heureVersMs(String heure) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date d = sdf.parse(heure);
            Calendar hCal = Calendar.getInstance();
            Calendar now  = Calendar.getInstance();
            hCal.setTime(d);
            now.set(Calendar.HOUR_OF_DAY, hCal.get(Calendar.HOUR_OF_DAY));
            now.set(Calendar.MINUTE,      hCal.get(Calendar.MINUTE));
            now.set(Calendar.SECOND,      hCal.get(Calendar.SECOND));
            now.set(Calendar.MILLISECOND, 0);
            return now.getTimeInMillis();
        } catch (Exception e) { return 0; }
    }

    // ─────────────────────────────────────────────────────────
    // Inner class: renders a sequence diagram directly in Swing
    // ─────────────────────────────────────────────────────────
    static class SequenceDiagramPanel extends JPanel {

        private List<HttpFrame> packets;
        private String ipA = "Host A", ipB = "Host B";

        private static final int MARGIN     = 60;
        private static final int COL_LEFT   = 160;
        private static final int COL_RIGHT  = 580;
        private static final int ROW_START  = 110;
        private static final int ROW_STEP   = 62;   // was 50 — more vertical space per arrow
        private static final int ACTOR_W    = 160;
        private static final int ACTOR_H    = 40;

        public SequenceDiagramPanel() {
            setBackground(new Color(15, 23, 42));
            setPreferredSize(new Dimension(750, 400));
        }

        public void setPackets(List<HttpFrame> packets, String ipA, String ipB) {
            this.packets = packets;
            this.ipA     = ipA;
            this.ipB     = ipB;
            int h = ROW_START + packets.size() * ROW_STEP + 80;
            setPreferredSize(new Dimension(750, Math.max(400, h)));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (packets == null || packets.isEmpty()) {
                g2.setColor(TEXT_MUTED);
                g2.setFont(FONT_BODY);
                g2.drawString("Run a search to see the sequence diagram.", 180, 200);
                return;
            }

            int h = getHeight();

            // Lifeline A
            drawActor(g2, COL_LEFT - ACTOR_W/2, 20, ipA);
            g2.setColor(new Color(71,85,105));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{6,4}, 0));
            g2.drawLine(COL_LEFT, 20 + ACTOR_H, COL_LEFT, h - 20);

            // Lifeline B
            drawActor(g2, COL_RIGHT - ACTOR_W/2, 20, ipB);
            g2.drawLine(COL_RIGHT, 20 + ACTOR_H, COL_RIGHT, h - 20);

            g2.setStroke(new BasicStroke(1.5f));

            // Arrows
            int y = ROW_START;
            for (HttpFrame f : packets) {
                boolean fromA = f.getSrcIp().equals(ipA);
                boolean isRes = f.getMethode().startsWith("HTTP/");

                Color arrowColor = isRes ? ACCENT_GREEN : ACCENT_BLUE;
                g2.setColor(arrowColor);

                String label = f.getMethode();
                if (!f.getUrl().isEmpty()) label += " " + f.getUrl();
                if (label.length() > 40) label = label.substring(0,37) + "...";

                int x1 = fromA ? COL_LEFT  : COL_RIGHT;
                int x2 = fromA ? COL_RIGHT : COL_LEFT;

                // Dashed for responses
                if (isRes) {
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                            0, new float[]{5,3}, 0));
                } else {
                    g2.setStroke(new BasicStroke(1.5f));
                }
                g2.drawLine(x1, y, x2, y);
                g2.setStroke(new BasicStroke(1.5f));
                drawArrowHead(g2, x1, x2, y);

                // Label with proper semi-transparent background
                int lx = Math.min(x1, x2) + Math.abs(x2 - x1) / 2;
                g2.setFont(FONT_BODY);   // use FONT_BODY (13pt) instead of FONT_SMALL (11pt) for readability
                FontMetrics fm = g2.getFontMetrics();
                int lw = fm.stringWidth(label);
                // Draw background rectangle using AlphaComposite so it works on all platforms
                Composite originalComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                g2.setColor(new Color(15, 23, 42));
                g2.fillRoundRect(lx - lw / 2 - 4, y - fm.getAscent() - 3, lw + 8, fm.getHeight() + 2, 4, 4);
                g2.setComposite(originalComposite);
                g2.setColor(arrowColor);
                g2.drawString(label, lx - lw / 2, y - 3);

                y += ROW_STEP;
            }
        }

        private void drawActor(Graphics2D g2, int x, int y, String label) {
            g2.setColor(BG_CARD);
            g2.fillRoundRect(x, y, ACTOR_W, ACTOR_H, 8, 8);
            g2.setColor(ACCENT_BLUE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, ACTOR_W, ACTOR_H, 8, 8);
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(FONT_HEADER);
            FontMetrics fm = g2.getFontMetrics();
            String disp = label.length() > 15 ? label.substring(0,12) + "..." : label;
            g2.drawString(disp, x + (ACTOR_W - fm.stringWidth(disp)) / 2, y + 26);
        }

        private void drawArrowHead(Graphics2D g2, int x1, int x2, int y) {
            boolean rightward = x2 > x1;
            // tip is always the destination end (x2)
            int tip = x2;
            int[] px = rightward
                    ? new int[]{tip, tip - 10, tip - 10}   // arrow pointing right
                    : new int[]{tip, tip + 10, tip + 10};  // arrow pointing left
            int[] py = {y, y - 5, y + 5};
            g2.fillPolygon(px, py, 3);
        }
    }
}
