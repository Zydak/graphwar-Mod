package Graphwar;

import javax.swing.*;

import GraphServer.Constants;
import Graphwar.GraphFormulaGenerator.Circle;
import Graphwar.GraphFormulaGenerator.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModMenu
{
    private static JFrame m_Frame;
    private static GameMapPanel m_MapPanel;
    private static JTextField functionInput;
    private static GameData gameData;
    private static JCheckBox intersectAlliesCheckbox;
    private static JCheckBox intersectEnemiesCheckbox;

    public static void open()
    {
        m_Frame = new JFrame("Mod Menu");

        m_MapPanel = new GameMapPanel();
        m_Frame.add(m_MapPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        // Function input panel
        JPanel functionPanel = new JPanel();
        functionInput = new JTextField(30);

        functionInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateFunction();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateFunction();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateFunction();
            }

            private void updateFunction() {
                String functionStr = ModMenu.additionalParsing(functionInput.getText());
                m_MapPanel.setFunctionToDraw(functionStr);
            }
        });

        JButton sendButton = new JButton("SEND");
        sendButton.addActionListener(e -> {
            if (gameData != null)
            {
                gameData.sendFunction(ModMenu.additionalParsing(functionInput.getText()));
            }
        });

        JButton clearButton = new JButton("CLEAR");
        clearButton.addActionListener(e -> {
            functionInput.setText("");
            m_MapPanel.setFunctionToDraw("");
        });

        functionPanel.add(new JLabel("Function: "));
        functionPanel.add(functionInput);
        functionPanel.add(sendButton);
        functionPanel.add(clearButton);

        // Generate function panel
        JPanel generatePanel = new JPanel();
        generatePanel.setBorder(BorderFactory.createTitledBorder("Generate Function"));

        intersectAlliesCheckbox = new JCheckBox("Intersect Allies");
        intersectEnemiesCheckbox = new JCheckBox("Intersect Enemies");
        intersectEnemiesCheckbox.setSelected(true);

        JCheckBox drawingModeCheckbox = new JCheckBox("Drawing Mode");
        drawingModeCheckbox.addActionListener(e -> {
            m_MapPanel.setDrawingMode(drawingModeCheckbox.isSelected());
        });

        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> generateFunction());

        JButton clearPointsButton = new JButton("Clear Points");
        clearPointsButton.addActionListener(e -> m_MapPanel.clearDrawnPoints());

        generatePanel.add(intersectAlliesCheckbox);
        generatePanel.add(intersectEnemiesCheckbox);
        generatePanel.add(drawingModeCheckbox);
        generatePanel.add(generateButton);
        generatePanel.add(clearPointsButton);

        bottomPanel.add(functionPanel, BorderLayout.NORTH);
        bottomPanel.add(generatePanel, BorderLayout.SOUTH);
        m_Frame.add(bottomPanel, BorderLayout.SOUTH);

        m_Frame.setSize(800, 600);
        m_Frame.setLocationRelativeTo(null);
        m_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        m_Frame.setVisible(true);
    }

    static public String additionalParsing(String input)
    {
        // Make step(h, x0) evaluate to h/(1+exp(-100*(x-(x0))))
        String parsedInput = input.replaceAll("step\\(([^,]+),([^\\)]+)\\)", "($1/(1+exp(-100*(x-($2))))");

        // Make spsin(h, f, x0) evaluate to hsin(fx)/(1+exp(-10*(x-(x0))))
        parsedInput = parsedInput.replaceAll("spsin\\(([^,]+),([^,]+),([^\\)]+)\\)", "($1*sin($2*x)/(1+exp(-10*(x-($3))))");

        return parsedInput;
    }

    static public void ReopenWindow()
    {
        if (m_Frame != null)
        {
            m_Frame.setVisible(true);
        }
    }

    private static void generateFunction()
    {
        if (gameData == null || m_MapPanel == null)
        {
            return;
        }

        List<Point> waypoints = new ArrayList<Point>();

        // If drawing mode and there are drawn points, use those
        if (m_MapPanel.isDrawingMode() && !m_MapPanel.getDrawnPoints().isEmpty())
        {
            waypoints.addAll(m_MapPanel.getDrawnPoints());
        }
        else
        {
            // Otherwise use the checkbox-based generation
            List<Player> players = gameData.getPlayers();
            if (players == null)
            {
                return;
            }

            // Find selected soldier team
            Soldier selectedSoldier = GameMapPanel.selectedSoldier;
            int selectedSoldierTeam = -1;
            for (Player player : players)
            {
                for (Soldier soldier : player.getSoldiers())
                {
                    if (soldier == selectedSoldier)
                    {
                        selectedSoldierTeam = player.getTeam();
                        break;
                    }
                }
                if (selectedSoldierTeam != -1)
                {
                    break;
                }
            }

            // Get current turn soldier position
            if (selectedSoldier == null || !selectedSoldier.isAlive())
            {
                return;
            }

            int selectedSoldierX = selectedSoldier.x;
            int selectedSoldierY = selectedSoldier.y;
            
            // Convert selected soldier position to game coordinates
            double[] shootingSoldierGamePos = GraphFormulaGenerator.fieldToGame(selectedSoldierX, selectedSoldierY);

            // Collect waypoints
            waypoints.add(new Point(shootingSoldierGamePos[0], shootingSoldierGamePos[1]));

            // Add ally positions if selected
            if (intersectAlliesCheckbox.isSelected())
            {
                List<Point> allyPositions = getSoldierPositions(selectedSoldierTeam);
                waypoints.addAll(allyPositions);
            }

            // Add enemy positions if selected
            if (intersectEnemiesCheckbox.isSelected())
            {
                int enemyTeam = selectedSoldierTeam == Constants.TEAM1 ? Constants.TEAM2 : Constants.TEAM1;

                List<Point> enemyPositions = getSoldierPositions(enemyTeam);
                waypoints.addAll(enemyPositions);
            }
        }

        // Sort waypoints by x
        waypoints.sort((a, b) -> Double.compare(a.x, b.x));

        // Get obstacles
        List<Circle> circles = GameMapPanel.obstacles;

        // Generate formula
        String formula = GraphFormulaGenerator.generateFormula(waypoints, circles);

        // Set in textbox
        if (!formula.isEmpty())
        {
            functionInput.setText(formula);
        }
    }

    private static List<Point> getSoldierPositions(int team)
    {
        return gameData.GetSoldierPositionCartesianPlane(team);
    }

    public static void setGameData(GameData data)
    {
        gameData = data;
    }

    public static void updateMap(List<Player> players, List<GraphFormulaGenerator.Circle> obstacles, boolean reversed)
    {
        // Reverse all positions of obstacles if the terrain is reversed

        if (players == null || obstacles == null)
        {
            players = GameMapPanel.players;
            obstacles = GameMapPanel.obstacles;
        }

        if (players == null || obstacles == null)
        {
            return;
        }

        if (reversed)
        {
            // Copy the circles onto a new list
            List<GraphFormulaGenerator.Circle> reversedObstacles = new ArrayList<>();
            for (GraphFormulaGenerator.Circle circle : obstacles)
            {
                GraphFormulaGenerator.Circle copyCircle = new GraphFormulaGenerator.Circle(-circle.x, circle.y, circle.radius);
                reversedObstacles.add(copyCircle);
            }

            // Copy players
            List<Player> reversedPlayers = new ArrayList<>();
            for (Player player : players)
            {
                Player copyPlayer = new Player(player);
                for (Soldier soldier : copyPlayer.getSoldiers())
                {
                    soldier.x = Constants.PLANE_LENGTH - soldier.x; // Reverse soldier's x position
                }
                reversedPlayers.add(copyPlayer);
            }

            if (m_MapPanel != null)
            {
                m_MapPanel.updateData(reversedPlayers, reversedObstacles);
            }
            return;
        }

        if (m_MapPanel != null)
        {
            m_MapPanel.updateData(players, obstacles);
        }
    }

    static class GameMapPanel extends JPanel
    {
        private static final double X_MIN = -25.0, X_MAX = 25.0;

        // The game is weird as fuck, it claims that it's 15 to -15 on Y axis
        // but in reality it's 15 to -14.2... because of the way it renders the graph plane
        private static final double Y_MIN = 15 - 450.0 * 50.0 / 770.0, Y_MAX = 15.0;
        // Game plane dimensions from Constants
        private static final int PLANE_DRAW_WIDTH = 770;
        private static final int PLANE_DRAW_HEIGHT = 450;

        public static List<Player> players = new ArrayList<>();
        public static List<GraphFormulaGenerator.Circle> obstacles = new ArrayList<>();
        private Function function = null;
        private double functionOffset = 0.0;
        public static Soldier selectedSoldier = null;
        private boolean drawingMode = false;
        private List<Point> drawnPoints = new ArrayList<Point>();

        public GameMapPanel()
        {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(800, 500));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e)
                {
                    handleMouseClick(e);
                }
            });
        }

        private int getOffsetX(int width)
        {
            return (width - PLANE_DRAW_WIDTH) / 2;
        }

        private int getOffsetY(int height)
        {
            return (height - PLANE_DRAW_HEIGHT) / 2;
        }

        private void handleMouseClick(java.awt.event.MouseEvent e)
        {
            int clickX = e.getX();
            int clickY = e.getY();

            int width = getWidth();
            int height = getHeight();
            int offsetX = getOffsetX(width);
            int offsetY = getOffsetY(height);

            // Drawing mode: add point
            if (drawingMode)
            {
                // Check if click is within the drawing area
                if (clickX >= offsetX && clickX <= offsetX + PLANE_DRAW_WIDTH &&
                    clickY >= offsetY && clickY <= offsetY + PLANE_DRAW_HEIGHT)
                {
                    // Convert screen coordinates to game coordinates
                    double gameX = screenToGameX(clickX - offsetX, PLANE_DRAW_WIDTH);
                    double gameY = screenToGameY(clickY - offsetY, PLANE_DRAW_HEIGHT);
                    drawnPoints.add(new Point(gameX, gameY));
                    repaint();
                }
                return;
            }

            // Normal mode: select soldier
            // Check if click is on any soldier
            for (Player player : players)
            {
                for (Soldier soldier : player.getSoldiers())
                {
                    if (soldier.isAlive())
                    {
                        int soldierX = soldier.getX();
                        int soldierY = soldier.getY();
                        double[] gamePos = GraphFormulaGenerator.fieldToGame(soldierX, soldierY);

                        int screenX = offsetX + gameToScreenX(gamePos[0], PLANE_DRAW_WIDTH);
                        int screenY = offsetY + gameToScreenY(gamePos[1], PLANE_DRAW_HEIGHT);

                        // Check if click is within soldier radius (12 pixels)
                        double distance = Math.hypot(clickX - screenX, clickY - screenY);
                        if (distance <= 12)
                        {
                            selectedSoldier = soldier;
                            // Recalculate offset with new selection
                            if (function != null)
                            {
                                functionOffset = calculateOffsetForSelectedSoldier();
                            }
                            repaint();
                            return;
                        }
                    }
                }
            }

            // If clicked on empty space, deselect
            selectedSoldier = null;
            functionOffset = 0.0;
            repaint();
        }

        public void setDrawingMode(boolean enabled)
        {
            drawingMode = enabled;
            if (!enabled)
            {
                drawnPoints.clear();
            }
            repaint();
        }

        public boolean isDrawingMode()
        {
            return drawingMode;
        }

        public List<Point> getDrawnPoints()
        {
            return drawnPoints;
        }

        public void clearDrawnPoints()
        {
            drawnPoints.clear();
            repaint();
        }

        public void updateData(List<Player> players, List<GraphFormulaGenerator.Circle> obstacles)
        {
            this.players = players != null ? players : new ArrayList<>();
            this.obstacles = obstacles != null ? obstacles : new ArrayList<>();
            repaint();
        }

        public void setFunctionToDraw(String functionStr)
        {
            try
            {
                if (functionStr != null && !functionStr.trim().isEmpty())
                {
                    this.function = new Function(functionStr);
                    // Calculate offset for selected soldier
                    this.functionOffset = calculateOffsetForSelectedSoldier();
                }
                else
                {
                    this.function = null;
                    this.functionOffset = 0.0;
                }
            }
            catch (MalformedFunction e)
            {
                this.function = null;
                this.functionOffset = 0.0;
                System.err.println("Invalid function: " + e.getMessage());
            }
            repaint();
        }

        private double calculateOffsetForSelectedSoldier()
        {
            if (selectedSoldier == null || function == null)
            {
                return 0.0;
            }

            // Convert to game coordinates
            double[] gamePos = GraphFormulaGenerator.fieldToGame(selectedSoldier.getX(), selectedSoldier.getY());

            // Get the PolishNotationFunction to evaluate at the soldier's x position
            PolishNotationFunction polishFunc = null;
            try
            {
                polishFunc = function.getStringFunc() != null ?
                    new PolishNotationFunction(function.getStringFunc()) : null;
            }
            catch (MalformedFunction e)
            {
                return 0.0;
            }

            if (polishFunc == null)
            {
                return 0.0;
            }

            // Calculate offset: offset = -f(x) + y
            double fx = polishFunc.evaluateFunction(gamePos[0], 0, 0);
            double offset = -fx + gamePos[1];

            return offset;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int offsetX = getOffsetX(width);
            int offsetY = getOffsetY(height);

            // Draw background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            drawGrid(g2d, offsetX, offsetY);
            drawAxes(g2d, offsetX, offsetY);
            drawObstacles(g2d, offsetX, offsetY);
            drawPlayers(g2d, offsetX, offsetY);
            drawDrawnPoints(g2d, offsetX, offsetY);
            drawFunction(g2d, offsetX, offsetY);
        }

        private void drawGrid(Graphics2D g2d, int offsetX, int offsetY)
        {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(1));

            for (double x = X_MIN; x <= X_MAX; x += 5)
            {
                int screenX = offsetX + gameToScreenX(x, PLANE_DRAW_WIDTH);
                g2d.drawLine(screenX, offsetY, screenX, offsetY + PLANE_DRAW_HEIGHT);
            }

            for (double y = -15; y <= 15; y += 5)
            {
                int screenY = offsetY + gameToScreenY(y, PLANE_DRAW_HEIGHT);
                g2d.drawLine(offsetX, screenY, offsetX + PLANE_DRAW_WIDTH, screenY);
            }

            // Border around the game area
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(offsetX, offsetY, PLANE_DRAW_WIDTH, PLANE_DRAW_HEIGHT);
        }

        private void drawAxes(Graphics2D g2d, int offsetX, int offsetY)
        {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));

            int yAxisScreen = offsetY + gameToScreenY(0, PLANE_DRAW_HEIGHT);
            g2d.drawLine(offsetX, yAxisScreen, offsetX + PLANE_DRAW_WIDTH, yAxisScreen);

            int xAxisScreen = offsetX + gameToScreenX(0, PLANE_DRAW_WIDTH);
            g2d.drawLine(xAxisScreen, offsetY, xAxisScreen, offsetY + PLANE_DRAW_HEIGHT);

            // Draw axis labels
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("X", offsetX + PLANE_DRAW_WIDTH - 15, yAxisScreen - 5);
            g2d.drawString("Y", xAxisScreen + 5, offsetY + 15);
        }

        private void drawObstacles(Graphics2D g2d, int offsetX, int offsetY)
        {
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2));

            for (GraphFormulaGenerator.Circle circle : obstacles)
            {
                int screenX = offsetX + gameToScreenX(circle.x, PLANE_DRAW_WIDTH);
                int screenY = offsetY + gameToScreenY(circle.y, PLANE_DRAW_HEIGHT);
                int screenRadius = (int) (circle.radius * PLANE_DRAW_WIDTH / (X_MAX - X_MIN));

                g2d.fillOval(screenX - screenRadius, screenY - screenRadius,
                            screenRadius * 2, screenRadius * 2);
            }
        }

        private void drawPlayers(Graphics2D g2d, int offsetX, int offsetY)
        {
            for (Player player : players)
            {
                Color teamColor = player.getTeam() == GraphServer.Constants.TEAM1 ? Color.BLUE : Color.RED;
                g2d.setColor(teamColor);
                g2d.setStroke(new BasicStroke(2));

                for (Soldier soldier : player.getSoldiers())
                {
                    if (soldier.isAlive())
                    {
                        int soldierX = soldier.getX();
                        int soldierY = soldier.getY();
                        double[] gamePos = GraphFormulaGenerator.fieldToGame(soldierX, soldierY);

                        int screenX = offsetX + gameToScreenX(gamePos[0], PLANE_DRAW_WIDTH);
                        int screenY = offsetY + gameToScreenY(gamePos[1], PLANE_DRAW_HEIGHT);

                        // Highlight selected soldier
                        if (soldier == selectedSoldier)
                        {
                            g2d.setColor(Color.YELLOW);
                            g2d.fillOval(screenX - 10, screenY - 10, 20, 20);
                            g2d.setColor(teamColor);

                            // Draw position text box
                            String posText = String.format("(%.2f, %.2f)", gamePos[0], gamePos[1]);
                            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                            FontMetrics fm = g2d.getFontMetrics();
                            int textWidth = fm.stringWidth(posText) + 5;
                            int textHeight = fm.getAscent() + 5;

                            int boxX = screenX + 15;
                            int boxY = screenY - 10;

                            // Draw box background
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect(boxX - 5, boxY - textHeight, textWidth + 10, textHeight + 10);
                            g2d.setColor(Color.BLACK);
                            g2d.drawRect(boxX - 5, boxY - textHeight, textWidth + 10, textHeight + 10);

                            // Draw text
                            g2d.setColor(Color.BLACK);
                            g2d.drawString(posText, boxX + 2, boxY);
                            g2d.setColor(teamColor);
                        }

                        // Draw soldier as a circle
                        g2d.fillOval(screenX - 6, screenY - 6, 12, 12);

                        // Draw outline
                        g2d.setColor(Color.BLACK);
                        g2d.drawOval(screenX - 6, screenY - 6, 12, 12);
                        g2d.setColor(teamColor);
                    }
                }
            }
        }

        private void drawDrawnPoints(Graphics2D g2d, int offsetX, int offsetY)
        {
            g2d.setColor(Color.MAGENTA);
            g2d.setStroke(new BasicStroke(2));

            // Draw lines connecting points
            if (drawnPoints.size() > 1)
            {
                for (int i = 0; i < drawnPoints.size() - 1; i++)
                {
                    Point p1 = drawnPoints.get(i);
                    Point p2 = drawnPoints.get(i + 1);

                    int screenX1 = offsetX + gameToScreenX(p1.x, PLANE_DRAW_WIDTH);
                    int screenY1 = offsetY + gameToScreenY(p1.y, PLANE_DRAW_HEIGHT);
                    int screenX2 = offsetX + gameToScreenX(p2.x, PLANE_DRAW_WIDTH);
                    int screenY2 = offsetY + gameToScreenY(p2.y, PLANE_DRAW_HEIGHT);

                    g2d.drawLine(screenX1, screenY1, screenX2, screenY2);
                }
            }

            // Draw points as circles
            for (Point point : drawnPoints)
            {
                int screenX = offsetX + gameToScreenX(point.x, PLANE_DRAW_WIDTH);
                int screenY = offsetY + gameToScreenY(point.y, PLANE_DRAW_HEIGHT);

                g2d.fillOval(screenX - 4, screenY - 4, 8, 8);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(screenX - 4, screenY - 4, 8, 8);
                g2d.setColor(Color.MAGENTA);
            }
        }

        private void drawFunction(Graphics2D g2d, int offsetX, int offsetY)
        {
            if (function == null)
            {
                return;
            }

            g2d.setColor(Color.GREEN);
            g2d.setStroke(new BasicStroke(1.0f));

            PolishNotationFunction polishFunc = null;
            try
            {
                polishFunc = function.getStringFunc() != null ?
                    new PolishNotationFunction(function.getStringFunc()) : null;
            }
            catch (MalformedFunction e)
            {
                return;
            }

            if (polishFunc == null)
            {
                return;
            }

            // Evaluate function across x range
            double step = 0.1;
            double prevX = X_MIN;
            double prevY = polishFunc.evaluateFunction(prevX, 0, 0) + functionOffset;

            for (double x = X_MIN + step; x <= X_MAX; x += step)
            {
                double y = polishFunc.evaluateFunction(x, 0, 0) + functionOffset;

                int screenX1 = offsetX + gameToScreenX(prevX, PLANE_DRAW_WIDTH);
                int screenY1 = offsetY + gameToScreenY(prevY, PLANE_DRAW_HEIGHT);
                int screenX2 = offsetX + gameToScreenX(x, PLANE_DRAW_WIDTH);
                int screenY2 = offsetY + gameToScreenY(y, PLANE_DRAW_HEIGHT);

                // Only draw if values are valid
                if (!Double.isNaN(prevY) && !Double.isInfinite(prevY) &&
                    !Double.isNaN(y) && !Double.isInfinite(y))
                {
                    g2d.drawLine(screenX1, screenY1, screenX2, screenY2);
                }

                prevX = x;
                prevY = y;
            }
        }

        private int gameToScreenX(double gameX, int drawWidth)
        {
            return (int) ((gameX - X_MIN) / (X_MAX - X_MIN) * drawWidth);
        }

        private int gameToScreenY(double gameY, int drawHeight)
        {
            return (int) ((Y_MAX - gameY) / (Y_MAX - Y_MIN) * drawHeight);
        }

        private double screenToGameX(int screenX, int drawWidth)
        {
            return X_MIN + (double) screenX / drawWidth * (X_MAX - X_MIN);
        }

        private double screenToGameY(int screenY, int drawHeight)
        {
            return Y_MAX - (double) screenY / drawHeight * (Y_MAX - Y_MIN);
        }
    }
}
