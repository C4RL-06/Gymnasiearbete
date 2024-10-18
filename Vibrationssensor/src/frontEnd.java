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
    private ArrayList<ArrayList<Integer>> sensorCoordinates = new ArrayList<>();

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
        window.setSize(500, 500);
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

                int centerX = (window.getWidth() - ((int) (window.getHeight()/ASPECT_RATIO))) / 2;
                if (mapImage != null) {

                    //Render image (-10 is a manual fix to a small blank space that was created to the left of the map)
                    g.drawImage(mapImage, centerX - 10, 0, (int) (window.getHeight()/ASPECT_RATIO), window.getHeight(), this);
                } else {
                    //Error message if image doesn't render
                    g.drawString("Image not found", 20, 20);
                }


                g.setColor(Color.RED);
                for (ArrayList<Integer> coordinate : sensorCoordinates) {
                    System.out.println(coordinate);
                    int x = coordinate.get(0);
                    int y = coordinate.get(1);
                    g.fillRect(x - 4, y - 4, 8, 8); // -4 to center rectangle on click
                }
            }
        };
        mapPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                ArrayList<Integer> sensorCoordinate = new ArrayList<>();
                sensorCoordinate.add(e.getX());
                sensorCoordinate.add(e.getY());
                sensorCoordinates.add(sensorCoordinate);
                mapPanel.repaint();
            }
        });


        mapPanel.setBackground(new Color(36, 47, 62));

        //Test page
        testPage = new JPanel();
        testPage.add(new JLabel("Test page"));
        testPage.setBackground(new Color(36, 47, 62));

        //Available Tabs (Row Order is Tab Index)
        tabbedPane.addTab("Map", mapPanel);
        tabbedPane.addTab("Test Page", testPage);

        //Set the tabbedPane to fill the window
        window.setLayout(new BorderLayout());
        window.getContentPane().setBackground(new Color(36, 47, 62));
        window.add(tabbedPane, BorderLayout.CENTER);
        window.setVisible(true);

        //This ensures the window will revalidate and repaint when resized
        //This could be imported into this class, but we decided to write the libraries in-line
        window.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                mapPanel.revalidate();
                mapPanel.repaint();
            }
        });
    }
}