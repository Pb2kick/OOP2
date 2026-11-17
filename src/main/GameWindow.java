package main;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JFrame;

public class GameWindow {
	private JFrame jframe;
	
	public GameWindow(GamePanel gamePanel) {
		jframe =  new JFrame();
                 
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.setTitle("AMBOT");
        jframe.add(gamePanel);
        jframe.setResizable(true);
        jframe.pack();
        jframe.setLocationRelativeTo(null); // after pack(), so the real size is centred
        jframe.setVisible(true); 
		jframe.addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
       
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                gamePanel.getGame().WindowFocustLost();
            }
            
        });
		
	}
}
