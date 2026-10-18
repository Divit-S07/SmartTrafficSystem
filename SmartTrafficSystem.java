import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import com.sun.net.httpserver.*;

/**
 * ============================================================================
 *  MINI PROJECT 12: SMART TRAFFIC MANAGEMENT AND EMERGENCY VEHICLE ROUTING
 *  City of Vegam - Traffic Control Desk Simulation
 *
 *  Enhanced Pure-Java Backend with:
 *   - Zero frameworks (Pure Java SE Standard Library com.sun.net.httpserver)
 *   - Built-in HTTP REST API & Static Web Server for HTML/CSS/JS frontend
 *   - MySQL Database Schema & JDBC Persistence Layer
 *   - Supabase Cloud Storage Snapshot Backup Integration
 *   - Dijkstra Routing, PriorityQueue Requests & FIFO Event Processing
 * ============================================================================
 */
public class SmartTrafficSystem {

    public static final int DEFAULT_PORT = 8080;
    private static TrafficController globalController = new TrafficController();
    private static DatabaseService dbService = new DatabaseService();
    private static SupabaseService supabaseService = new SupabaseService();
    private static final List<String> systemLogs = new CopyOnWriteArrayList<>();

    public static void log(String message) {
        String logEntry = String.format("[%tT] %s", new java.util.Date(), message);
        systemLogs.add(logEntry);
        if (systemLogs.size() > 500) {
            systemLogs.remove(0);
        }
        System.out.println(logEntry);
    }

    public static List<String> getSystemLogs() {
        return Collections.unmodifiableList(systemLogs);
    }

    public static void main(String[] args) {
        log("=============================================================");
        log(" VEGAM CITY - SMART TRAFFIC MANAGEMENT & EMERGENCY ROUTING");
        log("=============================================================");

        // Load configuration
        dbService.init();
        supabaseService.init();

        // System starts clean and empty. Sample dataset loads only when requested.
        log(">> System initialized with empty network. Ready for manual entry or sample load.");

        // Start HTTP Server on port 8080
        int port = DEFAULT_PORT;
        try {
            startHttpServer(port);
            log(">> HTTP Server started successfully at: http://localhost:" + port);
            log(">> Open your browser and navigate to: http://localhost:" + port);
        } catch (Exception e) {
            log("[!] Could not start HTTP server on port " + port + ": " + e.getMessage());
        }

        // If interactive console is available, provide console menu loop
        if (System.console() != null || (args.length > 0 && args[0].equalsIgnoreCase("--cli"))) {
            runConsoleLoop(globalController);
        } else {
            log(">> Running web service mode. Press Ctrl+C in terminal to stop.");
            // Keep main thread alive
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {}
        }
    }

    // ------------------------------------------------------------------
    // Built-in HTTP Server (Zero External Frameworks)
    // ------------------------------------------------------------------
    public static void startHttpServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Static files handler (serves public/ folder)
        server.createContext("/", new StaticFileHandler());

        // REST API Endpoints
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/clear", new ClearHandler());
        server.createContext("/api/junction", new AddJunctionHandler());
        server.createContext("/api/junction/delete", new DeleteJunctionHandler());
        server.createContext("/api/road", new AddRoadHandler());
        server.createContext("/api/road/delete", new DeleteRoadHandler());
        server.createContext("/api/road/update", new UpdateRoadHandler());
        server.createContext("/api/vehicle", new AddVehicleHandler());
        server.createContext("/api/vehicle/delete", new DeleteVehicleHandler());
        server.createContext("/api/vehicle/update", new UpdateVehicleHandler());
        server.createContext("/api/request", new SubmitRequestHandler());
        server.createContext("/api/request/delete", new DeleteRequestHandler());
        server.createContext("/api/request/update", new UpdateRequestHandler());
        server.createContext("/api/request/escalate", new EscalateRequestHandler());
        server.createContext("/api/event", new QueueEventHandler());
        server.createContext("/api/event/delete", new DeleteEventHandler());
        server.createContext("/api/event/process-next", new ProcessNextEventHandler());
        server.createContext("/api/event/process-all", new ProcessAllEventsHandler());
        server.createContext("/api/request/process-next", new ProcessNextRequestHandler());
        server.createContext("/api/request/process-all", new ProcessAllRequestsHandler());
        server.createContext("/api/vehicle/advance", new AdvanceVehicleHandler());
        server.createContext("/api/sample-data", new SampleDataHandler());
        server.createContext("/api/demo", new DemoHandler());
        server.createContext("/api/logs", new LogsHandler());
        server.createContext("/api/supabase/backup", new SupabaseBackupHandler());
        server.createContext("/api/supabase/config", new SupabaseConfigHandler());
        server.createContext("/api/export-sql", new ExportSqlHandler());

