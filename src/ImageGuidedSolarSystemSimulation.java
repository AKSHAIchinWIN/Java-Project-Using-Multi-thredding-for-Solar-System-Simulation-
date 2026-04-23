import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *UID-23BCA10444
 * Image-Guided Solar System Simulation
 * Modified to visually resemble image_0.png
 */
public class ImageGuidedSolarSystemSimulation extends JFrame {

    public ImageGuidedSolarSystemSimulation() {
        setTitle("Image-Guided Solar System Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1000); // Larger default window
        setLocationRelativeTo(null);

        SolarSystemPanel simulationPanel = new SolarSystemPanel();
        ControlPanel controls = new ControlPanel(simulationPanel);

        setLayout(new BorderLayout());
        add(simulationPanel, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> simulationPanel.startSimulation());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageGuidedSolarSystemSimulation().setVisible(true));
    }
}

/**
 * Main rendering panel with double buffering and image-like aesthetics
 */
class SolarSystemPanel extends JPanel {
    private final List<Planet> planets = new ArrayList<>();
    private final List<Asteroid> asteroids = new ArrayList<>();
    private final StarField starField;
    private final Timer renderTimer;
    private final Random random = new Random();
    private final Object planetsLock = new Object();

    private double zoom = 1.0;
    private double centerX, centerY;
    private volatile boolean showOrbits = true;
    private volatile double globalSpeed = 1.0;

    // Define colors for specific orbits
    private static final Color INNER_ORBIT_COLOR = new Color(255, 0, 0, 120); // Red
    private static final Color URANUS_ORBIT_COLOR = new Color(100, 200, 100, 120); // Greenish
    private static final Color NEPTUNE_ORBIT_COLOR = new Color(0, 0, 255); // Blue
    private static final Color PLUTO_ORBIT_COLOR = new Color(128, 0, 128); // Purple

    private static final BasicStroke THICK_STROKE = new BasicStroke(3);

    public SolarSystemPanel() {
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(1600, 1000));

        // Match clean background: sparse, dim stars
        starField = new StarField(2000, 1500, 100);
        initializePlanets();
        initializeAsteroidBelt();

