package com.networkanalyzer;

import com.networkanalyzer.ui.MainWindow;
import com.networkanalyzer.ui.UITheme;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Apply dark theme before any component is created
        UITheme.applyGlobalTheme();

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
