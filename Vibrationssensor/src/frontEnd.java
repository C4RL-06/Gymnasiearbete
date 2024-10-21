import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class frontEnd {
    // Original map image size: 1140x820 (aspect ratio 1140/820)
    private static final int ORIGINAL_WIDTH = 1140;
    private static final int ORIGINAL_HEIGHT = 820;
    private static final double ASPECT_RATIO = (double) ORIGINAL_HEIGHT / ORIGINAL_WIDTH;
    private ArrayList<Point> sensorCoordinates = new ArrayList<>();

    JFrame window;
    JTabbedPane tabbedPane;
    JPanel mapPanel;

    public void startFrontEnd() {
        System.out.println("Front end started");
        startSettings();
    }

    private void startSettings() {

        window = new JFrame();
        window.setSize(500, 500);
        window.setTitle("Road Guard Admin");
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        // Creates the map image in mapPanel
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image mapImage = new ImageIcon("./assets/karta.png").getImage();

                // Get current panel dimensions and calculate scaling
                int panelWidth = window.getWidth();
                int panelHeight = window.getHeight();

                int imageWidth = (int) (panelHeight / ASPECT_RATIO);
                int imageHeight = panelHeight;

                int centerX = (panelWidth - imageWidth) / 2;

                if (mapImage != null) {
                    // Render the scaled image
                    g.drawImage(mapImage, centerX, 0, imageWidth, imageHeight, this);
                } else {
                    // Error message if image doesn't render
                    g.drawString("Image not found", 20, 20);
                }

                // Scale factor to map original image coordinates to resized image
                double scaleX = (double) imageWidth / ORIGINAL_WIDTH;
                double scaleY = (double) imageHeight / ORIGINAL_HEIGHT;

                // Draw the red rectangles at their scaled positions
                g.setColor(Color.RED);
                for (Point coordinate : sensorCoordinates) {
                    int x = (int) (coordinate.x * scaleX) + centerX;
                    int y = (int) (coordinate.y * scaleY);
                    g.fillRect(x - 4, y - 4, 8, 8);
                }
            }
        };

        // Add mouse listener to mapPanel
        mapPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Get current panel dimensions and calculate scaling
                int panelWidth = window.getWidth();
                int panelHeight = window.getHeight();

                int imageWidth = (int) (panelHeight / ASPECT_RATIO);
                int imageHeight = panelHeight;
                int centerX = (panelWidth - imageWidth) / 2;

                // Only register clicks inside the image bounds
                if (e.getX() >= centerX && e.getX() <= centerX + imageWidth && e.getY() <= imageHeight) {
                    // Calculate the click relative to the original image size
                    double scaleX = (double) ORIGINAL_WIDTH / imageWidth;
                    double scaleY = (double) ORIGINAL_HEIGHT / imageHeight;

                    int imgX = (int) ((e.getX() - centerX) * scaleX);
                    int imgY = (int) (e.getY() * scaleY);
                    System.out.println("New sensor coordinate: " + imgX + ", " + imgY);

                    // Store the coordinates relative to the original image
                    sensorCoordinates.add(new Point(imgX, imgY));
                    mapPanel.repaint();
                }
            }
        });

        mapPanel.setBackground(new Color(36, 47, 62));

        // Test page
        JPanel testPage = new JPanel();
        testPage.add(new JLabel("Test page"));
        testPage.setBackground(new Color(36, 47, 62));

        // Available Tabs (Row Order is Tab Index)
        tabbedPane.addTab("Map", mapPanel);
        tabbedPane.addTab("Test Page", testPage);

        // Set the tabbedPane to fill the window
        window.setLayout(new BorderLayout());
        window.getContentPane().setBackground(new Color(36, 47, 62));
        window.add(tabbedPane, BorderLayout.CENTER);
        window.setVisible(true);

        // Revalidate and repaint the window when resized
        window.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                mapPanel.revalidate();
                mapPanel.repaint();
            }
        });
    }
}
