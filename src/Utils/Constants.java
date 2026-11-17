package Utils;

public class Constants {
	
	public static class Directions{
		public final static int LEFT = 0;
		public final static int UP = 1;
		public final static int RIGHT = 2;
		public final static int DOWN= 3;
	}
	
	public static class PlayerConstats{
		 public final static int RUNNING = 1;
		 public final static int IDLE = 0;
		 public final static int ATTACK = 6;
		 public final static int JUMP =2;
		 public static final int CROUCH = 99;
		 public static final int WEPEQUIP = 90;
		 
		 public static int GetSpriteAmount(int player_action) {
			 switch(player_action) {
				 case RUNNING:
					 return 6;
				 case IDLE:
					 return 4;
				 case ATTACK:
				 case JUMP:
					 return 7;
				default:
					return 1;
			 }
		 }
	}
}