        renderTimer = new Timer(16, e -> repaint());
    }

    private void initializePlanets() {
        // Updated sizes, distances (A, B) and perspectives to match image
        // Name, Color, Diameter, Semi-major, Semi-minor, Tilt(deg), Speed, HasMoon, OrbitColor, OrbitStroke

        // Inner planets - small and close, red orbits
        Planet mercury = new Planet("Mercury", new Color(169, 169, 169), 4, 70, 65, 7.0, 4.15, this, false, INNER_ORBIT_COLOR, null);
        Planet venus = new Planet("Venus", new Color(255, 198, 73), 5, 100, 95, 3.4, 3.07, this, false, INNER_ORBIT_COLOR, null);
        Planet earth = new Planet("Earth", new Color(79, 76, 176), 5, 130, 125, 0.0, 2.6, this, true, INNER_ORBIT_COLOR, null);
        earth.setMoon(new Moon());
        Planet mars = new Planet("Mars", new Color(193, 68, 56), 4, 170, 165, 1.9, 2.1, this, false, INNER_ORBIT_COLOR, null);

        // Mid solar system - and label the belt explicitly
        // Belt is (180, 230)
        Planet jupiter = new Planet("Jupiter", new Color(188, 149, 88), 15, 260, 240, 1.3, 1.15, this, false, INNER_ORBIT_COLOR, null);
        Planet saturn = new Planet("Saturn", new Color(221, 194, 133), 12, 340, 300, 2.5, 0.85, this, false, INNER_ORBIT_COLOR, null);

        // Outer solar system - massive expansion and eccentric perspective
        Planet uranus = new Planet("Uranus", new Color(121, 194, 203), 10, 460, 380, 0.8, 0.6, this, false, URANUS_ORBIT_COLOR, null);
        Planet neptune = new Planet("Neptune", new Color(62, 84, 232), 9, 580, 450, 1.8, 0.48, this, false, NEPTUNE_ORBIT_COLOR, THICK_STROKE);

        // Pluto - extremely eccentric and tilted, thick purple orbit
        Planet pluto = new Planet("Pluto", new Color(150, 133, 112), 3, 750, 500, 17.2, 0.4, this, false, PLUTO_ORBIT_COLOR, THICK_STROKE);

        synchronized (planetsLock) {
            planets.add(mercury);
            planets.add(venus);
            planets.add(earth);
            planets.add(mars);
            planets.add(jupiter);
            planets.add(saturn);
            planets.add(uranus);
            planets.add(neptune);
            planets.add(pluto);
        }
    }

    private void initializeAsteroidBelt() {
        // Shift belt slightly inward to create space
        for (int i = 0; i < 200; i++) {
            asteroids.add(new Asteroid(180, 230, random));
        }
    }

    public void startSimulation() {
        synchronized (planetsLock) {
            for (Planet planet : planets) {
                planet.start();
            }
        }
        renderTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        centerX = getWidth() / 2.0;
        centerY = getHeight() / 2.0;

        starField.draw(g2d, getWidth(), getHeight());
        drawSun(g2d);

        // Draw orbits first with specific colors and strokes
        if (showOrbits) {
            synchronized (planetsLock) {
                for (Planet planet : planets) {
                    Polygon orbit = planet.getOrbitPath(centerX, centerY, zoom, 150);
                    g2d.setColor(planet.getOrbitColor());
                    Stroke originalStroke = g2d.getStroke();
                    if (planet.getOrbitStroke() != null) {
                        g2d.setStroke(planet.getOrbitStroke());
                    }
                    g2d.drawPolyline(orbit.xpoints, orbit.ypoints, orbit.npoints);
                    g2d.setStroke(originalStroke); // Reset to default
                }
            }
        }

        // Asteroid belt
        for (Asteroid asteroid : asteroids) {
            asteroid.update(globalSpeed);
            Point pos = asteroid.getPosition(centerX, centerY, zoom);
            g2d.setColor(asteroid.getColor());
            g2d.fillOval(pos.x, pos.y, asteroid.getSize(), asteroid.getSize());
        }

        // Specific label for Asteroid Belt
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        double beltR = (180 + 230) / 2.0 * zoom;
        double beltLabelX = centerX + beltR * Math.cos(Math.toRadians(-60));
        double beltLabelY = centerY + beltR * Math.sin(Math.toRadians(-60)) * 0.85; // Perspective factor
        g2d.drawString("Asteroid belt", (int)beltLabelX - 30, (int)beltLabelY);

        // Planets and Moons
        synchronized (planetsLock) {
            for (Planet planet : planets) {
                Point pos = planet.getPosition(centerX, centerY, zoom);
                int size = (int)(planet.getDiameter() * zoom);

                // Saturn rings
                if (planet.getPlanetName().equals("Saturn")) {
                    g2d.setColor(new Color(200, 180, 140, 150));
                    int rw = (int)(size * 1.8);
                    int rh = (int)(size * 0.4);
                    g2d.setStroke(new BasicStroke(size / 6));
                    g2d.drawOval(pos.x - rw/2, pos.y - rh/2, rw, rh);
                }

                g2d.setColor(planet.getColor());
                g2d.fillOval(pos.x - size/2, pos.y - size/2, size, size);

                // Moon
                if (planet.hasMoon() && planet.getMoon() != null) {
                    Moon moon = planet.getMoon();
                    Point mPos = moon.getPosition(pos.x, pos.y);
                    g2d.setColor(moon.getColor());
                    g2d.fillOval(mPos.x - 1, mPos.y - 1, 2, 2); // Smaller moon
                }

                // Position label away from planet diameter
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.PLAIN, 11));
                g2d.drawString(planet.getPlanetName(), pos.x - 20, pos.y - size/2 - 10);
            }
        }
    }

    private void drawSun(Graphics2D g2d) {
        int sunSize = (int)(30 * zoom); // Smaller sun
        // Simplified sun
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)(centerX - sunSize/2), (int)(centerY - sunSize/2), sunSize, sunSize);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Sun", (int)centerX - 15, (int)centerY + sunSize/2 + 15);
    }

    public void setPaused(boolean paused) {
        synchronized (planetsLock) {
            for (Planet p : planets) {
                if (paused) p.pausePlanet(); else p.unpausePlanet();
            }
        }
    }

    public void setGlobalSpeed(double speed) {
        this.globalSpeed = speed;
        synchronized (planetsLock) {
            for (Planet p : planets) p.setSpeedMultiplier(speed);
        }
    }

    public void setZoom(double z) {
        if (z >= 0.3 && z <= 3.0) zoom = z;
    }

    public double getZoom() { return zoom; }
    public void toggleOrbits() { showOrbits = !showOrbits; }
}

