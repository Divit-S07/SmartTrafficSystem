import java.util.*;
public class SmartTrafficSystem {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TrafficController controller = new TrafficController();

        System.out.println("=============================================================");
        System.out.println(" VEGAM CITY - SMART TRAFFIC MANAGEMENT & EMERGENCY ROUTING   ");
        System.out.println("=============================================================");

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
                    case "7":  System.out.println("Processed " + controller.processAllTrafficEvents() + " traffic event(s)."); break;
                    case "8":  controller.processNextRequest(); break;
                    case "9":  System.out.println("Processed " + controller.processAllRequests() + " request(s)."); break;
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
                String msg = ex.getMessage();
                System.out.println("  [!] Error: " + (msg != null ? msg : ex));
            }
        }
        System.out.println("Session ended.");
        sc.close();
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
        System.out.println(" 0  Exit");
        System.out.print("Choose an option: ");
    }

    private static void addJunctionMenu(TrafficController c, Scanner sc) {
        System.out.print("  Junction id: ");
        String id = sc.nextLine().trim();
        c.addJunction(id);
        System.out.println("  Added junction " + id);
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
        System.out.println("  Road added: " + from + (twoWay ? " <-> " : " -> ") + to + " (" + time + " min)");
    }

    private static void addVehicleMenu(TrafficController c, Scanner sc) {
        System.out.print("  Vehicle id: ");
        String id = sc.nextLine().trim();
        System.out.print("  Type (AMBULANCE / FIRE_ENGINE / RESCUE): ");
        VehicleType type;
        try {
            type = VehicleType.valueOf(sc.nextLine().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            System.out.println("  [!] Unknown vehicle type; use AMBULANCE, FIRE_ENGINE or RESCUE.");
            return;
        }
        System.out.print("  Starting junction: ");
        String start = sc.nextLine().trim();
        if (!c.getNetwork().hasJunction(start)) {
            System.out.println("  [!] Unknown junction; add it first.");
            return;
        }
        c.registerVehicle(new EmergencyVehicle(id, type, start));
        System.out.println("  Registered " + id + " (" + type + ") at " + start);
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
        if (!c.getNetwork().hasJunction(dest)) {
            System.out.println("  [!] Unknown destination junction: " + dest);
            return;
        }
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
        if (!c.getNetwork().hasJunction(from) || !c.getNetwork().hasJunction(to)) {
            System.out.println("  [!] Unknown junction (" + from + " / " + to + "); add it first.");
            return;
        }
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

    private static Severity parseSeverity(String s) {
        s = s.trim().toUpperCase();
        switch (s) {
            case "C": case "CRITICAL": return Severity.CRITICAL;
            case "H": case "HIGH": return Severity.HIGH;
            case "N": case "NORMAL": return Severity.NORMAL;
            default: throw new IllegalArgumentException("Unknown severity: " + s);
        }
    }

    // ------------------------------------------------------------------
    // Sample dataset: 12 junctions (within the 10-15 range), 4 vehicles,
    // 10 traffic events + 10 routing requests = 20 events, as required.
    // ------------------------------------------------------------------

    private static void buildDemoNetwork(TrafficController c) {
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

    private static void loadSampleDataset(TrafficController c) {
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

        System.out.println("Sample dataset loaded: 12 junctions, 4 vehicles, 10 traffic events + 10 requests (20 total).");
        System.out.println("Use options 6/7 to apply traffic events and 8/9 to process requests.");
    }

    // ------------------------------------------------------------------
    // Scripted demonstration - normal operation + the four required
    // edge-case tests, each on its own freshly-built network.
    // ------------------------------------------------------------------

    private static void runDemonstration() {
        System.out.println("\n===================== SCRIPTED DEMONSTRATION =====================");
        TrafficController c = new TrafficController();
        buildDemoNetwork(c);

        System.out.println("\n--- Scenario 1: Normal operation ---");
        c.submitRequest("AMB1", "J6", Severity.CRITICAL);
        c.processNextRequest();

        System.out.println("\n--- Scenario 2: Road closed WHILE a vehicle is already en route ---");
        EmergencyVehicle amb1 = c.getVehicle("AMB1");
        List<String> originalPath = amb1.getCurrentRoute().getPath();
        if (originalPath.size() >= 3) {
            String nextHop = originalPath.get(1);
            String followingHop = originalPath.get(2);
            c.advanceVehicle("AMB1", nextHop);
            System.out.println("  AMB1 has physically passed " + nextHop + " (its new last-confirmed junction).");
            c.queueTrafficEvent(new ClosureEvent(nextHop, followingHop, false));
            c.processNextTrafficEvent(); // closure is applied AND affected en-route vehicles are auto-recomputed
            System.out.println("  Note: the recomputed route above starts from " + amb1.getPosition()
                    + " - the junction already passed (J1) was correctly never reused as a start point.");
        } else {
            System.out.println("  AMB1 has no remaining hops; scenario skipped.");
        }

        System.out.println("\n--- Scenario 3: Equal-severity requests (FIFO), then a severity escalation reorders the queue ---");
        c.submitRequest("FIRE1", "J1", Severity.HIGH);       // arrives first
        String r2 = c.submitRequest("RESCUE1", "J1", Severity.HIGH); // arrives second, same severity
        System.out.println("  Queue before escalation (priority order): " + c.pendingRequestsSnapshot());
        c.escalateRequest(r2, Severity.CRITICAL); // remove-and-reinsert on ranking-field change
        System.out.println("  Queue after escalating RESCUE1's request to CRITICAL: " + c.pendingRequestsSnapshot());
        c.processNextRequest(); // RESCUE1 now goes first (CRITICAL beats HIGH)
        c.processNextRequest(); // FIRE1 goes second

        System.out.println("\n--- Scenario 4: Unreachable destination ---");
        System.out.println("  J6->J12 is J12's only entrance; closing it isolates J12.");
        c.queueTrafficEvent(new ClosureEvent("J6", "J12", false));
        c.processNextTrafficEvent();
        c.submitRequest("RESCUE1", "J12", Severity.NORMAL);
        c.processNextRequest();

        System.out.println("\n--- Scenario 5: Reversed one-way access is correctly rejected ---");
        System.out.println("  J5->J11 exists, but J11->J5 does not. AMB2 sits at J11.");
        c.submitRequest("AMB2", "J5", Severity.HIGH);
        c.processNextRequest();

        System.out.println("\n===================== END OF DEMONSTRATION =====================");
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
    double getCurrentTravelTime() { return currentTravelTime; }
    boolean isClosed() { return closed; }
    String key() { return from + "->" + to; }

    // Encapsulation: mutators are package-private so only RoadNetwork can call them.
    void close() { this.closed = true; }
    void reopen() { this.closed = false; this.currentTravelTime = baseTravelTime; }
    void applyCongestion(double factor) { this.currentTravelTime = baseTravelTime * Math.max(1.0, factor); }

    /** Re-adding an existing road redefines its base travel time without
     *  discarding its current closure state or losing the shared segment object. */
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

    /** Polymorphic hook: each event type knows how to mutate the network. */
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

    /**
     * Keeps exactly one RoadSegment object per directed edge, shared by the key
     * index and the adjacency list, so closeRoad()/reopenRoad() always affect the
     * same object that Dijkstra traverses. Re-adding a road updates it in place.
     * @return true if this created a new edge, false if an existing edge was updated
     */
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

    // Encapsulation: RoadSegment's closed flag can only change through here.
    void closeRoad(String from, String to) {
        RoadSegment seg = getSegment(from, to);
        if (seg == null) { System.out.println("  [!] No road " + from + "->" + to + " to close."); return; }
        seg.close();
        System.out.println("  [x] Road closed: " + from + " -> " + to);
    }

    void reopenRoad(String from, String to) {
        RoadSegment seg = getSegment(from, to);
        if (seg == null) { System.out.println("  [!] No road " + from + "->" + to + " to reopen."); return; }
        seg.reopen();
        System.out.println("  [+] Road reopened: " + from + " -> " + to);
    }

    void applyCongestion(String from, String to, double factor) {
        RoadSegment seg = getSegment(from, to);
        if (seg == null) { System.out.println("  [!] No road " + from + "->" + to + " for congestion update."); return; }
        seg.applyCongestion(factor);
        System.out.printf("  [~] Congestion applied: %s -> %s now %.1f min%n", from, to, seg.getCurrentTravelTime());
    }

    /** Single entry point used by the controller; the mutation itself is polymorphic per event type. */
    void applyTrafficEvent(TrafficEvent event) { event.applyToNetwork(this); }

    void printNetwork() {
        System.out.println("Junctions: " + junctions);
        for (String j : junctions) for (RoadSegment seg : getOutgoing(j)) System.out.println("  " + seg);
    }

    /**
     * Hand-written Dijkstra's algorithm (positive weights only). A PriorityQueue
     * is used purely as the min-heap over immutable (junction, distance) entries;
     * the relaxation logic is implemented here, not delegated to a library
     * shortest-path routine. Closed roads are simply treated as having infinite
     * weight, so they are never selected. Because heap keys never change after
     * insertion, stale entries are detected on removal instead of mutating keys
     * inside the heap (which would corrupt the heap ordering).
     */
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
            if (entry.distance > dist.get(u)) continue; // stale heap entry (lazy deletion)
            if (settled.contains(u)) continue;          // duplicate entry for a settled junction
            settled.add(u);
            if (u.equals(destination)) break;

            for (RoadSegment seg : getOutgoing(u)) {
                double w = seg.effectiveTravelTime();
                if (w == Double.POSITIVE_INFINITY) continue; // closed road: unusable
                String v = seg.getTo();
                if (settled.contains(v)) continue;
                double candidate = dist.get(u) + w;
                if (candidate < dist.get(v)) {
                    dist.put(v, candidate);
                    prev.put(v, u);
                    minHeap.offer(new HeapEntry(v, candidate)); // decrease-key via insert + lazy stale check
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

    /** Immutable min-heap record: the key is fixed at insertion time. */
    private static final class HeapEntry {
        final String node;
        final double distance;
        HeapEntry(String node, double distance) { this.node = node; this.distance = distance; }
    }
}

// =============================================================================
// RoutePlanner - abstraction over routing (composition target of the controller)
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
    private final VehicleType type;
    private String lastConfirmedJunction; // position already reached; never re-used as a "past" point
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

    void setDestination(String destination) { this.destination = destination; }

    /** Encapsulation: a vehicle may only advance to a junction genuinely adjacent
     *  to it, and never through a road that is currently closed. */
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
            this.status = VehicleStatus.ARRIVED; // route computed from (and to) the junction already reached
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
    private final String destination;
    private Severity severity;             // mutable: escalation changes the ranking field
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

    /** Deterministic order: higher severity first, then earlier arrival first. */
    static final Comparator<RoutingRequest> PRIORITY_ORDER =
            Comparator.comparingInt((RoutingRequest r) -> r.severity.rank).reversed()
                    .thenComparingInt(r -> r.arrivalSequence);

    @Override
    public String toString() {
        return String.format("%s[veh=%s,dest=%s,%s,#%d]", id, vehicleId, destination, severity, arrivalSequence);
    }
}

// =============================================================================
// TrafficController - orchestrator (composition of network + queues + planner)
// =============================================================================

class TrafficController {
    private final RoadNetwork network = new RoadNetwork();
    private final ArrayDeque<TrafficEvent> eventQueue = new ArrayDeque<>();      // strict FIFO
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
        System.out.println("  Queued " + req);
        return id;
    }

    /** Priority queues can't update a key in place, so a ranking-field change
     *  requires removing the record and reinserting it, as required. */
    void escalateRequest(String requestId, Severity newSeverity) {
        RoutingRequest req = requestsById.get(requestId);
        if (req == null) { System.out.println("  [!] No such pending request: " + requestId); return; }
        boolean removed = requestQueue.remove(req);
        req.setSeverity(newSeverity);
        if (removed) requestQueue.offer(req);
        System.out.println("  Updated " + req + " (re-ranked in queue)");
    }

    void queueTrafficEvent(TrafficEvent event) {
        eventQueue.offer(event);
        System.out.println("  Queued traffic event #" + event.getSequence() + ": " + event.getDescription());
    }

    boolean processNextTrafficEvent() {
        TrafficEvent event = eventQueue.poll();
        if (event == null) { System.out.println("  No pending traffic events."); return false; }
        System.out.println("Processing traffic event #" + event.getSequence() + ": " + event.getDescription());
        network.applyTrafficEvent(event); // polymorphic dispatch to ClosureEvent/CongestionEvent

        // The network just changed: any vehicle currently en route (including one
        // previously marked UNREACHABLE, in case a road reopened) must be
        // recomputed from its last CONFIRMED junction, never from its original start.
        for (EmergencyVehicle v : vehicles.values()) {
            if ((v.getStatus() == VehicleStatus.EN_ROUTE || v.getStatus() == VehicleStatus.UNREACHABLE)
                    && v.getDestination() != null) {
                Route recomputed = planner.planRoute(network, v.getPosition(), v.getDestination());
                v.assignRoute(recomputed);
                System.out.println("  Recomputed route for " + v.getId() + " from " + v.getPosition() + ": " + recomputed);
            }
        }
        return true;
    }

    /** Every route calculation must run on an up-to-date network: apply all
     *  pending traffic events (in arrival order) before computing any route. */
    private void applyPendingTrafficEvents() {
        if (eventQueue.isEmpty()) return;
        System.out.println("  [i] Applying " + eventQueue.size()
                + " pending traffic event(s) first so the network is up to date.");
        while (!eventQueue.isEmpty()) processNextTrafficEvent();
    }

    int processAllTrafficEvents() {
        int count = 0;
        while (processNextTrafficEvent()) count++;
        return count;
    }

    boolean processNextRequest() {
        if (requestQueue.peek() == null) { System.out.println("  No pending routing requests."); return false; }
        applyPendingTrafficEvents(); // network must be updated before the next route calculation
        RoutingRequest req = requestQueue.poll();
        requestsById.remove(req.getId());
        EmergencyVehicle v = vehicles.get(req.getVehicleId());

        System.out.println("Processing " + req);
        v.setDestination(req.getDestination()); // destination is confirmed when the request is processed
        Route route = planner.planRoute(network, v.getPosition(), req.getDestination()); // from current position, not original start
        v.assignRoute(route);
        if (route.getStatus() == RouteStatus.UNREACHABLE) {
            System.out.println("  RESULT: destination unreachable from " + v.getPosition() + " to " + req.getDestination());
        } else {
            System.out.println("  RESULT: route assigned -> " + route);
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
        applyPendingTrafficEvents(); // movement must be checked against an up-to-date network
        v.updatePosition(network, toJunction);
        System.out.println("  " + vehicleId + " advanced to " + toJunction + " (status=" + v.getStatus() + ")");
        // Re-plan from the new last-confirmed junction so the stored route never
        // starts at a location the vehicle has already passed.
        if (v.getDestination() != null
                && (v.getStatus() == VehicleStatus.EN_ROUTE || v.getStatus() == VehicleStatus.UNREACHABLE)) {
            Route recomputed = planner.planRoute(network, v.getPosition(), v.getDestination());
            v.assignRoute(recomputed);
            System.out.println("  Route recomputed from " + v.getPosition() + ": " + recomputed);
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