        server.start();
    }

    // ------------------------------------------------------------------
    // HTTP Handlers
    // ------------------------------------------------------------------

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Map path to public directory
            File file = new File("public" + path);
            if (!file.exists() || file.isDirectory()) {
                file = new File("E:/New folder/public" + path);
            }
            if (!file.exists() || file.isDirectory()) {
                file = new File("C:/Users/DIVIT S/.gemini/antigravity/scratch/SmartTrafficManagement/public" + path);
            }

            if (!file.exists() || file.isDirectory()) {
                byte[] notFound = "404 Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(notFound); }
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             Scanner s = new Scanner(is, StandardCharsets.UTF_8)) {
            return s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
    }

    // Simple JSON key-value extraction without any external library
    private static Map<String, String> parseJsonSimple(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
        }
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String json = buildStatusJson(globalController);
            sendJsonResponse(exchange, 200, json);
        }
    }

    static class ClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            globalController.clearAll();
            log("Web API: Cleared all data. System is now empty.");
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"All data cleared. System is now empty.\"}");
        }
    }

    static class AddJunctionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String id = body.get("id");
                if (id != null && !id.trim().isEmpty()) {
                    id = id.trim().toUpperCase();
                    globalController.addJunction(id);
                    log("Web API: Added junction " + id);
                    dbService.saveJunction(id);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Junction " + id + " added.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid junction id\"}");
        }
    }

    static class DeleteJunctionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String id = body.get("id");
                if (id != null && globalController.deleteJunction(id)) {
                    log("Web API: Deleted junction " + id);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Junction " + id + " deleted.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to delete junction\"}");
        }
    }

    static class AddRoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String from = body.get("from");
                String to = body.get("to");
                double time = Double.parseDouble(body.getOrDefault("time", "1.0"));
                boolean twoWay = Boolean.parseBoolean(body.getOrDefault("twoWay", "false"));
                if (from != null && to != null) {
                    globalController.addRoad(from, to, time, twoWay);
                    log(String.format("Web API: Added road %s %s %s (%.1f min)", from, twoWay ? "<->" : "->", to, time));
                    dbService.saveRoad(from, to, time, twoWay);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Road added successfully.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid road parameters\"}");
        }
    }

    static class DeleteRoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String from = body.get("from");
                String to = body.get("to");
                boolean twoWay = Boolean.parseBoolean(body.getOrDefault("twoWay", "false"));
                boolean d1 = globalController.deleteRoad(from, to);
                boolean d2 = false;
                if (twoWay) d2 = globalController.deleteRoad(to, from);
                if (d1 || d2) {
                    log("Web API: Deleted road " + from + " -> " + to);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Road deleted.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to delete road\"}");
        }
    }

    static class UpdateRoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String from = body.get("from");
                String to = body.get("to");
                double time = Double.parseDouble(body.getOrDefault("time", "1.0"));
                boolean closed = Boolean.parseBoolean(body.getOrDefault("closed", "false"));
                if (globalController.updateRoad(from, to, time, closed)) {
                    log("Web API: Updated road " + from + " -> " + to + " (time=" + time + ", closed=" + closed + ")");
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Road updated.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to update road\"}");
        }
    }

    static class AddVehicleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String id = body.get("id");
                String typeStr = body.get("type");
                String start = body.get("start");
                try {
                    VehicleType type = VehicleType.valueOf(typeStr.toUpperCase());
                    if (!globalController.getNetwork().hasJunction(start)) {
                        globalController.addJunction(start);
                        log("Web API: Auto-created starting junction " + start + " for vehicle " + id);
                    }
                    EmergencyVehicle v = new EmergencyVehicle(id, type, start);
                    globalController.registerVehicle(v);
                    log("Web API: Registered vehicle " + id + " (" + type + ") at " + start);
                    dbService.saveVehicle(v);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Vehicle " + id + " registered.\"}");
                    return;
                } catch (Exception ex) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid request method\"}");
        }
    }

    static class DeleteVehicleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String id = body.get("id");
                if (id != null && globalController.deleteVehicle(id)) {
                    log("Web API: Deleted vehicle " + id);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Vehicle " + id + " deleted.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to delete vehicle\"}");
        }
    }

    static class UpdateVehicleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String id = body.get("id");
                String typeStr = body.get("type");
                String junction = body.get("junction");
                String statusStr = body.get("status");

                VehicleType type = (typeStr != null && !typeStr.isEmpty()) ? VehicleType.valueOf(typeStr.toUpperCase()) : null;
                VehicleStatus status = (statusStr != null && !statusStr.isEmpty()) ? VehicleStatus.valueOf(statusStr.toUpperCase()) : null;

                if (globalController.updateVehicle(id, type, junction, status)) {
                    log("Web API: Updated vehicle " + id);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Vehicle " + id + " updated.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to update vehicle\"}");
        }
    }

    static class SubmitRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String vid = body.get("vehicleId");
                String dest = body.get("destination");
                String sevStr = body.get("severity");
                try {
                    Severity sev = parseSeverity(sevStr);
                    if (!globalController.getNetwork().hasJunction(dest)) {
                        globalController.addJunction(dest);
                        log("Web API: Auto-created destination junction " + dest + " for request");
                    }
                    String reqId = globalController.submitRequest(vid, dest, sev);
                    log("Web API: Submitted request " + reqId + " for vehicle " + vid + " to " + dest + " [" + sev + "]");
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"requestId\":\"" + reqId + "\"}");
                    return;
                } catch (Exception ex) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid request method\"}");
        }
    }

    static class EscalateRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String reqId = body.get("requestId");
                String sevStr = body.get("severity");
                try {
                    Severity sev = parseSeverity(sevStr);
                    globalController.escalateRequest(reqId, sev);
                    log("Web API: Escalated request " + reqId + " to " + sev);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Request escalated successfully.\"}");
                    return;
                } catch (Exception ex) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid request method\"}");
        }
    }

    static class DeleteRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String id = body.get("id");
                if (id != null && globalController.deleteRequest(id)) {
                    log("Web API: Deleted routing request " + id);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Request " + id + " deleted.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to delete request\"}");
        }
    }

    static class UpdateRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String id = body.get("id");
                String destination = body.get("destination");
                String sevStr = body.get("severity");
                Severity sev = (sevStr != null && !sevStr.isEmpty()) ? parseSeverity(sevStr) : null;
                if (globalController.updateRequest(id, destination, sev)) {
                    log("Web API: Updated request " + id);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Request " + id + " updated.\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to update request\"}");
        }
    }

    static class QueueEventHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String type = body.get("type");
                String from = body.get("from");
                String to = body.get("to");
                double factor = Double.parseDouble(body.getOrDefault("factor", "1.5"));
                try {
                    if ("CLOSE".equalsIgnoreCase(type)) {
                        globalController.queueTrafficEvent(new ClosureEvent(from, to, false));
                    } else if ("REOPEN".equalsIgnoreCase(type)) {
                        globalController.queueTrafficEvent(new ClosureEvent(from, to, true));
                    } else if ("CONGESTION".equalsIgnoreCase(type)) {
                        globalController.queueTrafficEvent(new CongestionEvent(from, to, factor));
                    } else {
                        throw new IllegalArgumentException("Unknown event type: " + type);
                    }
                    log("Web API: Queued event " + type + " on " + from + " -> " + to);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Traffic event queued.\"}");
                    return;
                } catch (Exception ex) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid request method\"}");
        }
    }

    static class DeleteEventHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String seqStr = body.get("sequence");
                if (seqStr != null) {
                    int seq = Integer.parseInt(seqStr);
                    if (globalController.deleteEvent(seq)) {
                        log("Web API: Deleted traffic event #" + seq);
                        sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Event #" + seq + " deleted.\"}");
                        return;
                    }
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to delete event\"}");
        }
    }

    static class ProcessNextEventHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean processed = globalController.processNextTrafficEvent();
            log("Web API: Processed next traffic event -> " + (processed ? "Success" : "Queue Empty"));
            sendJsonResponse(exchange, 200, "{\"success\":true,\"processed\":" + processed + "}");
        }
    }

    static class ProcessAllEventsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int count = globalController.processAllTrafficEvents();
            log("Web API: Processed all " + count + " traffic events.");
            sendJsonResponse(exchange, 200, "{\"success\":true,\"count\":" + count + "}");
        }
    }

    static class ProcessNextRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean processed = globalController.processNextRequest();
            log("Web API: Processed next routing request -> " + (processed ? "Success" : "Queue Empty"));
            sendJsonResponse(exchange, 200, "{\"success\":true,\"processed\":" + processed + "}");
        }
    }

    static class ProcessAllRequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int count = globalController.processAllRequests();
            log("Web API: Processed all " + count + " routing requests.");
            sendJsonResponse(exchange, 200, "{\"success\":true,\"count\":" + count + "}");
        }
    }

    static class AdvanceVehicleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String vid = body.get("vehicleId");
                String to = body.get("to");
                try {
                    globalController.advanceVehicle(vid, to);
                    log("Web API: Advanced vehicle " + vid + " to " + to);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Vehicle advanced to " + to + "\"}");
                    return;
                } catch (Exception ex) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid request method\"}");
        }
    }

    static class SampleDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            globalController = new TrafficController();
            loadSampleDataset(globalController);
            log("Web API: Reset and loaded standard sample dataset (12 junctions, 4 vehicles, 20 events).");
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Sample dataset loaded successfully.\"}");
        }
    }

    static class DemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            PrintStream origOut = System.out;
            try {
                System.setOut(new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                        sw.write(b);
                        origOut.write(b);
                    }
                    @Override
                    public void write(byte[] b, int off, int len) {
                        sw.write(new String(b, off, len));
                        origOut.write(b, off, len);
                    }
                }));
                runDemonstration();
            } finally {
                System.setOut(origOut);
            }
            String output = sw.toString();
            log("Web API: Scripted demonstration completed.");
            sendJsonResponse(exchange, 200, "{\"success\":true,\"output\":\"" + escapeJson(output) + "\"}");
        }
    }

    static class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"logs\":[");
            List<String> logs = getSystemLogs();
            for (int i = 0; i < logs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(logs.get(i))).append("\"");
            }
            sb.append("]}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    static class SupabaseBackupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String snapshotJson = buildStatusJson(globalController);
            String result = supabaseService.uploadSnapshot(snapshotJson);
            sendJsonResponse(exchange, 200, result);
        }
    }

    static class SupabaseConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = parseJsonSimple(readRequestBody(exchange));
                String url = body.get("url");
                String key = body.get("key");
                String bucket = body.get("bucket");
                supabaseService.updateConfig(url, key, bucket);
                log("Supabase configuration updated from Web UI.");
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Supabase config updated.\"}");
                return;
            }
            String json = supabaseService.getConfigJson();
            sendJsonResponse(exchange, 200, json);
        }
    }

    static class ExportSqlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String sql = generateSqlDump(globalController);
            byte[] bytes = sql.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/sql");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"vegam_traffic_dump.sql\"");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    // ------------------------------------------------------------------
    // JSON Serialization Helpers (Zero External Libraries)
    // ------------------------------------------------------------------

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String buildStatusJson(TrafficController c) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // 1. Junctions
        sb.append("\"junctions\":[");
        int jCount = 0;
        for (String j : c.getNetwork().getJunctions()) {
            if (jCount++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(j)).append("\"");
        }
        sb.append("],");

        // 2. Roads
        sb.append("\"roads\":[");
        int rCount = 0;
        for (String j : c.getNetwork().getJunctions()) {
            for (RoadSegment seg : c.getNetwork().getOutgoing(j)) {
                if (rCount++ > 0) sb.append(",");
                sb.append("{");
                sb.append("\"from\":\"").append(escapeJson(seg.getFrom())).append("\",");
                sb.append("\"to\":\"").append(escapeJson(seg.getTo())).append("\",");
                sb.append("\"baseTravelTime\":").append(seg.getBaseTravelTime()).append(",");
                sb.append("\"currentTravelTime\":").append(seg.getCurrentTravelTime()).append(",");
                sb.append("\"isClosed\":").append(seg.isClosed());
                sb.append("}");
            }
        }
        sb.append("],");

        // 3. Vehicles
        sb.append("\"vehicles\":[");
        int vCount = 0;
        for (EmergencyVehicle v : c.getAllVehicles()) {
            if (vCount++ > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(v.getId())).append("\",");
            sb.append("\"type\":\"").append(v.getType()).append("\",");
            sb.append("\"position\":\"").append(escapeJson(v.getPosition())).append("\",");
            sb.append("\"destination\":").append(v.getDestination() == null ? "null" : "\"" + escapeJson(v.getDestination()) + "\"").append(",");
            sb.append("\"status\":\"").append(v.getStatus()).append("\",");
            Route route = v.getCurrentRoute();
            if (route != null) {
                sb.append("\"route\":{");
                sb.append("\"status\":\"").append(route.getStatus()).append("\",");
                sb.append("\"time\":").append(Double.isInfinite(route.getTotalTravelTime()) ? -1 : route.getTotalTravelTime()).append(",");
                sb.append("\"path\":[");
                List<String> path = route.getPath();
                for (int pi = 0; pi < path.size(); pi++) {
                    if (pi > 0) sb.append(",");
                    sb.append("\"").append(escapeJson(path.get(pi))).append("\"");
                }
                sb.append("]");
                sb.append("}");
            } else {
                sb.append("\"route\":null");
            }
            sb.append("}");
        }
        sb.append("],");

        // 4. Pending Requests (Priority Order)
        sb.append("\"pendingRequests\":[");
        List<RoutingRequest> reqs = c.pendingRequestsSnapshot();
        for (int i = 0; i < reqs.size(); i++) {
            if (i > 0) sb.append(",");
            RoutingRequest req = reqs.get(i);
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(req.getId())).append("\",");
            sb.append("\"vehicleId\":\"").append(escapeJson(req.getVehicleId())).append("\",");
            sb.append("\"destination\":\"").append(escapeJson(req.getDestination())).append("\",");
            sb.append("\"severity\":\"").append(req.getSeverity()).append("\",");
            sb.append("\"arrivalSequence\":").append(req.getArrivalSequence());
            sb.append("}");
        }
        sb.append("],");

        // 5. Pending Events (FIFO)
        sb.append("\"pendingEvents\":[");
        List<TrafficEvent> events = c.getPendingEventsList();
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(",");
            TrafficEvent ev = events.get(i);
            sb.append("{");
            sb.append("\"sequence\":").append(ev.getSequence()).append(",");
            sb.append("\"description\":\"").append(escapeJson(ev.getDescription())).append("\"");
            sb.append("}");
        }
        sb.append("],");

        // 6. Database and Cloud Status
        sb.append("\"database\":{");
        sb.append("\"mysqlConnected\":").append(dbService.isConnected()).append(",");
        sb.append("\"supabaseConfigured\":").append(supabaseService.isConfigured());
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    public static String generateSqlDump(TrafficController c) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Vegam City Smart Traffic System - Live Data Dump\n");
        sb.append("-- Generated at: ").append(new java.util.Date()).append("\n\n");
        sb.append("USE `smart_traffic_db`;\n\n");

        for (String j : c.getNetwork().getJunctions()) {
            sb.append("INSERT INTO `junctions` (`id`) VALUES ('").append(j).append("') ON DUPLICATE KEY UPDATE `id`=`id`;\n");
        }
        sb.append("\n");

        for (String j : c.getNetwork().getJunctions()) {
            for (RoadSegment seg : c.getNetwork().getOutgoing(j)) {
                sb.append("INSERT INTO `roads` (`from_junction`,`to_junction`,`base_travel_time`,`current_travel_time`,`is_closed`) ")
                  .append("VALUES ('").append(seg.getFrom()).append("','").append(seg.getTo()).append("',")
                  .append(seg.getBaseTravelTime()).append(",").append(seg.getCurrentTravelTime()).append(",")
                  .append(seg.isClosed() ? 1 : 0).append(") ")
                  .append("ON DUPLICATE KEY UPDATE `current_travel_time`=").append(seg.getCurrentTravelTime())
                  .append(", `is_closed`=").append(seg.isClosed() ? 1 : 0).append(";\n");
            }
        }
        sb.append("\n");

        for (EmergencyVehicle v : c.getAllVehicles()) {
            sb.append("INSERT INTO `vehicles` (`id`,`vehicle_type`,`current_junction`,`status`) VALUES ('")
              .append(v.getId()).append("','").append(v.getType()).append("','")
              .append(v.getPosition()).append("','").append(v.getStatus()).append("') ")
              .append("ON DUPLICATE KEY UPDATE `current_junction`='").append(v.getPosition())
              .append("', `status`='").append(v.getStatus()).append("';\n");
        }

        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Database Service (MySQL Support via Pure JDBC)
    // ------------------------------------------------------------------
    public static class DatabaseService {
        private boolean connected = false;
        private Connection connection = null;

        public void init() {
            Properties props = loadConfig();
            boolean enabled = Boolean.parseBoolean(props.getProperty("mysql.enabled", "false"));
            if (!enabled) {
                log("[DB] MySQL integration in standalone / in-memory mode (mysql.enabled=false).");
                return;
            }

            String host = props.getProperty("mysql.host", "localhost");
            String port = props.getProperty("mysql.port", "3306");
            String db = props.getProperty("mysql.database", "smart_traffic_db");
            String user = props.getProperty("mysql.user", "root");
            String pass = props.getProperty("mysql.password", "");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

            try {
                // Try to load MySQL Driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, user, pass);
                connected = true;
                log("[DB] Connected to MySQL database successfully: " + db + "@" + host);
            } catch (ClassNotFoundException e) {
                log("[DB] MySQL Connector/J driver not in classpath. Running with high-speed in-memory state.");
            } catch (SQLException e) {
                log("[DB] MySQL connection notice: " + e.getMessage() + ". Running with in-memory state.");
            }
        }

        public boolean isConnected() { return connected; }

        public void saveJunction(String id) {
            if (!connected || connection == null) return;
            try (PreparedStatement ps = connection.prepareStatement("INSERT IGNORE INTO junctions (id) VALUES (?)")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception ex) {
                log("[DB Error] Failed to save junction: " + ex.getMessage());
            }
        }

        public void saveRoad(String from, String to, double time, boolean twoWay) {
            if (!connected || connection == null) return;
            try {
                String sql = "INSERT INTO roads (from_junction, to_junction, base_travel_time, current_travel_time, is_two_way) " +
                             "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE base_travel_time=?, current_travel_time=?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, from);
                    ps.setString(2, to);
                    ps.setDouble(3, time);
                    ps.setDouble(4, time);
                    ps.setBoolean(5, twoWay);
                    ps.setDouble(6, time);
                    ps.setDouble(7, time);
                    ps.executeUpdate();
                }
            } catch (Exception ex) {
                log("[DB Error] Failed to save road: " + ex.getMessage());
            }
        }

        public void saveVehicle(EmergencyVehicle v) {
            if (!connected || connection == null) return;
            try {
                String sql = "INSERT INTO vehicles (id, vehicle_type, current_junction, status) " +
                             "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE current_junction=?, status=?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, v.getId());
                    ps.setString(2, v.getType().name());
                    ps.setString(3, v.getPosition());
                    ps.setString(4, v.getStatus().name());
                    ps.setString(5, v.getPosition());
                    ps.setString(6, v.getStatus().name());
                    ps.executeUpdate();
                }
            } catch (Exception ex) {
                log("[DB Error] Failed to save vehicle: " + ex.getMessage());
            }
        }

        private Properties loadConfig() {
            Properties props = new Properties();
            File cfg = new File("database/db.properties");
            if (!cfg.exists()) cfg = new File("E:/New folder/database/db.properties");
            if (!cfg.exists()) cfg = new File("C:/Users/DIVIT S/.gemini/antigravity/scratch/SmartTrafficManagement/database/db.properties");
            if (cfg.exists()) {
                try (InputStream is = new FileInputStream(cfg)) {
                    props.load(is);
                } catch (Exception ignored) {}
            }
            return props;
        }
    }

    // ------------------------------------------------------------------
    // Supabase Cloud Storage Service (Zero Frameworks, Standard HttpClient)
    // ------------------------------------------------------------------
    public static class SupabaseService {
        private String url = "";
        private String apiKey = "";
        private String bucket = "traffic-backups";
        private boolean configured = false;
        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        public void init() {
            Properties props = loadConfig();
            this.url = props.getProperty("supabase.url", "").trim();
            this.apiKey = props.getProperty("supabase.anon_key", "").trim();
            this.bucket = props.getProperty("supabase.bucket", "traffic-backups").trim();
            checkConfig();
        }

        public void updateConfig(String url, String key, String bucket) {
            if (url != null) this.url = url.trim();
            if (key != null) this.apiKey = key.trim();
            if (bucket != null) this.bucket = bucket.trim();
            checkConfig();
        }

        private void checkConfig() {
            this.configured = url.startsWith("http") && !apiKey.isEmpty() && !apiKey.contains("your-supabase");
            if (configured) {
                log("[Supabase] Cloud Storage ready: " + url + " | Bucket: " + bucket);
            } else {
                log("[Supabase] Cloud Storage not configured yet. Set credentials in database/db.properties or web UI.");
            }
        }

        public boolean isConfigured() { return configured; }

        public String getConfigJson() {
            return "{\"url\":\"" + escapeJson(url) + "\",\"bucket\":\"" + escapeJson(bucket) + "\",\"configured\":" + configured + "}";
        }

        public String uploadSnapshot(String jsonSnapshot) {
            String filename = "snapshot-" + System.currentTimeMillis() + ".json";
            if (!configured) {
                // Save locally to backups directory as fallback and return instructions
                try {
                    File dir = new File("backups");
                    if (!dir.exists()) dir.mkdirs();
                    File snapFile = new File(dir, filename);
                    Files.writeString(snapFile.toPath(), jsonSnapshot);
                    log("[Supabase Notice] Supabase credentials not set yet. Saved local snapshot: " + snapFile.getAbsolutePath());
                    return "{\"success\":true,\"cloud\":false,\"filename\":\"" + filename + "\",\"message\":\"Local snapshot saved to /backups. To push directly to Supabase Cloud Storage, provide your Supabase Project URL and Anon Key in Settings.\"}";
                } catch (Exception ex) {
                    return "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}";
                }
            }

            try {
                String targetUrl = url;
                if (!targetUrl.endsWith("/")) targetUrl += "/";
                targetUrl += "storage/v1/object/" + bucket + "/" + filename;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .header("apikey", apiKey)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("x-upsert", "true")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonSnapshot))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    log("[Supabase] Snapshot successfully uploaded to cloud: " + filename);
                    return "{\"success\":true,\"cloud\":true,\"filename\":\"" + filename + "\",\"url\":\"" + targetUrl + "\"}";
                } else {
                    log("[Supabase Error] Cloud upload failed HTTP " + response.statusCode() + ": " + response.body());
                    return "{\"success\":false,\"cloud\":false,\"statusCode\":" + response.statusCode() + ",\"response\":\"" + escapeJson(response.body()) + "\"}";
                }
            } catch (Exception ex) {
                log("[Supabase Exception] " + ex.getMessage());
                return "{\"success\":false,\"cloud\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}";
            }
        }

        private Properties loadConfig() {
            Properties props = new Properties();
            File cfg = new File("database/db.properties");
            if (!cfg.exists()) cfg = new File("E:/New folder/database/db.properties");
            if (!cfg.exists()) cfg = new File("C:/Users/DIVIT S/.gemini/antigravity/scratch/SmartTrafficManagement/database/db.properties");
            if (cfg.exists()) {
                try (InputStream is = new FileInputStream(cfg)) {
                    props.load(is);
                } catch (Exception ignored) {}
            }
            return props;
        }
    }

    // ------------------------------------------------------------------
    // Console CLI Loop (Preserves Original Interactive Terminal Usage)
    // ------------------------------------------------------------------

    private static void runConsoleLoop(TrafficController controller) {
        Scanner sc = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1":  addJunctionMenu(controller, sc); break;
                    case "2":  addRoadMenu(controller, sc); break;
                    case "3":  addVehicleMenu(controller, sc); break;
                    case "4":  submitRequestMenu(controller, sc); break;
                    case "5":  queueEventMenu(controller, sc); break;
                    case "6":  controller.processNextTrafficEvent(); break;
                    case "7":  log("Processed " + controller.processAllTrafficEvents() + " traffic event(s)."); break;
                    case "8":  controller.processNextRequest(); break;
                    case "9":  log("Processed " + controller.processAllRequests() + " request(s)."); break;
                    case "10": advanceVehicleMenu(controller, sc); break;
                    case "11": escalateMenu(controller, sc); break;
                    case "12": controller.getNetwork().printNetwork(); break;
                    case "13": viewVehicles(controller); break;
                    case "14": viewQueues(controller); break;
                    case "15": loadSampleDataset(controller); break;
                    case "16": runDemonstration(); break;
                    case "0":  running = false; break;
                    default:   System.out.println("Invalid option, please choose again.");
                }
            } catch (Exception ex) {
                log("  [!] Error: " + ex.getMessage());
            }
        }
        System.out.println("Console session ended. Web server remains active.");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("-------------------------------------------------------------");
        System.out.println(" 1  Add junction");
        System.out.println(" 2  Add road (one-way / two-way)");
        System.out.println(" 3  Register emergency vehicle");
        System.out.println(" 4  Submit routing request");
        System.out.println(" 5  Queue traffic event (closure / reopen / congestion)");
        System.out.println(" 6  Process NEXT traffic event (FIFO)");
        System.out.println(" 7  Process ALL pending traffic events");
        System.out.println(" 8  Process NEXT routing request (priority order)");
        System.out.println(" 9  Process ALL pending routing requests");
        System.out.println("10  Advance a vehicle to an adjacent junction");
        System.out.println("11  Escalate a pending request's severity");
        System.out.println("12  View road network status");
        System.out.println("13  View vehicle status");
        System.out.println("14  View pending queues");
        System.out.println("15  Load sample dataset (12 junctions, 4 vehicles, 20 events)");
        System.out.println("16  Run scripted demonstration (normal + 4 edge cases)");
        System.out.println(" 0  Exit Console");
        System.out.print("Choose an option: ");
    }

    private static void addJunctionMenu(TrafficController c, Scanner sc) {
        System.out.print("  Junction id: ");
        String id = sc.nextLine().trim();
        c.addJunction(id);
        log("  Added junction " + id);
    }

    private static void addRoadMenu(TrafficController c, Scanner sc) {
        System.out.print("  From junction: ");
        String from = sc.nextLine().trim();
        System.out.print("  To junction: ");
        String to = sc.nextLine().trim();
        System.out.print("  Travel time (minutes): ");
        double time = Double.parseDouble(sc.nextLine().trim());
        System.out.print("  Two-way? (y/n): ");
        boolean twoWay = sc.nextLine().trim().equalsIgnoreCase("y");
        c.addRoad(from, to, time, twoWay);
        log("  Road added: " + from + (twoWay ? " <-> " : " -> ") + to + " (" + time + " min)");
    }

    private static void addVehicleMenu(TrafficController c, Scanner sc) {
        System.out.print("  Vehicle id: ");
        String id = sc.nextLine().trim();
        System.out.print("  Type (AMBULANCE / FIRE_ENGINE / RESCUE): ");
        VehicleType type = VehicleType.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("  Starting junction: ");
        String start = sc.nextLine().trim();
        if (!c.getNetwork().hasJunction(start)) {
            System.out.println("  [!] Unknown junction; add it first.");
            return;
        }
        c.registerVehicle(new EmergencyVehicle(id, type, start));
        log("  Registered " + id + " (" + type + ") at " + start);
    }

    private static void submitRequestMenu(TrafficController c, Scanner sc) {
        System.out.print("  Vehicle id: ");
        String vid = sc.nextLine().trim();
        if (c.getVehicle(vid) == null) {
            System.out.println("  [!] Unknown vehicle.");
            return;
        }
        System.out.print("  Destination junction: ");
        String dest = sc.nextLine().trim();
        System.out.print("  Severity (CRITICAL / HIGH / NORMAL): ");
        Severity sev = parseSeverity(sc.nextLine().trim());
        c.submitRequest(vid, dest, sev);
    }

    private static void queueEventMenu(TrafficController c, Scanner sc) {
        System.out.print("  Event type (CLOSE / REOPEN / CONGESTION): ");
        String type = sc.nextLine().trim().toUpperCase();
        System.out.print("  From junction: ");
        String from = sc.nextLine().trim();
        System.out.print("  To junction: ");
        String to = sc.nextLine().trim();
        switch (type) {
            case "CLOSE":
                c.queueTrafficEvent(new ClosureEvent(from, to, false));
                break;
            case "REOPEN":
                c.queueTrafficEvent(new ClosureEvent(from, to, true));
                break;
            case "CONGESTION":
                System.out.print("  Slowdown factor (e.g. 1.5 = 50% slower): ");
                double factor = Double.parseDouble(sc.nextLine().trim());
                c.queueTrafficEvent(new CongestionEvent(from, to, factor));
                break;
            default:
                System.out.println("  [!] Unknown event type.");
        }
    }

    private static void advanceVehicleMenu(TrafficController c, Scanner sc) {
        System.out.print("  Vehicle id: ");
        String vid = sc.nextLine().trim();
        System.out.print("  Move to junction: ");
        String to = sc.nextLine().trim();
        c.advanceVehicle(vid, to);
    }

    private static void escalateMenu(TrafficController c, Scanner sc) {
        System.out.println("  Pending: " + c.pendingRequestsSnapshot());
        System.out.print("  Request id: ");
        String rid = sc.nextLine().trim();
        System.out.print("  New severity (CRITICAL / HIGH / NORMAL): ");
        Severity sev = parseSeverity(sc.nextLine().trim());
        c.escalateRequest(rid, sev);
    }

    private static void viewVehicles(TrafficController c) {
        if (c.getAllVehicles().isEmpty()) {
            System.out.println("  No vehicles registered.");
            return;
        }
        for (EmergencyVehicle v : c.getAllVehicles()) System.out.println("  " + v);
    }

    private static void viewQueues(TrafficController c) {
        System.out.println("  Pending traffic events (FIFO order): " + c.pendingEventsSummary());
        System.out.println("  Pending requests (priority order): " + c.pendingRequestsSnapshot());
    }

    public static Severity parseSeverity(String s) {
        s = s.trim().toUpperCase();
        switch (s) {
            case "C": case "CRITICAL": return Severity.CRITICAL;
            case "H": case "HIGH": return Severity.HIGH;
            case "N": case "NORMAL": return Severity.NORMAL;
            default: throw new IllegalArgumentException("Unknown severity: " + s);
        }
    }

    // ------------------------------------------------------------------
    // Sample dataset: 12 junctions, 4 vehicles, 20 events total
    // ------------------------------------------------------------------

    public static void buildDemoNetwork(TrafficController c) {
        String[] junctions = {"J1","J2","J3","J4","J5","J6","J7","J8","J9","J10","J11","J12"};
        for (String j : junctions) c.addJunction(j);

        // Two-way backbone (paired directed edges)
        c.addRoad("J1","J2", 5, true);
        c.addRoad("J2","J3", 4, true);
        c.addRoad("J3","J4", 3, true);
        c.addRoad("J4","J5", 6, true);
        c.addRoad("J5","J6", 2, true);
        c.addRoad("J1","J7", 8, true);
        c.addRoad("J7","J8", 3, true);
        c.addRoad("J8","J9", 4, true);
        c.addRoad("J9","J10", 5, true);
        c.addRoad("J10","J6", 7, true);
        c.addRoad("J2","J8", 10, true);
        c.addRoad("J3","J9", 9, true);

        // One-way spur: J5 -> J11 only (no reverse edge) - a genuine dead end
        c.addRoad("J5","J11", 3, false);

        // Single one-way lifeline into J12 (its only entrance)
        c.addRoad("J6","J12", 5, false);

        c.registerVehicle(new EmergencyVehicle("AMB1",    VehicleType.AMBULANCE,   "J1"));
        c.registerVehicle(new EmergencyVehicle("FIRE1",   VehicleType.FIRE_ENGINE, "J7"));
        c.registerVehicle(new EmergencyVehicle("RESCUE1", VehicleType.RESCUE,      "J4"));
        c.registerVehicle(new EmergencyVehicle("AMB2",    VehicleType.AMBULANCE,   "J11"));
    }

    public static void loadSampleDataset(TrafficController c) {
        buildDemoNetwork(c);

        // 10 traffic events, queued in arrival order
        c.queueTrafficEvent(new CongestionEvent("J1","J2", 1.5));
        c.queueTrafficEvent(new CongestionEvent("J2","J3", 2.0));
        c.queueTrafficEvent(new ClosureEvent("J7","J8", false));
        c.queueTrafficEvent(new ClosureEvent("J7","J8", true));
        c.queueTrafficEvent(new CongestionEvent("J4","J5", 1.3));
        c.queueTrafficEvent(new ClosureEvent("J9","J10", false));
        c.queueTrafficEvent(new CongestionEvent("J3","J9", 1.8));
        c.queueTrafficEvent(new ClosureEvent("J2","J8", false));
        c.queueTrafficEvent(new CongestionEvent("J8","J9", 1.4));
        c.queueTrafficEvent(new ClosureEvent("J9","J10", true));

        // 10 routing requests across the 4 vehicles with varied severities
        c.submitRequest("AMB1",    "J6",  Severity.CRITICAL);
        c.submitRequest("FIRE1",   "J10", Severity.HIGH);
        c.submitRequest("RESCUE1", "J9",  Severity.NORMAL);
        c.submitRequest("AMB2",    "J5",  Severity.HIGH);
        c.submitRequest("AMB1",    "J12", Severity.NORMAL);
        c.submitRequest("FIRE1",   "J1",  Severity.CRITICAL);
        c.submitRequest("RESCUE1", "J6",  Severity.HIGH);
        c.submitRequest("AMB2",    "J1",  Severity.NORMAL);
        c.submitRequest("AMB1",    "J9",  Severity.HIGH);
        c.submitRequest("FIRE1",   "J12", Severity.NORMAL);

        log("Sample dataset loaded: 12 junctions, 4 vehicles, 10 traffic events + 10 requests (20 total).");
    }

    public static void runDemonstration() {
        log("\n===================== SCRIPTED DEMONSTRATION =====================");
        TrafficController c = new TrafficController();
        buildDemoNetwork(c);

        log("\n--- Scenario 1: Normal operation ---");
        c.submitRequest("AMB1", "J6", Severity.CRITICAL);
        c.processNextRequest();

        log("\n--- Scenario 2: Road closed WHILE a vehicle is already en route ---");
        EmergencyVehicle amb1 = c.getVehicle("AMB1");
        List<String> originalPath = amb1.getCurrentRoute().getPath();
        if (originalPath.size() >= 3) {
            String nextHop = originalPath.get(1);
            String followingHop = originalPath.get(2);
            c.advanceVehicle("AMB1", nextHop);
            log("  AMB1 has physically passed " + nextHop + " (its new last-confirmed junction).");
            c.queueTrafficEvent(new ClosureEvent(nextHop, followingHop, false));
            c.processNextTrafficEvent(); // closure is applied AND affected en-route vehicles are auto-recomputed
            log("  Note: the recomputed route starts from " + amb1.getPosition()
                    + " - junction already passed was correctly never reused.");
        }

        log("\n--- Scenario 3: Equal-severity requests (FIFO), then escalation reorders queue ---");
        c.submitRequest("FIRE1", "J1", Severity.HIGH);
        String r2 = c.submitRequest("RESCUE1", "J1", Severity.HIGH);
        log("  Queue before escalation: " + c.pendingRequestsSnapshot());
        c.escalateRequest(r2, Severity.CRITICAL);
        log("  Queue after escalating RESCUE1 to CRITICAL: " + c.pendingRequestsSnapshot());
        c.processNextRequest(); // RESCUE1 now goes first (CRITICAL beats HIGH)
        c.processNextRequest(); // FIRE1 goes second

        log("\n--- Scenario 4: Unreachable destination ---");
        log("  J6->J12 is J12's only entrance; closing it isolates J12.");
        c.queueTrafficEvent(new ClosureEvent("J6", "J12", false));
        c.processNextTrafficEvent();
        c.submitRequest("RESCUE1", "J12", Severity.NORMAL);
        c.processNextRequest();

        log("\n--- Scenario 5: Reversed one-way access is correctly rejected ---");
        log("  J5->J11 exists, but J11->J5 does not. AMB2 sits at J11.");
        c.submitRequest("AMB2", "J5", Severity.HIGH);
        c.processNextRequest();

        log("\n===================== END OF DEMONSTRATION =====================");
    }
}