/**
 * Planet thread with fixed method names and specific orbit properties
 */
class Planet extends Thread {
    private final String name;
    private final Color color;
    private final int diameter;
    private final double semiMajorAxis;
    private final double semiMinorAxis;
    private final double tiltAngle;
    private final double baseSpeed;
    private final boolean hasMoonFlag;
    private final Object pauseLock = new Object();

    private final Color orbitColor;
    private final Stroke orbitStroke;

    private volatile double currentAngle = 0.0;
    private volatile boolean paused = false;
    private volatile boolean running = true;
    private volatile double speedMultiplier = 1.0;
    private Moon moon;

    public Planet(String name, Color color, int diameter, double a, double b,
                  double tiltDeg, double speed, SolarSystemPanel panel, boolean hasMoon,
                  Color orbitColor, Stroke orbitStroke) {
        this.name = name;
        this.color = color;
        this.diameter = diameter;
        this.semiMajorAxis = a;
        this.semiMinorAxis = b;
        this.tiltAngle = Math.toRadians(tiltDeg);
        this.baseSpeed = speed;
        this.hasMoonFlag = hasMoon;
        this.orbitColor = orbitColor;
        this.orbitStroke = orbitStroke;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            synchronized (pauseLock) {
                while (paused) {
                    try { pauseLock.wait(); }
                    catch (InterruptedException e) { return; }
                }
            }

            synchronized (this) {
                currentAngle += baseSpeed * speedMultiplier * 0.01;
                if (currentAngle > 2 * Math.PI) currentAngle -= 2 * Math.PI;
            }

            if (moon != null) moon.update();

            try { Thread.sleep(16); }
            catch (InterruptedException e) { break; }
        }
    }

    public Point getPosition(double cx, double cy, double zoom) {
        double a = semiMajorAxis * zoom;
        double b = semiMinorAxis * zoom;
        double x = a * Math.cos(currentAngle);
        double y = b * Math.sin(currentAngle);
        double rx = x * Math.cos(tiltAngle) - y * Math.sin(tiltAngle);
        double ry = x * Math.sin(tiltAngle) + y * Math.cos(tiltAngle);
        return new Point((int)(cx + rx), (int)(cy + ry));
    }

    public Polygon getOrbitPath(double cx, double cy, double zoom, int points) {
        Polygon p = new Polygon();
        double a = semiMajorAxis * zoom;
        double b = semiMinorAxis * zoom;
        for (int i = 0; i <= points; i++) {
            double ang = 2 * Math.PI * i / points;
            double x = a * Math.cos(ang);
            double y = b * Math.sin(ang);
            double rx = x * Math.cos(tiltAngle) - y * Math.sin(tiltAngle);
            double ry = x * Math.sin(tiltAngle) + y * Math.cos(tiltAngle);
            p.addPoint((int)(cx + rx), (int)(cy + ry));
        }
        return p;
    }

    // Fixed method names (not overriding Thread's final methods)
    public void pausePlanet() { paused = true; }
    public void unpausePlanet() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }
    public void setSpeedMultiplier(double m) { speedMultiplier = m; }

    public String getPlanetName() { return name; }
    public Color getColor() { return color; }
    public int getDiameter() { return diameter; }
    public boolean hasMoon() { return hasMoonFlag; }
    public void setMoon(Moon m) { this.moon = m; }
    public Moon getMoon() { return moon; }
    public Color getOrbitColor() { return orbitColor; }
    public Stroke getOrbitStroke() { return orbitStroke; }
}

class Moon {
    private double angle = 0;
    private final double speed = 0.1;
    private final Color color = Color.LIGHT_GRAY;

