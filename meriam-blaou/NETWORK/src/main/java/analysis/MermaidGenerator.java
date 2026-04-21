package analysis;

import model.HttpFrame;
import java.util.List;

public class MermaidGenerator {

    public static String generer(
            List<HttpFrame> paquets,
            String ipA,
            String ipB) {

        StringBuilder sb = new StringBuilder();
        sb.append("sequenceDiagram\n");
        sb.append("    participant A as ").append(ipA).append("\n");
        sb.append("    participant B as ").append(ipB).append("\n");

        for (HttpFrame f : paquets) {

            String label = f.getMethode() + " " + f.getUrl();
            label = label.replace("\"", "'")
                    .replace(";",  ",")
                    .replace("\n", " ")
                    .replace("\r", "");

            if (label.length() > 45)
                label = label.substring(0, 42) + "...";

            boolean deA = f.getSrcIp().equals(ipA);

            boolean estRequete =
                    f.getMethode().startsWith("GET")
                            || f.getMethode().startsWith("POST")
                            || f.getMethode().startsWith("PUT")
                            || f.getMethode().startsWith("DELETE")
                            || f.getMethode().startsWith("HEAD");

            boolean estReponse =
                    f.getMethode().startsWith("HTTP/");

            if (estRequete) {
                if (deA)
                    sb.append("    A->>B: ")
                            .append(label).append("\n");
                else
                    sb.append("    B->>A: ")
                            .append(label).append("\n");

            } else if (estReponse) {
                if (deA)
                    sb.append("    A-->>B: ")
                            .append(label).append("\n");
                else
                    sb.append("    B-->>A: ")
                            .append(label).append("\n");
            }
        }
        return sb.toString();
    }
}