package com.qanpi;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

class Console {
    static String newLine = "\n";
    final static private JTextPane component = new JTextPane();

    static JTextPane getComponent() {
        return component;
    }

    static private void log(String msg, SimpleAttributeSet sas) {
        try {
            Document doc = component.getDocument();
            doc.insertString(doc.getLength(), msg + newLine, sas);
        } catch (BadLocationException e) {
            System.err.println("An unknown error occurred when trying to append text to the document.");
            e.printStackTrace();
            System.exit(2);
        }
    }

    static void log(String msg) {
        log(msg, new SimpleAttributeSet());
    }

    static void logErr(String msg) {
        //make the text color of exceptions red
        SimpleAttributeSet errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, Color.red);
        log(msg, errorStyle);
    }

    static String read() {
        return component.getText();
    }

    static void clear() {
        component.setText("");
    }
}