// =============================================================================
// Enums
// =============================================================================

enum Severity {
    CRITICAL(3), HIGH(2), NORMAL(1);
    final int rank;
    Severity(int rank) { this.rank = rank; }
}

enum VehicleType { AMBULANCE, FIRE_ENGINE, RESCUE }

enum VehicleStatus { IDLE, EN_ROUTE, ARRIVED, UNREACHABLE }

enum RouteStatus { VALID, UNREACHABLE }

// =============================================================================
// RoadSegment - a directed, weighted edge of the network
// =============================================================================

class RoadSegment {
    private final String from;
    private final String to;
    private double baseTravelTime;
    private double currentTravelTime;
    private boolean closed;

    RoadSegment(String from, String to, double baseTravelTime) {
        this.from = from;
        this.to = to;
        this.baseTravelTime = baseTravelTime;
        this.currentTravelTime = baseTravelTime;
        this.closed = false;
    }

    String getFrom() { return from; }
    String getTo() { return to; }
    double getBaseTravelTime() { return baseTravelTime; }
    double getCurrentTravelTime() { return currentTravelTime; }
    boolean isClosed() { return closed; }
    String key() { return from + "->" + to; }

    void close() { this.closed = true; }
    void reopen() { this.closed = false; this.currentTravelTime = baseTravelTime; }
    void applyCongestion(double factor) { this.currentTravelTime = baseTravelTime * Math.max(1.0, factor); }

