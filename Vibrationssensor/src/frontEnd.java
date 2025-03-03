import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class frontEnd implements backEnd.CollisionListener, backEnd.DeviceListener {
    // Original map image size: 1140x820 (aspect ratio 1140/820)
    private static final int ORIGINAL_WIDTH = 1140;
    private static final int ORIGINAL_HEIGHT = 820;
    private static final double ASPECT_RATIO = (double) ORIGINAL_HEIGHT / ORIGINAL_WIDTH;
    private ArrayList<Point> sensorCoordinates = new ArrayList<>();
    private ArrayList<Point> collisionBetweenSensors = new ArrayList<>();
    private ArrayList<Point> pairedSensors = new ArrayList<>();
    private ArrayList<Integer> singleDetectionsList = new ArrayList<>();
    backEnd backEndOBJ = new backEnd();

    JFrame window;
    JTabbedPane tabbedPane;
    JPanel mapPanel, settingsPanel;
    DefaultTableModel mapSensorsTableModel, physicalInputsTabelModel;

    public void startFrontEnd() {
        startSettings();
        System.out.println("Front end started");

        backEndOBJ.setCollisionListener(this);
        backEndOBJ.setDeviceListener(this);
        backEndOBJ.startBackEnd();
    }
    public void onCollisionDetected(int sensor1, int sensor2, int singleOrDoubleDetection) {
        if (singleOrDoubleDetection == 2) {
            System.out.println("FrontEnd: Collision detected between sensors " + sensor1 + " and " + sensor2);

            Thread thread = new Thread(() -> {
                collisionBetweenSensors.add(new Point(sensor1, sensor2));
                mapPanel.repaint();
                try {
                    Thread.sleep(10000); // Pause for the specified delay
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted: " + e.getMessage());
                }
                collisionBetweenSensors.remove(new Point(sensor1, sensor2));
                mapPanel.repaint();
            });
            thread.start();
        } else {
            System.out.println("FrontEnd: 1 singular detector fired");

            Thread thread = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (!singleDetectionsList.contains(sensor1)) {
                    singleDetectionsList.add(sensor1);
                }
                mapPanel.repaint();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("Sleep interrupted");
                }
                if (singleDetectionsList.contains(sensor1)) {
                    singleDetectionsList.remove(Integer.valueOf(sensor1));
                }
                mapPanel.repaint();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("Sleep interrupted");
                }
            }
            });
            thread.start();
        }
    }
    public void onDevicesChanged(ArrayList<Integer>devicesList) {
        updateInputsTableModel(devicesList);
    };
    private void startSettings() {
        window = new JFrame();
        window.setSize(500, 500);
        window.setTitle("Road Guard Admin");
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setIconImage(new ImageIcon("./assets/logo.png").getImage());

        sensorCoordinates.add(new Point(623, 86));
        sensorCoordinates.add(new Point(708, 278));
        sensorCoordinates.add(new Point(798, 451));

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

                //Draw lines to indicate a crash between 2 coordinates
                g.setColor(Color.ORANGE);
                if (pairedSensors.size() >= 2 && collisionBetweenSensors.size() >= 1) {
                    ArrayList<Point> sensor1s = new ArrayList<>();
                    ArrayList<Point> sensor2s = new ArrayList<>();

                    for (Point collisionSensors : collisionBetweenSensors) {
                        for (Point pair : pairedSensors) {
                            if (pair.y == collisionSensors.x) {
                                sensor1s.add(sensorCoordinates.get(pair.x));
                                break;
                            }
                        }
                    }
                    for (Point collisionSensors : collisionBetweenSensors) {
                        for (Point pair : pairedSensors) {
                            if (pair.y == collisionSensors.y) {
                                sensor2s.add(sensorCoordinates.get(pair.x));
                                break;
                            }
                        }
                    }
                    for (int i = 0; i < sensor2s.size(); i++) {
                        int x1 = (int) (sensor1s.get(i).x * scaleX) + centerX;
                        int y1 = (int) (sensor1s.get(i).y * scaleY);

                        int x2 = (int) (sensor2s.get(i).x * scaleX) + centerX;
                        int y2 = (int) (sensor2s.get(i).y * scaleY);
                        g.drawLine(x1, y1, x2, y2);
                    }
                }

                // Draw the red rectangles at their scaled positions
                g.setColor(Color.RED);
                for (Point coordinate : sensorCoordinates) {
                    int x = (int) (coordinate.x * scaleX) + centerX;
                    int y = (int) (coordinate.y * scaleY);
                    g.fillRect(x - 4, y - 4, 8, 8);
                }

                g.setColor(Color.GREEN);
                for (int singleSensor : singleDetectionsList) {
                    for (Point pair : pairedSensors) {
                        if (pair.y == singleSensor) {
                            int x = (int) (sensorCoordinates.get(pair.x).x * scaleX) + centerX;
                            int y = (int) ((sensorCoordinates.get(pair.x).y) * scaleY);
                            g.drawRect(x - 5, y - 5, 10, 10);
                        }
                    }
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
                    updateSensorsTableModel();
                    mapPanel.repaint();
                }
            }
        });

        mapPanel.setBackground(new Color(36, 47, 62));

        // Settings page
        settingsPanel = new JPanel();
        settingsPanel.setBackground(new Color(200, 200, 200));
        settingsPanel.setLayout(null);

        JLabel settingsLabel = new JLabel("Settings");
        settingsLabel.setSize(100, 25);
        settingsLabel.setLocation((window.getWidth() - 50)/2, 2);
        settingsPanel.add(settingsLabel);

        String[] columnNamesSensors = {"Map Sensors"};
        mapSensorsTableModel = new DefaultTableModel(columnNamesSensors, 0);
        JTable mapSensorsTable = new JTable(mapSensorsTableModel);
        mapSensorsTable.setForeground(Color.BLACK);
        mapSensorsTable.setGridColor(Color.BLACK);
        mapSensorsTable.setBorder(new LineBorder(Color.BLACK));

        // Scroll Pane to wrap around the table
        JScrollPane sensorsScrollPane = new JScrollPane(mapSensorsTable);
        sensorsScrollPane.setBounds(30, 30, 100, window.getHeight() - 125);
        settingsPanel.add(sensorsScrollPane);

        String[] columNamesPhysicalInputs = {"Physical inputs"};
        physicalInputsTabelModel = new DefaultTableModel(columNamesPhysicalInputs, 0);
        JTable physicalInputsTable = new JTable(physicalInputsTabelModel);
        physicalInputsTable.setForeground(Color.BLACK);
        physicalInputsTable.setGridColor(Color.BLACK);
        physicalInputsTable.setBorder(new LineBorder(Color.BLACK));

        JScrollPane inputsScrollPane = new JScrollPane(physicalInputsTable);
        inputsScrollPane.setBounds(160, 30, 100, window.getHeight() - 125);
        settingsPanel.add(inputsScrollPane);

        physicalInputsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int selectedSensorRow = mapSensorsTable.getSelectedRow();
                    int selectedInputsRow = physicalInputsTable.getSelectedRow();

                    if (doesntContainSelectedSensorsOrInputs(selectedSensorRow, selectedInputsRow)) {
                        pairedSensors.add(new Point(selectedSensorRow, selectedInputsRow));
                        System.out.println("Sensors successfully paried sensor: " + sensorCoordinates.get(selectedSensorRow) + " with input: " + selectedInputsRow);
                    } else {
                        System.out.println("Pair failed");
                    }
                }
            }
        });

        // Available Tabs (Row Order is Tab Index)
        tabbedPane.addTab("Map", mapPanel);
        tabbedPane.addTab("Settings", settingsPanel);

        // Set the tabbedPane to fill the window
        window.setLayout(new BorderLayout());
        window.getContentPane().setBackground(new Color(36, 47, 62));
        window.add(tabbedPane, BorderLayout.CENTER);
        window.setVisible(true);

        // Revalidate and repaint the window when resized
        settingsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                settingsPanel.revalidate();
                settingsPanel.repaint();

                int panelWidth = settingsPanel.getWidth();
                int panelHeight = settingsPanel.getHeight();

                settingsLabel.setLocation((panelWidth - 50)/2, 2); //50 = label width/2
                sensorsScrollPane.setBounds(30, 30, 100, panelHeight - 60);
                inputsScrollPane.setBounds(160, 30, 100, panelHeight - 60);
            }
        });
        updateSensorsTableModel();
    }
    public boolean doesntContainSelectedSensorsOrInputs(int selectedSensorRow, int selectedInputRow)  {
        for (Point pair : pairedSensors) {
            if (sensorCoordinates.get(pair.x) != sensorCoordinates.get(selectedSensorRow) && backEndOBJ.physicalIDs.get(pair.y) != selectedInputRow) {

            } else {
                return false;
            }
        }
        return true;
    }
    private void updateSensorsTableModel() {
        // Clear the current table model
        mapSensorsTableModel.setRowCount(0);
        // Add each sensor coordinate as a single string in (X, Y) format
        for (Point point : sensorCoordinates) {
            mapSensorsTableModel.addRow(new Object[]{point.x + ", " + point.y});
        }
    }
    private void updateInputsTableModel(ArrayList<Integer>devicesList) {
        // Clear the current table model
        physicalInputsTabelModel.setRowCount(0);
        // Add each sensor coordinate as a single string in (X, Y) format
        for (int inputID : devicesList) {
            physicalInputsTabelModel.addRow(new Object[]{inputID});
        }
    }
}
