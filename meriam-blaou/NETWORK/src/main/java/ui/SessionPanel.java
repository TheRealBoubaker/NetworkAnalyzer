package ui;

import analysis.MermaidGenerator;
import analysis.SessionAnalyser;
import model.HttpFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SessionPanel extends JPanel {

    private List<HttpFrame> trames;

    private JTextField champIpA   = new JTextField("", 13);
    private JTextField champIpB   = new JTextField("", 13);
    private JTextField champDebut = new JTextField("00:00:00", 9);
    private JTextField champFin   = new JTextField("23:59:59", 9);

    private DefaultTableModel tableModel;
    private JTextArea         zoneMermaid;
    private JTextArea         zoneReqRep;
    public SessionPanel(List<HttpFrame> trames) {
        this.trames = trames;
        setLayout(new BorderLayout(5, 5));
        add(creerBarreFiltre(), BorderLayout.NORTH);

        JTabbedPane onglets = new JTabbedPane();
        onglets.addTab("Paquets de la session",
                creerOngletPaquets());
        onglets.addTab("Diagramme de séquence",
                creerOngletSequence());
        onglets.addTab("Requêtes / Réponses",
                creerOngletReqRep());

        add(onglets, BorderLayout.CENTER);
    }

    private JPanel creerBarreFiltre() {
        JPanel p = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setBorder(BorderFactory.createTitledBorder(
                "Session Viewer — Filtre"));

        p.add(new JLabel("IP A :"));
        p.add(champIpA);
        p.add(new JLabel("IP B :"));
        p.add(champIpB);
        p.add(new JLabel("Début HH:mm:ss :"));
        p.add(champDebut);
        p.add(new JLabel("Fin HH:mm:ss :"));
        p.add(champFin);

        JButton btn = new JButton("Rechercher");
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.addActionListener(e -> rechercherSession());
        p.add(btn);

        return p;
    }

    private JPanel creerOngletPaquets() {
        JPanel p = new JPanel(new BorderLayout());

        String[] cols = {
                "N°", "IP Source", "Port Src",
                "IP Dest",  "Port Dst",
                "Méthode",  "URL",  "Taille (o)"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setRowHeight(22);
        table.getTableHeader().setFont(
                new Font("SansSerif", Font.BOLD, 12));


        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);
        table.getColumnModel().getColumn(6).setPreferredWidth(200);
        table.getColumnModel().getColumn(7).setPreferredWidth(80);

        p.add(new JScrollPane(table), BorderLayout.CENTER);

        // Label compteur en bas
        JLabel lblCount = new JLabel(" Total : 0 paquet(s)");
        lblCount.setFont(new Font("SansSerif", Font.PLAIN, 12));
        p.add(lblCount, BorderLayout.SOUTH);

        return p;
    }

    private JPanel creerOngletSequence() {
        JPanel p = new JPanel(new BorderLayout(5, 5));

        zoneMermaid = new JTextArea();
        zoneMermaid.setFont(new Font("Monospaced", Font.PLAIN, 13));
        zoneMermaid.setEditable(false);
        zoneMermaid.setBackground(new Color(248, 249, 250));

        JButton btnOuvrir = new JButton(
                "Ouvrir le diagramme dans le navigateur");
        btnOuvrir.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnOuvrir.setBackground(new Color(52, 152, 219));
        btnOuvrir.setForeground(Color.WHITE);
        btnOuvrir.addActionListener(e -> ouvrirDiagramme());

        p.add(new JScrollPane(zoneMermaid), BorderLayout.CENTER);
        p.add(btnOuvrir, BorderLayout.SOUTH);
        return p;
    }
    private JPanel creerOngletReqRep() {
        JPanel p = new JPanel(new BorderLayout());

        zoneReqRep = new JTextArea();
        zoneReqRep.setFont(new Font("Monospaced", Font.PLAIN, 13));
        zoneReqRep.setEditable(false);
        zoneReqRep.setBackground(new Color(248, 249, 250));

        p.add(new JScrollPane(zoneReqRep), BorderLayout.CENTER);
        return p;
    }

    private void rechercherSession() {

        String ipA    = champIpA.getText().trim();
        String ipB    = champIpB.getText().trim();
        long   startMs = heureVersMs(champDebut.getText().trim());
        long   endMs   = heureVersMs(champFin.getText().trim());

        if (ipA.isEmpty() || ipB.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez saisir IP A et IP B.",
                    "Champs vides",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<HttpFrame> paquets =
                SessionAnalyser.getPaquetsSession(
                        trames, ipA, ipB, startMs, endMs);

        // ── Onglet 1 : remplir tableau ──
        tableModel.setRowCount(0);
        for (HttpFrame f : paquets) {
            tableModel.addRow(new Object[]{
                    f.getNumero(),
                    f.getSrcIp(),   f.getSrcPort(),
                    f.getDstIp(),   f.getDstPort(),
                    f.getMethode(), f.getUrl(),
                    f.getTaille()
            });
        }

        if (paquets.isEmpty()) {
            zoneMermaid.setText("Aucun paquet trouvé.");
        } else {
            zoneMermaid.setText(
                    MermaidGenerator.generer(paquets, ipA, ipB));
        }

        zoneReqRep.setText(
                SessionAnalyser.getRequetesReponses(paquets));

        if (paquets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Aucun paquet trouvé entre\n"
                            + ipA + "  et  " + ipB + "\n"
                            + "dans cet intervalle de temps.",
                    "Résultat vide",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void ouvrirDiagramme() {

        String code = zoneMermaid.getText();

        if (code.isEmpty() || code.equals("Aucun paquet trouvé.")) {
            JOptionPane.showMessageDialog(this,
                    "Lance d'abord une recherche.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            File tmp = File.createTempFile("sequence_", ".html");
            tmp.deleteOnExit();
            Files.writeString(tmp.toPath(), buildHtml(code));
            Desktop.getDesktop().browse(tmp.toURI());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erreur : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildHtml(String codeMermaid) {
        return "<!DOCTYPE html>\n"
                + "<html lang='fr'>\n"
                + "<head>\n"
                + "  <meta charset='UTF-8'>\n"
                + "  <title>Sequence Diagram</title>\n"
                + "  <style>\n"
                + "    body {\n"
                + "      font-family: sans-serif;\n"
                + "      background: #f0f4f8;\n"
                + "      display: flex;\n"
                + "      flex-direction: column;\n"
                + "      align-items: center;\n"
                + "      padding: 40px;\n"
                + "    }\n"
                + "    h2 { color: #2c3e50; }\n"
                + "    #diagram-container {\n"
                + "      background: white;\n"
                + "      border-radius: 12px;\n"
                + "      padding: 30px;\n"
                + "      box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n"
                + "      min-width: 600px;\n"
                + "    }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <h2>Diagramme de sequence HTTP</h2>\n"
                + "  <div id='diagram-container'>"
                + "Chargement...</div>\n"
                + "  <script type='module'>\n"
                + "    import mermaid from\n"
                + "      'https://cdn.jsdelivr.net/npm/mermaid@10"
                + "/dist/mermaid.esm.min.mjs';\n"
                + "    mermaid.initialize({\n"
                + "      startOnLoad: false,\n"
                + "      theme: 'forest'\n"
                + "    });\n"
                + "    const drawDiagram = async () => {\n"
                + "      const element =\n"
                + "        document.querySelector('#diagram-container');\n"
                + "      const graphDefinition = `\n"
                + codeMermaid
                + "      `;\n"
                + "      const { svg } = await mermaid.render(\n"
                + "        'id-for-diagram', graphDefinition);\n"
                + "      element.innerHTML = svg;\n"
                + "    };\n"
                + "    drawDiagram();\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>\n";
    }

    private long heureVersMs(String heure) {
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("HH:mm:ss");
            Date d = sdf.parse(heure);
            Calendar hCal = Calendar.getInstance();
            Calendar now  = Calendar.getInstance();
            hCal.setTime(d);
            now.set(Calendar.HOUR_OF_DAY,
                    hCal.get(Calendar.HOUR_OF_DAY));
            now.set(Calendar.MINUTE,
                    hCal.get(Calendar.MINUTE));
            now.set(Calendar.SECOND,
                    hCal.get(Calendar.SECOND));
            now.set(Calendar.MILLISECOND, 0);
            return now.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    public void setTrames(List<HttpFrame> trames) {
        this.trames = trames;
    }
}