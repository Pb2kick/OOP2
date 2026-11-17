package ambot.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/** Forwards Swing key and mouse events into an {@link InputState}. Left mouse = attack. */
public final class SwingInputAdapter implements KeyListener, MouseListener {
    private final InputState state;

    public SwingInputAdapter(InputState state) {
        this.state = state;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        state.keyPressed(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        state.keyReleased(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) state.actionPressed(Action.ATTACK);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) state.actionReleased(Action.ATTACK);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
