import javax.swing.*;
import java.awt.*;

public class frontEnd {

    JFrame window = new JFrame();
    JTabbedPane JTP;
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

        JTP = new JTabbedPane();
        //Creates Map Image In mapPanel
        mapPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image mapImage = new ImageIcon("././assets/karta.png").getImage();
                g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        mapPanel.setPreferredSize(new Dimension(800,800));
        //window.setContentPane(mapPanel);

        //Test page
        testPage = new JPanel();
        testPage.add(new JLabel("Test page"));


        //Available Tabs (Row Order is Tab Index)
        JTP.addTab("Map", mapPanel);
        JTP.addTab("Test Page", testPage);

        window.add(JTP);
        window.pack();
        window.setVisible(true);
    }




}