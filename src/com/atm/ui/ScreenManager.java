package com.atm.ui;

import com.atm.service.AtmService;
import com.atm.service.AtmSession;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ScreenManager {
    public interface KeypadListener {
        void onKeyPressed(String key);
        void onClear();
        void onCancel();
        void onEnter();
    }

    private final JPanel containerPanel;
    private final CardLayout cardLayout;
    private final AtmKioskFrame frame;
    private final AtmService atmService;
    private final Map<String, JPanel> screenMap = new HashMap<>();

    private AtmSession currentSession;
    private String currentScreenName;
    private KeypadListener activeKeypadListener;

    public ScreenManager(AtmKioskFrame frame) {
        this.frame = frame;
        this.atmService = new AtmService();
        this.cardLayout = new CardLayout();
        this.containerPanel = new JPanel(cardLayout);
        this.containerPanel.setBackground(UITheme.SCREEN_BG);
    }

    public JPanel getContainerPanel() {
        return containerPanel;
    }

    public void registerScreen(String name, JPanel screen) {
        screenMap.put(name, screen);
        containerPanel.add(screen, name);
    }

    public void showScreen(String name) {
        this.currentScreenName = name;
        JPanel screen = screenMap.get(name);
        if (screen instanceof KeypadListener) {
            this.activeKeypadListener = (KeypadListener) screen;
        } else {
            this.activeKeypadListener = null;
        }

        // Trigger refresh if screen supports it
        if (screen instanceof RefreshableScreen) {
            ((RefreshableScreen) screen).refreshScreen();
        }

        cardLayout.show(containerPanel, name);
        frame.updateKioskStatus(name, currentSession);
    }

    public void forwardKeypadPress(String key) {
        if (activeKeypadListener != null) {
            activeKeypadListener.onKeyPressed(key);
        }
    }

    public void forwardKeypadClear() {
        if (activeKeypadListener != null) {
            activeKeypadListener.onClear();
        }
    }

    public void forwardKeypadCancel() {
        if (activeKeypadListener != null) {
            activeKeypadListener.onCancel();
        } else if (currentSession != null) {
            // Default cancel returns to Main Menu
            showScreen("MAIN_MENU");
        }
    }

    public void forwardKeypadEnter() {
        if (activeKeypadListener != null) {
            activeKeypadListener.onEnter();
        }
    }

    public AtmSession getSession() { return currentSession; }
    public void setSession(AtmSession session) { this.currentSession = session; }
    public AtmService getAtmService() { return atmService; }
    public AtmKioskFrame getFrame() { return frame; }
    public String getCurrentScreenName() { return currentScreenName; }
    public JPanel getScreen(String name) { return screenMap.get(name); }

    public interface RefreshableScreen {
        void refreshScreen();
    }
}
