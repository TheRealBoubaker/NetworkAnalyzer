package analysis;

import model.HttpFrame;
import java.util.ArrayList;
import java.util.List;

public class SessionAnalyser {

    // ─────────────────────────────────────────────────
    // TÂCHE 1 : Paquets de la même session
    // Filtre par IP source + IP destination + intervalle
    // ─────────────────────────────────────────────────
    public static List<HttpFrame> getPaquetsSession(
            List<HttpFrame> toutes,
            String ipA,
            String ipB,
            long   startMs,
            long   endMs) {

        List<HttpFrame> resultat = new ArrayList<>();

        for (HttpFrame f : toutes) {

            boolean dansIntervalle =
                    f.getTimestamp() >= startMs
                            && f.getTimestamp() <= endMs;

            boolean bonCouple =
                    (f.getSrcIp().equals(ipA) && f.getDstIp().equals(ipB))
                            || (f.getSrcIp().equals(ipB) && f.getDstIp().equals(ipA));

            if (dansIntervalle && bonCouple)
                resultat.add(f);
        }
        return resultat;
    }

    // ─────────────────────────────────────────────────
    // TÂCHE 3 : Requêtes / Réponses HTTP
    // Groupe chaque requête avec sa réponse + délai ms
    // ─────────────────────────────────────────────────
    public static String getRequetesReponses(
            List<HttpFrame> paquets) {

        if (paquets.isEmpty())
            return "Aucun paquet trouvé.";

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════╗\n");
        sb.append("║       REQUÊTES / RÉPONSES HTTP           ║\n");
        sb.append("╚══════════════════════════════════════════╝\n\n");

        HttpFrame derniereRequete = null;

        for (HttpFrame f : paquets) {

            boolean estRequete =
                    f.getMethode().startsWith("GET")
                            || f.getMethode().startsWith("POST")
                            || f.getMethode().startsWith("PUT")
                            || f.getMethode().startsWith("DELETE")
                            || f.getMethode().startsWith("HEAD");

            boolean estReponse =
                    f.getMethode().startsWith("HTTP/");

            if (estRequete) {
                sb.append("┌─── REQUÊTE ──────────────────────────\n");
                sb.append("│ De      : ")
                        .append(f.getSrcIp()).append(":")
                        .append(f.getSrcPort()).append("\n");
                sb.append("│ Vers    : ")
                        .append(f.getDstIp()).append(":")
                        .append(f.getDstPort()).append("\n");
                sb.append("│ Méthode : ")
                        .append(f.getMethode()).append("\n");
                sb.append("│ URL     : ")
                        .append(f.getUrl()).append("\n");
                sb.append("│ Taille  : ")
                        .append(f.getTaille()).append(" octets\n");
                derniereRequete = f;

            } else if (estReponse) {
                sb.append("└─── RÉPONSE ──────────────────────────\n");
                sb.append("    Statut : ")
                        .append(f.getMethode()).append("\n");
                sb.append("    Taille : ")
                        .append(f.getTaille()).append(" octets\n");

                if (derniereRequete != null) {
                    long delai = f.getTimestamp()
                            - derniereRequete.getTimestamp();
                    sb.append("    Délai  : ")
                            .append(delai).append(" ms\n");
                    derniereRequete = null;
                }
                sb.append("───────────────────────────────────────\n\n");
            }
        }
        return sb.toString();
    }
}