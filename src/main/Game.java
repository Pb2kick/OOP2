package main;

import java.awt.Graphics;

import Entities.Player;

public class Game implements Runnable{
	
	private GamePanel gamePanel;
	private GameWindow gameWindow;
	private Thread gameThread;
	private final int FPS_SET = 120;
	private final int UPS_SET = 200;
	private Player player;
	
	
	public Game() {
		initClasses();
		gamePanel = new GamePanel(this); 
		gameWindow = new GameWindow(gamePanel);
		gamePanel.requestFocus();
		
		startGameLoop();
		
	}
	
	private void initClasses() {
		player = new Player(200, 200);
		
	}

	private void startGameLoop() {
		gameThread = new Thread(this);
		gameThread.start();
	}
	
	public void update() {
		player.update();
	}
	
	public void render(Graphics g) {
		 player.render(g);
	}
	
	
	@Override
	public void run() {
		double timePerFrame = 1000000000.0 / FPS_SET;
		double timePerUpdate =  1000000000.0 / UPS_SET;
		
		long previousTime = System.nanoTime();
		int  frames = 0;
		int updates = 0;
		long lastCheck = System.currentTimeMillis();
		
		double deltaU = 0;
		double deltaF = 0;
		while(true) {
			
			long currentTime = System.nanoTime();
			
			deltaU += (currentTime  - previousTime) / timePerUpdate;
			deltaF += (currentTime  - previousTime) / timePerFrame;
			previousTime = currentTime;
			
			
			if(deltaU >= 1) {
				update();
				updates++;
				deltaU--;
			}
			
			if(deltaF >= 1) {
				gamePanel.repaint();
				
				frames++;
				deltaF--;
			}
			
			
	        if(System.currentTimeMillis() - lastCheck >= 1000){
	        	lastCheck = System.currentTimeMillis();
	        	System.out.println("FPS: " + frames + " | UPS :" + updates);
	        	frames = 0;
	        	updates = 0;
	        }

	        // Rest until the next update or frame is due instead of spinning a full CPU core.
	        double waitU = (1 - deltaU) * timePerUpdate;
	        double waitF = (1 - deltaF) * timePerFrame;
	        long waitNanos = (long) Math.min(waitU, waitF);
	        if (waitNanos > 2_000_000) {
	        	try {
	        		Thread.sleep((waitNanos - 1_000_000) / 1_000_000);
	        	} catch (InterruptedException e) {
	        		Thread.currentThread().interrupt();
	        		return;
	        	}
	        } else {
	        	Thread.onSpinWait();
	        }
		}
	}
	
	public Player getPlayer() {
		return player;
		
	}

    public void WindowFocustLost() {
       player.resetDirBooleans();
    }
	
	
}