    void setBaseTravelTime(double time) {
        this.baseTravelTime = time;
        if (!this.closed) this.currentTravelTime = time;
    }

    double effectiveTravelTime() { return closed ? Double.POSITIVE_INFINITY : currentTravelTime; }

    @Override
    public String toString() {
        return String.format("%s -> %s [%.1f min%s]", from, to, currentTravelTime, closed ? ", CLOSED" : "");
    }
}

// =============================================================================
// TrafficEvent hierarchy (inheritance + polymorphism)
// =============================================================================

abstract class TrafficEvent {
    private static int counter = 0;
    private final int sequence;      // arrival order, used for strict FIFO processing
    private final String description;

    TrafficEvent(String description) {
        this.sequence = ++counter;
        this.description = description;
    }

    int getSequence() { return sequence; }
    String getDescription() { return description; }

    abstract void applyToNetwork(RoadNetwork network);
}

class ClosureEvent extends TrafficEvent {
    private final String from, to;
    private final boolean reopening;

    ClosureEvent(String from, String to, boolean reopening) {
        super((reopening ? "REOPEN " : "CLOSE ") + from + "->" + to);
        this.from = from;
        this.to = to;
        this.reopening = reopening;
    }

    @Override
    void applyToNetwork(RoadNetwork network) {
        if (reopening) network.reopenRoad(from, to);
        else network.closeRoad(from, to);
    }
}

