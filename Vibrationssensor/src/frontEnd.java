import javax.swing.*;
import java.awt.*;

public class frontEnd {
    // Original map image size: 1140x820 (aspect ratio 1140/820)
    private static final int ORIGINAL_WIDTH = 1140;
    private static final int ORIGINAL_HEIGHT = 820;
    private static final double ASPECT_RATIO = (double) ORIGINAL_HEIGHT / ORIGINAL_WIDTH;
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 575;

    JFrame window;
    JTabbedPane tabbedPane;
    JPanel mapPanel;
    JPanel testPage;

    public void startFrontEnd() {
        System.out.println("Front end started");
        startSettings();
    }

    private void startSettings() {
        window = new JFrame();
        window.setSize(800, 800);
        window.setTitle("Road Guard Admin");
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        //Creates the map image in mapPanel
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image mapImage = new ImageIcon("./assets/karta.png").getImage();

                if (mapImage != null) {
                    //Aspec ratio calculation
                    int panelWidth = getWidth();
                    int panelHeight = (int) (panelWidth * ASPECT_RATIO); // Maintain aspect ratio

                    //Maximum size
                    if (panelWidth > MAX_WIDTH) {
                        panelWidth = MAX_WIDTH;
                        panelHeight = (int) (panelWidth * ASPECT_RATIO); // Recalculate height
                    }

                    if (panelHeight > MAX_HEIGHT) {
                        panelHeight = MAX_HEIGHT;
                        panelWidth = (int) (panelHeight / ASPECT_RATIO); // Recalculate width
                    }

                    int centerX = (getWidth() - panelWidth) / 2;

                    //Render image
                    g.drawImage(mapImage, centerX, 0, panelWidth, panelHeight, this);
                } else {
                    //Error message if image doesn't render
                    g.drawString("Image not found", 20, 20);
                }
            }
        };

        //Test page
        testPage = new JPanel();
        testPage.add(new JLabel("Test page"));

        //Available Tabs (Row Order is Tab Index)
        tabbedPane.addTab("Map", mapPanel);
        tabbedPane.addTab("Test Page", testPage);

        //Set the tabbedPane to fill the window
        window.setLayout(new BorderLayout());
        window.add(tabbedPane, BorderLayout.CENTER);
        window.setVisible(true);

        //This ensures the panel will revalidate and repaint properly when resized
        //This could be imported to the class but we decided to write the libraries in-line
        window.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                mapPanel.revalidate();
                mapPanel.repaint();
            }
        });
    }
}