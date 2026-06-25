package Graphwar;

import java.util.*;

import GraphServer.Constants;

public class GraphFormulaGenerator {
    private static final double VERTICAL_MAX_COEFF = 999;
    private static final double VERTICAL_MIN_EPS = 0.001;
    private static final double DEFAULT_CLEARANCE = 0.1;
    private static final double X_MIN = -25.0, X_MAX = 25.0;

    // The game is weird as fuck, it claims that it's 15 to -15 on Y axis
    // but in reality it's 15 to -14.22 because of the way it renders the graph plane
    private static final double Y_MIN = 15 - 450.0 * 50.0 / 770.0, Y_MAX = 15.0;
    private static final int COLS = 400, ROWS = 240;
    
    private static final int[][] NEIGHBORS = {{1, 0}, {1, 1}, {1, -1}, {0, 1}, {0, -1}};
    
    public static class Point {
        public double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }
        @Override public String toString() { return String.format("(%.5f, %.5f)", x, y); }
    }
    
    public static class Circle {
        public double x, y, radius;
        public Circle(double x, double y, double radius) { this.x = x; this.y = y; this.radius = radius; }
    }

    public static double[] fieldToGame(double fieldX, double fieldY) {
        double gameX = -25 + fieldX * 50 / Constants.PLANE_LENGTH;
        double gameY =  15 - fieldY * 50 / Constants.PLANE_LENGTH;
        return new double[]{ gameX, gameY };
    }
    
    private static double verticalEps(double yFrom, double yTo) {
        double dy = Math.abs(yTo - yFrom);
        if (dy < 1e-9) return VERTICAL_MIN_EPS;
        return Math.max(VERTICAL_MIN_EPS, dy / (2 * VERTICAL_MAX_COEFF));
    }
    
    private static boolean segmentIntersectsCircle(Point p1, Point p2, Circle circle, double margin) {
        double cx = circle.x, cy = circle.y, r = circle.radius + margin;
        double x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;
        double dx = x2 - x1, dy = y2 - y1;
        
        if (Math.abs(dx) < 1e-12 && Math.abs(dy) < 1e-12) {
            return Math.hypot(x1 - cx, y1 - cy) <= r;
        }
        
        double fx = x1 - cx, fy = y1 - cy;
        double a = dx * dx + dy * dy;
        double b = 2 * (fx * dx + fy * dy);
        double c = fx * fx + fy * fy - r * r;
        double disc = b * b - 4 * a * c;
        
        if (disc < 0) return false;
        
        double sqrtDisc = Math.sqrt(disc);
        double[] tValues = {(-b - sqrtDisc) / (2 * a), (-b + sqrtDisc) / (2 * a)};
        for (double t : tValues) {
            if (t >= -1e-9 && t <= 1 + 1e-9) return true;
        }
        return false;
    }
    
    private static boolean segmentHitsAny(Point p1, Point p2, List<Circle> obstacles, double margin) {
        for (Circle o : obstacles) {
            if (segmentIntersectsCircle(p1, p2, o, margin)) return true;
        }
        return false;
    }
    
    private static int[] gameToCell(double x, double y) {
        int gi = (int) Math.round((x - X_MIN) / (X_MAX - X_MIN) * (COLS - 1));
        int gj = (int) Math.round((Y_MAX - y) / (Y_MAX - Y_MIN) * (ROWS - 1));
        gi = Math.max(0, Math.min(COLS - 1, gi));
        gj = Math.max(0, Math.min(ROWS - 1, gj));
        return new int[]{gi, gj};
    }
    
    private static Point cellToGame(int gi, int gj) {
        double x = X_MIN + (double) gi / (COLS - 1) * (X_MAX - X_MIN);
        double y = Y_MAX - (double) gj / (ROWS - 1) * (Y_MAX - Y_MIN);
        return new Point(x, y);
    }
    
    private static boolean cellBlocked(int gi, int gj, List<Circle> obstacles, double margin) {
        Point cell = cellToGame(gi, gj);
        for (Circle o : obstacles) {
            if (Math.hypot(cell.x - o.x, cell.y - o.y) <= o.radius + margin) return true;
        }
        return false;
    }
    
    private static boolean[][] buildBlockedGrid(List<Circle> obstacles, double margin) {
        boolean[][] blocked = new boolean[COLS][ROWS];
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                blocked[i][j] = cellBlocked(i, j, obstacles, margin);
            }
        }
        return blocked;
    }
    
    private static int[] nearestFree(int gi, int gj, boolean[][] blocked, boolean preferRight) {
        if (!blocked[gi][gj]) return new int[]{gi, gj};
        
        int[] best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int di = -10; di <= 10; di++) {
            for (int dj = -10; dj <= 10; dj++) {
                int ni = gi + di, nj = gj + dj;
                if (ni < 0 || ni >= COLS || nj < 0 || nj >= ROWS) continue;
                if (blocked[ni][nj]) continue;
                if (preferRight && ni < gi) continue;
                double score = di * di + dj * dj;
                if (score < bestScore) {
                    bestScore = score;
                    best = new int[]{ni, nj};
                }
            }
        }
        return best;
    }
    
    private static double heuristic(int gi, int gj, int goalI, int goalJ, List<Circle> obstacles, double margin) {
        Point cell = cellToGame(gi, gj);
        Point goal = cellToGame(goalI, goalJ);
        double dist = Math.hypot(cell.x - goal.x, cell.y - goal.y);
        
        double penalty = 0;
        for (Circle o : obstacles) {
            double d = Math.hypot(cell.x - o.x, cell.y - o.y);
            double pad = o.radius + margin + 0.4;
            if (d < pad) penalty += (pad - d) * 1.8;
        }
        return dist + penalty;
    }
    
    private static List<Point> astarGame(Point start, Point goal, List<Circle> obstacles, double clearance) {
        double margin = clearance;
        boolean[][] blocked = buildBlockedGrid(obstacles, margin);
        
        int[] startCell = gameToCell(start.x, start.y);
        int[] goalCell = gameToCell(goal.x, goal.y);
        
        int[] startFree = nearestFree(startCell[0], startCell[1], blocked, false);
        int[] goalFree = nearestFree(goalCell[0], goalCell[1], blocked, false);
        if (startFree == null || goalFree == null) return null;
        
        int startI = startFree[0], startJ = startFree[1];
        int goalI = goalFree[0], goalJ = goalFree[1];
        
        if (startI > goalI) return null;
        
        PriorityQueue<double[]> openHeap = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        Map<Long, Double> gScore = new HashMap<>();
        Map<Long, int[]> cameFrom = new HashMap<>();
        
        long startKey = (long) startI << 32 | startJ;
        long goalKey = (long) goalI << 32 | goalJ;
        
        openHeap.add(new double[]{0, startKey});
        gScore.put(startKey, 0.0);
        
        while (!openHeap.isEmpty()) {
            double[] current = openHeap.poll();
            long currentKey = (long) current[1];
            
            if (currentKey == goalKey) break;
            
            int ci = (int) (currentKey >> 32);
            int cj = (int) (currentKey & 0xFFFFFFFF);
            
            for (int[] neighbor : NEIGHBORS) {
                int ni = ci + neighbor[0], nj = cj + neighbor[1];
                if (ni < 0 || ni >= COLS || nj < 0 || nj >= ROWS) continue;
                if (blocked[ni][nj]) continue;
                
                long neighborKey = (long) ni << 32 | nj;
                double stepCost = (neighbor[0] == 1 && neighbor[1] != 0) ? Math.sqrt(2) : 1.0;
                double tentative = gScore.get(currentKey) + stepCost;
                
                if (tentative >= gScore.getOrDefault(neighborKey, Double.POSITIVE_INFINITY)) continue;
                
                cameFrom.put(neighborKey, new int[]{ci, cj});
                gScore.put(neighborKey, tentative);
                double f = tentative + heuristic(ni, nj, goalI, goalJ, obstacles, margin);
                openHeap.add(new double[]{f, neighborKey});
            }
        }
        
        if (!cameFrom.containsKey(goalKey) && goalKey != startKey) return null;
        
        List<int[]> cells = new ArrayList<>();
        long current = goalKey;
        while (current != startKey) {
            cells.add(new int[]{(int) (current >> 32), (int) (current & 0xFFFFFFFF)});
            current = (long) cameFrom.get(current)[0] << 32 | cameFrom.get(current)[1];
        }
        cells.add(new int[]{startI, startJ});
        Collections.reverse(cells);
        
        List<Point> path = new ArrayList<>();
        for (int[] cell : cells) {
            path.add(cellToGame(cell[0], cell[1]));
        }

        if (!path.isEmpty()) {
            path.set(0, start);
            path.set(path.size() - 1, goal);
        }

        return path;
    }
    
    private static List<Point> simplifyPath(List<Point> points, List<Circle> obstacles, double margin) {
        if (points.size() <= 2) return points;
        
        List<Point> out = new ArrayList<>();
        out.add(points.get(0));
        int i = 0;
        
        while (i < points.size() - 1) {
            int j = points.size() - 1;
            while (j > i + 1) {
                if (!segmentHitsAny(points.get(i), points.get(j), obstacles, margin)) break;
                j--;
            }
            out.add(points.get(j));
            i = j;
        }
        return out;
    }
    
    private static String directLine(Point p1, Point p2) {
        double x1 = p1.x, y1 = p1.y;
        double x2 = p2.x, y2 = p2.y;
        double dx = x2 - x1;
        
        if (Math.abs(dx) < 1e-12) {
            dx = verticalEps(y1, y2);
            x2 = x1 + dx;
        }
        
        double dist = -((y1 - y2) / 2) / dx;
        return String.format("%.6f*(abs(x-%.6f)-abs(x-%.6f))", dist, x1, x2).replace("--", "+");
    }

    private static List<Point> mergeNearbyPoints(List<Point> points, double threshold) {
        if (points.size() <= 2) return points;
        
        List<Point> result = new ArrayList<>();
        result.add(points.get(0));
        
        for (int i = 1; i < points.size(); i++) {
            Point last = result.get(result.size() - 1);
            Point current = points.get(i);
            
            if (Math.hypot(last.x - current.x, last.y - current.y) > threshold) {
                result.add(current);
            }
            // If points are within threshold, skip the current one (keep the last)
        }
        
        return result;
    }
    
    private static String waypointsToFormula(List<Point> waypoints) {
        if (waypoints.size() < 2) return "";
        
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            parts.add(directLine(waypoints.get(i), waypoints.get(i + 1)));
        }

        String formula = String.join("+", parts).replace("+-", "-");
        // Trim from newlines and spaces
        formula = formula.replaceAll("\\s+", " ").trim();

        return formula;
    }
    
    /**
     * Main function to generate a formula that hits as many waypoints as possible while avoiding obstacles.
     * 
     * @param waypoints List of target points (x, y) to hit
     * @param obstacles List of obstacles with (x, y, radius)
     * @param clearance Safety margin around obstacles (default 0.1)
     * @return Mathematical formula string, or null if no valid path found
     */
    public static String generateFormula(List<Point> waypoints, List<Circle> obstacles, double clearance) {
        if (waypoints == null || waypoints.isEmpty()) return null;
        if (obstacles == null) obstacles = new ArrayList<>();
        if (clearance <= 0) clearance = DEFAULT_CLEARANCE;
        
        // Sort waypoints by x coordinate (must move rightward)
        List<Point> sorted = new ArrayList<>(waypoints);
        sorted.sort(Comparator.comparingDouble(p -> p.x));
        
        // Build path using A* between consecutive waypoints
        List<Point> path = new ArrayList<>();
        path.add(sorted.get(0));
        
        for (int i = 1; i < sorted.size(); i++) {
            Point current = path.get(path.size() - 1);
            Point target = sorted.get(i);
            
            if (target.x < current.x - 1e-6) {
                // Can't move left, skip this waypoint
                continue;
            }
            
            List<Point> segmentPath = astarGame(current, target, obstacles, clearance);
            if (segmentPath == null) {
                // No path found, try direct line (may hit obstacles)
                if (!segmentHitsAny(current, target, obstacles, clearance)) {
                    path.add(target);
                }
                continue;
            }
            
            List<Point> simplified = simplifyPath(segmentPath, obstacles, clearance);
            for (Point pt : simplified) {
                if (path.isEmpty() || Math.hypot(pt.x - path.get(path.size() - 1).x, pt.y - path.get(path.size() - 1).y) > 1e-6) {
                    path.add(pt);
                }
            }

            // Ensure the exact target waypoint is included at the end
            Point lastPoint = path.get(path.size() - 1);
            if (Math.hypot(lastPoint.x - target.x, lastPoint.y - target.y) > 1e-6) {
                // Check if we can reach the exact target without hitting obstacles
                if (!segmentHitsAny(lastPoint, target, obstacles, clearance)) {
                    path.add(target);
                }
            }
        }
        
        if (path.size() < 2) return null;

        //List<Point> mergedPath = mergeNearbyPoints(path, 0.25);
        
        return waypointsToFormula(path);
    }
    
    /**
     * Convenience method with default clearance.
     */
    public static String generateFormula(List<Point> waypoints, List<Circle> obstacles) {
        return generateFormula(waypoints, obstacles, DEFAULT_CLEARANCE);
    }
    
    // Example usage
    public static void main(String[] args) {
        // Example: Generate formula to hit waypoints while avoiding obstacles
        List<Point> waypoints = new ArrayList<>();
        waypoints.add(new Point(-20, 0));
        waypoints.add(new Point(-10, 5));
        waypoints.add(new Point(0, -5));
        waypoints.add(new Point(10, 3));
        waypoints.add(new Point(20, 0));
        
        List<Circle> obstacles = new ArrayList<>();
        obstacles.add(new Circle(-5, 0, 2));
        obstacles.add(new Circle(5, 2, 1.5));
        
        String formula = generateFormula(waypoints, obstacles);
        System.out.println("Generated Formula: " + formula);
    }
}