class CongestionEvent extends TrafficEvent {
    private final String from, to;
    private final double factor;

    CongestionEvent(String from, String to, double factor) {
        super("CONGESTION " + from + "->" + to + " x" + factor);
        if (Double.isNaN(factor) || Double.isInfinite(factor) || factor < 1.0) {
            throw new IllegalArgumentException("Slowdown factor must be a finite number >= 1.0 (e.g. 1.5 = 50% slower)");
        }
        this.from = from;
        this.to = to;
        this.factor = factor;
    }

    @Override
    void applyToNetwork(RoadNetwork network) {
        network.applyCongestion(from, to, factor);
    }
}

// =============================================================================
// Route - result of a routing computation
// =============================================================================

class Route {
    private final List<String> path;
    private final double totalTravelTime;
    private final RouteStatus status;
    private final String requestedFrom, requestedTo;

    private Route(List<String> path, double totalTravelTime, RouteStatus status, String from, String to) {
        this.path = path;
        this.totalTravelTime = totalTravelTime;
        this.status = status;
        this.requestedFrom = from;
        this.requestedTo = to;
    }

    static Route valid(List<String> path, double time) {
        return new Route(path, time, RouteStatus.VALID, path.get(0), path.get(path.size() - 1));
    }

    static Route unreachable(String from, String to) {
        return new Route(Collections.emptyList(), Double.POSITIVE_INFINITY, RouteStatus.UNREACHABLE, from, to);
    }

