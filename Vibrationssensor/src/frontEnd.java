import javax.swing.*;
import java.awt.*;

public class frontEnd {
    //Map size: 1140x820 (1140/820)
    JFrame window = new JFrame();
    JTabbedPane tabbedPane;
    JPanel mapPanel;
    JPanel testPage;


    public void startFrontEnd() {
        System.out.println("Front end started");
        startSettings();
    }

    private void startSettings(){

        window.setSize(new Dimension(800,800));
        window.setTitle("Road Guard Admin");
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        //Creates Map Image In mapPanel
        mapPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image mapImage = new ImageIcon("././assets/karta.png").getImage();
                g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        mapPanel.setPreferredSize(new Dimension(800,300));

        //Test page
        testPage = new JPanel();
        testPage.add(new JLabel("Test page"));


        //Available Tabs (Row Order is Tab Index)
        tabbedPane.addTab("Map", mapPanel);
        tabbedPane.addTab("Test Page", testPage);

        window.add(tabbedPane);
        window.pack();
        window.setVisible(true);
    }
}