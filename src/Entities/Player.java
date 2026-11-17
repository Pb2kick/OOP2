package Entities;

import static Utils.Constants.PlayerConstats.GetSpriteAmount;
import static Utils.Constants.PlayerConstats.IDLE;
import static Utils.Constants.PlayerConstats.RUNNING;
import static Utils.Constants.PlayerConstats.ATTACK;
import static Utils.Constants.PlayerConstats.CROUCH;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class Player extends Entity {
    /** Sheet frames are 50x37; drawn at exactly 3x so the sprite is not stretched. */
    public static final int DRAW_WIDTH = 150, DRAW_HEIGHT = 111;
    private int aniTick, aniIndex;
    private double aniSpeed = 23;
    private int playerAction = IDLE;
    private boolean moving = false, attacking = false, facingLeft = false;
    private BufferedImage[][] anims;
    private boolean left, up, right, down, ctrl, equip;
    private float playerSpeed = 2.0f;

    public Player(float x, float y) {
        super(x, y);
        loadAnimations();
    }

    public void update() {
        updatePos();
        updateAnims();
        setAnimation();
    }

    public void render(Graphics g) {
        BufferedImage frame = frameFor(playerAction, aniIndex);
        if (facingLeft) {
            // negative width mirrors the sheet frame horizontally
            g.drawImage(frame, (int) x + DRAW_WIDTH, (int) y, -DRAW_WIDTH, DRAW_HEIGHT, null);
        } else {
            g.drawImage(frame, (int) x, (int) y, DRAW_WIDTH, DRAW_HEIGHT, null);
        }
    }

    public boolean isFacingLeft() {
        return facingLeft;
    }

    /** The sheet frame shown for an animation action (= sheet row) at a given frame index. */
    BufferedImage frameFor(int action, int index) {
        return anims[action][index];
    }

    private void updateAnims() {
        aniTick++;

        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= GetSpriteAmount(playerAction)) {
                aniIndex = 0; // Reset animation
                attacking = false;
            }
        }
    }

    private void setAnimation() {


		int startAni = playerAction;


        if (ctrl) {
            playerAction = CROUCH; // If crouching, use CROUCH animation
        } else if (moving) {
            playerAction = RUNNING;
        } else {
            playerAction = IDLE;
        }

		if(attacking){
			playerAction = ATTACK;
		}

		if(startAni != playerAction){
			resestAniTick();
		}
    }

	public void resestAniTick(){
		aniTick = 0;
		aniIndex = 0;
	}

    private void updatePos() {
        moving = false;

        if (left && !right) {
            x -= playerSpeed;
            moving = true;
            facingLeft = true;
        } else if (!left && right) {
            x += playerSpeed;
            moving = true;
            facingLeft = false;
        }

        if (up && !down) {
            y -= playerSpeed;
            moving = true;
        } else if (!up && down) {
            y += playerSpeed;
            moving = true;
        }
    }

    private void loadAnimations() {
        InputStream is = getClass().getResourceAsStream("/Betteradventurer-Sheet.png");

        try {
            BufferedImage img = ImageIO.read(is);
            anims = new BufferedImage[img.getHeight() / 37][img.getWidth() / 50];

            for (int j = 0; j < anims.length; j++) {
                for (int i = 0; i < anims[j].length; i++) {
                    anims[j][i] = img.getSubimage(i * 50, j * 37, 50, 37);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


	public void resetDirBooleans() {
		left = false;
		right = false;
		up = false;
		down = false;
		ctrl = false;
    }

	public void setAttacking(boolean attacking){
		this.attacking = attacking;
	}

    public void setCtrl(boolean ctrl) {
        this.ctrl = ctrl;
        if (ctrl) {
            aniIndex = 0; // Reset crouch animation when starting
        }
    }

    public boolean getCtrl() {
        return ctrl;
    }

    public boolean getLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public boolean getUp() {
        return up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public boolean getRight() {
        return right;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public boolean getDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

	
    
}