    List<String> getPath() { return path; }
    double getTotalTravelTime() { return totalTravelTime; }
    RouteStatus getStatus() { return status; }

    @Override
    public String toString() {
        if (status == RouteStatus.UNREACHABLE) {
            return String.format("UNREACHABLE (no usable path from %s to %s)", requestedFrom, requestedTo);
        }
        return String.format("%s (%.1f min)", String.join(" -> ", path), totalTravelTime);
    }
}

// =============================================================================
// RoadNetwork - the directed weighted graph, plus a hand-written Dijkstra
// =============================================================================

class RoadNetwork {
    private final Set<String> junctions = new LinkedHashSet<>();
    private final Map<String, List<RoadSegment>> adjacency = new HashMap<>();
    private final Map<String, RoadSegment> segments = new HashMap<>();

    void addJunction(String id) {
        junctions.add(id);
        adjacency.putIfAbsent(id, new ArrayList<>());
    }

    void clear() {
        junctions.clear();
        adjacency.clear();
        segments.clear();
    }

    boolean removeJunction(String id) {
        if (!junctions.contains(id)) return false;
        junctions.remove(id);
        List<RoadSegment> outgoing = adjacency.remove(id);
        if (outgoing != null) {
            for (RoadSegment s : outgoing) {
                segments.remove(s.key());
            }
        }
        for (String j : new ArrayList<>(junctions)) {
            List<RoadSegment> list = adjacency.get(j);
            if (list != null) {
                list.removeIf(s -> {
                    if (s.getTo().equals(id)) {
                        segments.remove(s.key());
                        return true;
                    }
                    return false;
                });
            }
        }
        return true;
    }

    boolean removeRoad(String from, String to) {
        String key = from + "->" + to;
        RoadSegment seg = segments.remove(key);
        if (seg == null) return false;
        List<RoadSegment> list = adjacency.get(from);
        if (list != null) {
            list.removeIf(s -> s.getTo().equals(to));
        }
        return true;
    }

    boolean updateRoad(String from, String to, double newTime, boolean closed) {
        RoadSegment seg = getSegment(from, to);
        if (seg == null) return false;
        seg.setBaseTravelTime(newTime);
        if (closed) seg.close(); else seg.reopen();
        return true;
    }

    boolean hasJunction(String id) { return junctions.contains(id); }
    Set<String> getJunctions() { return junctions; }

