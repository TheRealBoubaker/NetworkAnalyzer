package com.networkanalyzer.analysis;

import com.networkanalyzer.model.HttpFrame;
import java.util.List;

public class MermaidGenerator {

    public static String generer(List<HttpFrame> packets, String ipA, String ipB) {
        StringBuilder sb = new StringBuilder();
        sb.append("sequenceDiagram\n");
        sb.append("    participant A as ").append(ipA).append("\n");
        sb.append("    participant B as ").append(ipB).append("\n");

        for (HttpFrame f : packets) {
            String label = (f.getMethode() + " " + f.getUrl())
                    .replace("\"","'").replace(";",",").replace("\n"," ").replace("\r","");
            if (label.length() > 45) label = label.substring(0, 42) + "...";

            boolean fromA   = f.getSrcIp().equals(ipA);
            boolean isReq   = f.getMethode().matches("GET|POST|PUT|DELETE|HEAD|PATCH.*");
            boolean isRes   = f.getMethode().startsWith("HTTP/");

            String arrow = isRes ? "-->>" : "->>";
            if (fromA) sb.append("    A").append(arrow).append("B: ").append(label).append("\n");
            else       sb.append("    B").append(arrow).append("A: ").append(label).append("\n");
        }
        return sb.toString();
    }

    /** Build a self-contained HTML page that renders the Mermaid diagram inline. */
    public static String buildHtml(String mermaidCode) {
        return "<!DOCTYPE html>\n<html><head><meta charset='UTF-8'>\n"
                + "<style>body{font-family:sans-serif;background:#f0f4f8;display:flex;"
                + "flex-direction:column;align-items:center;padding:30px;}"
                + "h2{color:#2c3e50;}#d{background:white;border-radius:12px;padding:30px;"
                + "box-shadow:0 2px 10px rgba(0,0,0,.1);min-width:600px;}</style></head>\n"
                + "<body><h2>HTTP Sequence Diagram</h2><div id='d'>Loading...</div>\n"
                + "<script type='module'>\n"
                + "import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';\n"
                + "mermaid.initialize({startOnLoad:false,theme:'forest'});\n"
                + "(async()=>{"
                + "const el=document.querySelector('#d');"
                + "const def=`\n" + mermaidCode + "`;\n"
                + "const{svg}=await mermaid.render('g',def);"
                + "el.innerHTML=svg;})();\n"
                + "</script></body></html>\n";
    }
}
