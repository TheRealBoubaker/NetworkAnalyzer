package com.packetviewer;

import com.google.gson.Gson;

import static spark.Spark.*;

public class PacketWebServer {

    public static void start(PacketService service) {
        port(8081);
        Gson gson = new Gson();

        staticFiles.location("/public"); // src/main/resources/public

        // --- CORS simple (utile si tu testes le HTML ailleurs) ---
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });
        options("/*", (req, res) -> "OK");

        // --- API: liste des paquets avec filtres ---
        // /api/packets?ip=...&protocol=HTTP&direction=from|to&limit=200
        get("/api/packets", (req, res) -> {
            res.type("application/json");
            String ip = req.queryParams("ip");
            String protocol = req.queryParams("protocol");
            String direction = req.queryParams("direction"); // from / to / (vide)
            int limit = parseIntOrDefault(req.queryParams("limit"), 200);
            return gson.toJson(service.filterPackets(ip, protocol, direction, limit));
        });

        // --- API: stats sur une période ---
        // /api/stats?ip=...&protocol=HTTP&periodSec=10
        get("/api/stats", (req, res) -> {
            res.type("application/json");
            String ip = req.queryParams("ip");
            String protocol = req.queryParams("protocol");
            int periodSec = parseIntOrDefault(req.queryParams("periodSec"), 10);
            return gson.toJson(service.computeStatsOverPeriod(ip, protocol, periodSec));
        });

        // --- API: vider l'historique ---
        post("/api/clear", (req, res) -> {
            service.clear();
            res.type("application/json");
            return "{\"ok\":true}";
        });

        // Home
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });
    }

    private static int parseIntOrDefault(String s, int def) {
        try {
            return (s == null || s.isBlank()) ? def : Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}