    boolean addRoad(String from, String to, double travelTime, boolean twoWay) {
        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty())
            throw new IllegalArgumentException("Road endpoints must be non-empty junction ids.");
        if (Double.isNaN(travelTime) || Double.isInfinite(travelTime) || travelTime <= 0)
            throw new IllegalArgumentException("Travel time must be a positive number (got " + travelTime + ").");
        addJunction(from);
        addJunction(to);
        boolean addedNew = addDirectedRoad(from, to, travelTime);
        if (twoWay) addedNew &= addDirectedRoad(to, from, travelTime);
        return addedNew;
    }

    private boolean addDirectedRoad(String from, String to, double time) {
        String key = from + "->" + to;
        RoadSegment existing = segments.get(key);
        if (existing != null) {
            existing.setBaseTravelTime(time);
            return false;
        }
        RoadSegment seg = new RoadSegment(from, to, time);
        adjacency.get(from).add(seg);
        segments.put(key, seg);
        return true;
    }

    RoadSegment getSegment(String from, String to) { return segments.get(from + "->" + to); }
    List<RoadSegment> getOutgoing(String junction) { return adjacency.getOrDefault(junction, Collections.emptyList()); }

    void closeRoad(String from, String to) {
        RoadSegment seg = getSegment(from, to);
        if (seg == null) { SmartTrafficSystem.log("  [!] No road " + from + "->" + to + " to close."); return; }
        seg.close();
        SmartTrafficSystem.log("  [x] Road closed: " + from + " -> " + to);
    }

    void reopenRoad(String from, String to) {
        RoadSegment seg = getSegment(from, to);
        if (seg == null) { SmartTrafficSystem.log("  [!] No road " + from + "->" + to + " to reopen."); return; }
        seg.reopen();
        SmartTrafficSystem.log("  [+] Road reopened: " + from + " -> " + to);
    }

    void applyCongestion(String from, String to, double factor) {
        RoadSegment seg = getSegment(from, to);
        if (seg == null) { SmartTrafficSystem.log("  [!] No road " + from + "->" + to + " for congestion update."); return; }
        seg.applyCongestion(factor);
        SmartTrafficSystem.log(String.format("  [~] Congestion applied: %s -> %s now %.1f min", from, to, seg.getCurrentTravelTime()));
    }

    void applyTrafficEvent(TrafficEvent event) { event.applyToNetwork(this); }

    void printNetwork() {
        System.out.println("Junctions: " + junctions);
        for (String j : junctions) for (RoadSegment seg : getOutgoing(j)) System.out.println("  " + seg);
    }

    Route shortestPath(String source, String destination) {
        if (!hasJunction(source) || !hasJunction(destination)) return Route.unreachable(source, destination);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> settled = new HashSet<>();
        for (String j : junctions) dist.put(j, Double.POSITIVE_INFINITY);
        dist.put(source, 0.0);

        PriorityQueue<HeapEntry> minHeap = new PriorityQueue<>(Comparator.comparingDouble(e -> e.distance));
        minHeap.offer(new HeapEntry(source, 0.0));

        while (!minHeap.isEmpty()) {
            HeapEntry entry = minHeap.poll();
            String u = entry.node;
            if (entry.distance > dist.get(u)) continue;
            if (settled.contains(u)) continue;
            settled.add(u);
            if (u.equals(destination)) break;

            for (RoadSegment seg : getOutgoing(u)) {
                double w = seg.effectiveTravelTime();
                if (w == Double.POSITIVE_INFINITY) continue;
                String v = seg.getTo();
                if (settled.contains(v)) continue;
                double candidate = dist.get(u) + w;
                if (candidate < dist.get(v)) {
                    dist.put(v, candidate);
                    prev.put(v, u);
                    minHeap.offer(new HeapEntry(v, candidate));
                }
            }
        }

        if (dist.get(destination) == Double.POSITIVE_INFINITY) return Route.unreachable(source, destination);

        LinkedList<String> path = new LinkedList<>();
        String cur = destination;
        while (cur != null) {
            path.addFirst(cur);
            if (cur.equals(source)) break;
            cur = prev.get(cur);
        }
        if (!path.peekFirst().equals(source)) return Route.unreachable(source, destination);
        return Route.valid(path, dist.get(destination));
    }

    private static final class HeapEntry {
        final String node;
        final double distance;
        HeapEntry(String node, double distance) { this.node = node; this.distance = distance; }
    }
}

// =============================================================================
// RoutePlanner - abstraction over routing
// =============================================================================

interface RoutePlanner {
    Route planRoute(RoadNetwork network, String from, String to);
}

class DijkstraRoutePlanner implements RoutePlanner {
    @Override
    public Route planRoute(RoadNetwork network, String from, String to) {
        return network.shortestPath(from, to);
    }
}

// =============================================================================
// EmergencyVehicle
// =============================================================================

class EmergencyVehicle {
    private final String id;
    private VehicleType type;
    private String lastConfirmedJunction;
    private String destination;
    private VehicleStatus status;
    private Route currentRoute;

    EmergencyVehicle(String id, VehicleType type, String startJunction) {
        this.id = id;
        this.type = type;
        this.lastConfirmedJunction = startJunction;
        this.status = VehicleStatus.IDLE;
    }

    String getId() { return id; }
    VehicleType getType() { return type; }
    String getPosition() { return lastConfirmedJunction; }
    String getDestination() { return destination; }
    VehicleStatus getStatus() { return status; }
    Route getCurrentRoute() { return currentRoute; }

    void setType(VehicleType type) { this.type = type; }
    void setPosition(String junction) { this.lastConfirmedJunction = junction; }
    void setStatus(VehicleStatus status) { this.status = status; }
    void setDestination(String destination) { this.destination = destination; }

    void updatePosition(RoadNetwork network, String newJunction) {
        if (!network.hasJunction(newJunction)) throw new IllegalArgumentException("Unknown junction: " + newJunction);
        RoadSegment seg = network.getSegment(lastConfirmedJunction, newJunction);
        if (seg == null) throw new IllegalStateException(newJunction + " is not directly reachable from " + lastConfirmedJunction);
        if (seg.isClosed()) throw new IllegalStateException("Road " + lastConfirmedJunction + " -> " + newJunction + " is closed; vehicle cannot use it.");
        this.lastConfirmedJunction = newJunction;
        if (destination != null && newJunction.equals(destination)) status = VehicleStatus.ARRIVED;
    }

    void assignRoute(Route route) {
        this.currentRoute = route;
        if (route.getStatus() == RouteStatus.UNREACHABLE) {
            this.status = VehicleStatus.UNREACHABLE;
        } else if (destination != null && destination.equals(lastConfirmedJunction)) {
            this.status = VehicleStatus.ARRIVED;
        } else {
            this.status = VehicleStatus.EN_ROUTE;
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %-11s at %-4s -> dest %-4s | status=%-11s | route=%s",
                id, type, lastConfirmedJunction, destination == null ? "-" : destination, status,
                currentRoute == null ? "none" : currentRoute);
    }
}

// =============================================================================
// RoutingRequest
// =============================================================================

class RoutingRequest {
    private final String id;
    private final String vehicleId;
    private String destination;
    private Severity severity;
    private final int arrivalSequence;
    private static int counter = 0;

    RoutingRequest(String id, String vehicleId, String destination, Severity severity) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.destination = destination;
        this.severity = severity;
        this.arrivalSequence = ++counter;
    }

    String getId() { return id; }
    String getVehicleId() { return vehicleId; }
    String getDestination() { return destination; }
    Severity getSeverity() { return severity; }
    int getArrivalSequence() { return arrivalSequence; }
    void setSeverity(Severity severity) { this.severity = severity; }
    void setDestination(String destination) { this.destination = destination; }

    static final Comparator<RoutingRequest> PRIORITY_ORDER =
            Comparator.comparingInt((RoutingRequest r) -> r.severity.rank).reversed()
                    .thenComparingInt(r -> r.arrivalSequence);

    @Override
    public String toString() {
        return String.format("%s[veh=%s,dest=%s,%s,#%d]", id, vehicleId, destination, severity, arrivalSequence);
    }
}

// =============================================================================
// TrafficController - orchestrator
// =============================================================================

class TrafficController {
    private final RoadNetwork network = new RoadNetwork();
    private final ArrayDeque<TrafficEvent> eventQueue = new ArrayDeque<>();
    private final PriorityQueue<RoutingRequest> requestQueue = new PriorityQueue<>(RoutingRequest.PRIORITY_ORDER);
    private final Map<String, EmergencyVehicle> vehicles = new LinkedHashMap<>();
    private final Map<String, RoutingRequest> requestsById = new HashMap<>();
    private final RoutePlanner planner = new DijkstraRoutePlanner();
    private int requestCounter = 0;

    RoadNetwork getNetwork() { return network; }
    void addJunction(String id) { network.addJunction(id); }
    void addRoad(String from, String to, double time, boolean twoWay) { network.addRoad(from, to, time, twoWay); }

    void registerVehicle(EmergencyVehicle v) {
        if (v == null) throw new IllegalArgumentException("Vehicle must not be null.");
        if (!network.hasJunction(v.getPosition()))
            throw new IllegalArgumentException("Unknown starting junction: " + v.getPosition());
        vehicles.put(v.getId(), v);
    }

