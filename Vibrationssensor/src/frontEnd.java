import javax.swing.*;
import java.awt.*;

public class frontEnd {

    JFrame window = new JFrame();
    JPanel mapPanel;

    public void startFrontEnd() {
        System.out.println("Front end started");
        startSettings();
    }

    private void startSettings(){

        window.setSize(new Dimension(800,800));
        window.setTitle("Road Guard Admin");
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mapPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image mapImage = new ImageIcon("././assets/karta.png").getImage();
                g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        mapPanel.setPreferredSize(new Dimension(800,800));
        window.setContentPane(mapPanel);

        window.pack();
        window.setVisible(true);
    }



}