    public void update() {
        angle += speed;
        if (angle > 2 * Math.PI) angle -= 2 * Math.PI;
    }

    public Point getPosition(int earthX, int earthY) {
        return new Point((int)(earthX + 10 * Math.cos(angle)),
                (int)(earthY + 10 * Math.sin(angle)));
    }
    public Color getColor() { return color; }
}

class Asteroid {
    private double angle;
    private final double distance;
    private final double speed;
    private final int size;
    private final Color color;

    public Asteroid(double minDist, double maxDist, Random rand) {
        this.distance = minDist + rand.nextDouble() * (maxDist - minDist);
        this.angle = rand.nextDouble() * 2 * Math.PI;
        this.speed = 0.002 + rand.nextDouble() * 0.001;
        this.size = 1 + rand.nextInt(2); // Slightly smaller asteroids
        int g = 100 + rand.nextInt(80);
        this.color = new Color(g, g-20, g-40);
    }

    public void update(double multiplier) {
        angle += speed * multiplier;
        if (angle > 2 * Math.PI) angle -= 2 * Math.PI;
    }

    public Point getPosition(double cx, double cy, double zoom) {
        double r = distance * zoom;
        return new Point((int)(cx + r * Math.cos(angle)),
                (int)(cy + r * Math.sin(angle) * 0.85)); // Perspective factor
    }
    public int getSize() { return size; }
    public Color getColor() { return color; }
}

class StarField {
    private final Star[] stars;
    private final int w, h;

    private static class Star {
        int x, y;
        double brightness, twinkleSpeed;
        Star(int w, int h, Random r) {
            x = r.nextInt(w); y = r.nextInt(h);
            brightness = 0.2 + r.nextDouble() * 0.5; // Dimmer stars
            twinkleSpeed = 0.01 + r.nextDouble() * 0.02;
        }
        double getAlpha() {
            return brightness * (0.8 + 0.2 * Math.sin(System.currentTimeMillis() * twinkleSpeed));
        }
    }

    public StarField(int w, int h, int count) {
        this.w = w; this.h = h;
        stars = new Star[count];
        Random r = new Random();
        for (int i = 0; i < count; i++) stars[i] = new Star(w, h, r);
    }

    public void draw(Graphics2D g, int pw, int ph) {
        double sx = (double)pw / w;
        double sy = (double)ph / h;
        for (Star s : stars) {
            int a = (int)(255 * s.getAlpha());
            g.setColor(new Color(255, 255, 255, a));
            g.fillRect((int)(s.x * sx), (int)(s.y * sy), 1, 1);
        }
    }
}

class ControlPanel extends JPanel {
    public ControlPanel(SolarSystemPanel sim) {
        setBackground(new Color(30, 30, 30));
        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton pauseBtn = new JButton("Pause");
        pauseBtn.addActionListener(e -> {
            boolean p = pauseBtn.getText().equals("Pause");
            sim.setPaused(p);
            pauseBtn.setText(p ? "Resume" : "Pause");
        });
        style(pauseBtn);
        add(pauseBtn);

        add(new JLabel("Speed:"));
        JSlider speed = new JSlider(0, 100, 10);
        speed.addChangeListener(e -> {
            if (!speed.getValueIsAdjusting())
                sim.setGlobalSpeed(speed.getValue() / 10.0);
        });
        add(speed);

        JButton zoomOut = new JButton("Zoom Out");
        zoomOut.addActionListener(e -> sim.setZoom(sim.getZoom() - 0.2));
        style(zoomOut);
        add(zoomOut);

        JButton zoomIn = new JButton("Zoom In");
        zoomIn.addActionListener(e -> sim.setZoom(sim.getZoom() + 0.2));
        style(zoomIn);
        add(zoomIn);

        JButton orbits = new JButton("Toggle Orbits");
        orbits.addActionListener(e -> sim.toggleOrbits());
        style(orbits);
        add(orbits);

        // Added status label from image
        JLabel status = new JLabel("| 9 Planets + Asteroid Belt");
        status.setForeground(Color.WHITE);
        add(status);
    }

    private void style(JButton b) {
        b.setBackground(new Color(60, 60, 60));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
    }
}