    EmergencyVehicle getVehicle(String id) { return vehicles.get(id); }
    Collection<EmergencyVehicle> getAllVehicles() { return vehicles.values(); }

    String submitRequest(String vehicleId, String destination, Severity severity) {
        EmergencyVehicle v = vehicles.get(vehicleId);
        if (v == null) throw new IllegalArgumentException("Unknown vehicle: " + vehicleId);
        if (!network.hasJunction(destination))
            throw new IllegalArgumentException("Unknown destination junction: " + destination);
        String id = "REQ" + (++requestCounter);
        RoutingRequest req = new RoutingRequest(id, vehicleId, destination, severity);
        requestQueue.offer(req);
        requestsById.put(id, req);
        SmartTrafficSystem.log("  Queued " + req);
        return id;
    }

    void escalateRequest(String requestId, Severity newSeverity) {
        RoutingRequest req = requestsById.get(requestId);
        if (req == null) { SmartTrafficSystem.log("  [!] No such pending request: " + requestId); return; }
        boolean removed = requestQueue.remove(req);
        req.setSeverity(newSeverity);
        if (removed) requestQueue.offer(req);
        SmartTrafficSystem.log("  Updated " + req + " (re-ranked in queue)");
    }

    void queueTrafficEvent(TrafficEvent event) {
        eventQueue.offer(event);
        SmartTrafficSystem.log("  Queued traffic event #" + event.getSequence() + ": " + event.getDescription());
    }

    boolean processNextTrafficEvent() {
        TrafficEvent event = eventQueue.poll();
        if (event == null) { SmartTrafficSystem.log("  No pending traffic events."); return false; }
        SmartTrafficSystem.log("Processing traffic event #" + event.getSequence() + ": " + event.getDescription());
        network.applyTrafficEvent(event);

        for (EmergencyVehicle v : vehicles.values()) {
            if ((v.getStatus() == VehicleStatus.EN_ROUTE || v.getStatus() == VehicleStatus.UNREACHABLE)
                    && v.getDestination() != null) {
                Route recomputed = planner.planRoute(network, v.getPosition(), v.getDestination());
                v.assignRoute(recomputed);
                SmartTrafficSystem.log("  Recomputed route for " + v.getId() + " from " + v.getPosition() + ": " + recomputed);
            }
        }
        return true;
    }

    private void applyPendingTrafficEvents() {
        if (eventQueue.isEmpty()) return;
        SmartTrafficSystem.log("  [i] Applying " + eventQueue.size()
                + " pending traffic event(s) first so network is up to date.");
        while (!eventQueue.isEmpty()) processNextTrafficEvent();
    }

    int processAllTrafficEvents() {
        int count = 0;
        while (processNextTrafficEvent()) count++;
        return count;
    }

    boolean processNextRequest() {
        if (requestQueue.peek() == null) { SmartTrafficSystem.log("  No pending routing requests."); return false; }
        applyPendingTrafficEvents();
        RoutingRequest req = requestQueue.poll();
        requestsById.remove(req.getId());
        EmergencyVehicle v = vehicles.get(req.getVehicleId());

        SmartTrafficSystem.log("Processing " + req);
        v.setDestination(req.getDestination());
        Route route = planner.planRoute(network, v.getPosition(), req.getDestination());
        v.assignRoute(route);
        if (route.getStatus() == RouteStatus.UNREACHABLE) {
            SmartTrafficSystem.log("  RESULT: destination unreachable from " + v.getPosition() + " to " + req.getDestination());
        } else {
            SmartTrafficSystem.log("  RESULT: route assigned -> " + route);
        }
        return true;
    }

    int processAllRequests() {
        int count = 0;
        while (processNextRequest()) count++;
        return count;
    }

    void advanceVehicle(String vehicleId, String toJunction) {
        EmergencyVehicle v = vehicles.get(vehicleId);
        if (v == null) throw new IllegalArgumentException("Unknown vehicle: " + vehicleId);
        applyPendingTrafficEvents();
        v.updatePosition(network, toJunction);
        SmartTrafficSystem.log("  " + vehicleId + " advanced to " + toJunction + " (status=" + v.getStatus() + ")");
        if (v.getDestination() != null
                && (v.getStatus() == VehicleStatus.EN_ROUTE || v.getStatus() == VehicleStatus.UNREACHABLE)) {
            Route recomputed = planner.planRoute(network, v.getPosition(), v.getDestination());
            v.assignRoute(recomputed);
            SmartTrafficSystem.log("  Route recomputed from " + v.getPosition() + ": " + recomputed);
        }
    }

    void clearAll() {
        network.clear();
        eventQueue.clear();
        requestQueue.clear();
        vehicles.clear();
        requestsById.clear();
        requestCounter = 0;
    }

    boolean deleteJunction(String id) {
        boolean removed = network.removeJunction(id);
        if (removed) {
            for (EmergencyVehicle v : vehicles.values()) {
                if (v.getPosition().equals(id)) {
                    v.setStatus(VehicleStatus.UNREACHABLE);
                }
            }
            replanAllActiveVehicles();
        }
        return removed;
    }

    boolean deleteRoad(String from, String to) {
        boolean removed = network.removeRoad(from, to);
        if (removed) {
            replanAllActiveVehicles();
        }
        return removed;
    }

    boolean updateRoad(String from, String to, double time, boolean closed) {
        boolean ok = network.updateRoad(from, to, time, closed);
        if (ok) {
            replanAllActiveVehicles();
        }
        return ok;
    }

    boolean deleteVehicle(String id) {
        EmergencyVehicle removed = vehicles.remove(id);
        if (removed != null) {
            List<RoutingRequest> toRemove = new ArrayList<>();
            for (RoutingRequest r : requestQueue) {
                if (r.getVehicleId().equals(id)) toRemove.add(r);
            }
            for (RoutingRequest r : toRemove) {
                requestQueue.remove(r);
                requestsById.remove(r.getId());
            }
            return true;
        }
        return false;
    }

    boolean updateVehicle(String id, VehicleType type, String junction, VehicleStatus status) {
        EmergencyVehicle v = vehicles.get(id);
        if (v == null) return false;
        if (type != null) v.setType(type);
        if (junction != null && network.hasJunction(junction)) v.setPosition(junction);
        if (status != null) v.setStatus(status);
        if (v.getDestination() != null && (v.getStatus() == VehicleStatus.EN_ROUTE || v.getStatus() == VehicleStatus.UNREACHABLE)) {
            Route recomputed = planner.planRoute(network, v.getPosition(), v.getDestination());
            v.assignRoute(recomputed);
        }
        return true;
    }

    boolean deleteRequest(String id) {
        RoutingRequest req = requestsById.remove(id);
        if (req != null) {
            requestQueue.remove(req);
            return true;
        }
        return false;
    }

    boolean updateRequest(String id, String newDest, Severity newSev) {
        RoutingRequest req = requestsById.get(id);
        if (req == null) return false;
        requestQueue.remove(req);
        if (newDest != null && network.hasJunction(newDest)) req.setDestination(newDest);
        if (newSev != null) req.setSeverity(newSev);
        requestQueue.offer(req);
        return true;
    }

    boolean deleteEvent(int sequence) {
        return eventQueue.removeIf(e -> e.getSequence() == sequence);
    }

    List<TrafficEvent> getPendingEventsList() {
        return new ArrayList<>(eventQueue);
    }

    private void replanAllActiveVehicles() {
        for (EmergencyVehicle v : vehicles.values()) {
            if ((v.getStatus() == VehicleStatus.EN_ROUTE || v.getStatus() == VehicleStatus.UNREACHABLE)
                    && v.getDestination() != null) {
                Route recomputed = planner.planRoute(network, v.getPosition(), v.getDestination());
                v.assignRoute(recomputed);
            }
        }
    }

    List<RoutingRequest> pendingRequestsSnapshot() {
        List<RoutingRequest> list = new ArrayList<>(requestQueue);
        list.sort(RoutingRequest.PRIORITY_ORDER);
        return list;
    }

    List<String> pendingEventsSummary() {
        List<String> list = new ArrayList<>();
        for (TrafficEvent e : eventQueue) list.add("#" + e.getSequence() + " " + e.getDescription());
        return list;
    }
}

