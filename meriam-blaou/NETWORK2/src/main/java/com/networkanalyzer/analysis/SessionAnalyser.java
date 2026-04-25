package com.networkanalyzer.analysis;

import com.networkanalyzer.model.HttpFrame;
import java.util.ArrayList;
import java.util.List;

public class SessionAnalyser {

    public static List<HttpFrame> getPaquetsSession(
            List<HttpFrame> all, String ipA, String ipB, long startMs, long endMs) {

        List<HttpFrame> result = new ArrayList<>();
        for (HttpFrame f : all) {
            boolean inTime = f.getTimestamp() >= startMs && f.getTimestamp() <= endMs;
            boolean match  = (f.getSrcIp().equals(ipA) && f.getDstIp().equals(ipB))
                    || (f.getSrcIp().equals(ipB) && f.getDstIp().equals(ipA));
            if (inTime && match) result.add(f);
        }
        return result;
    }

    public static String getRequetesReponses(List<HttpFrame> packets) {
        if (packets.isEmpty()) return "No packets found.";

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════╗\n");
        sb.append("║         HTTP REQUESTS / RESPONSES        ║\n");
        sb.append("╚══════════════════════════════════════════╝\n\n");

        HttpFrame lastRequest = null;
        for (HttpFrame f : packets) {
            boolean isReq = f.getMethode().matches("GET|POST|PUT|DELETE|HEAD|PATCH.*");
            boolean isRes = f.getMethode().startsWith("HTTP/");

            if (isReq) {
                sb.append("┌─── REQUEST ───────────────────────────\n");
                sb.append("│ From   : ").append(f.getSrcIp()).append(":").append(f.getSrcPort()).append("\n");
                sb.append("│ To     : ").append(f.getDstIp()).append(":").append(f.getDstPort()).append("\n");
                sb.append("│ Method : ").append(f.getMethode()).append("\n");
                sb.append("│ URL    : ").append(f.getUrl()).append("\n");
                sb.append("│ Size   : ").append(f.getTaille()).append(" bytes\n");
                lastRequest = f;
            } else if (isRes) {
                sb.append("└─── RESPONSE ──────────────────────────\n");
                sb.append("    Status : ").append(f.getMethode()).append("\n");
                sb.append("    Size   : ").append(f.getTaille()).append(" bytes\n");
                if (lastRequest != null) {
                    sb.append("    Delay  : ").append(f.getTimestamp() - lastRequest.getTimestamp()).append(" ms\n");
                    lastRequest = null;
                }
                sb.append("───────────────────────────────────────\n\n");
            }
        }
        return sb.toString();
    }
}
