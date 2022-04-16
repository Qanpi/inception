package com.qanpi;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.OutputStream;
import java.io.PrintWriter;

class Console {
    final static String nL = System.lineSeparator();
    static IO io;

    final private JTextPane component;

    JTextPane getComponent() {
        return component;
    }

    Console () {
        component = new JTextPane();
        component.setEditable(false);
        bindUserInputListener();

        io = new IO();
    }

    /**
     * Listen to input into the console and automatically redirect it to the end of the already present text.
     */
    void bindUserInputListener() {
        KeyListener focusAction = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                int last = component.getDocument().getLength();
                component.setCaretPosition(last);
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        };

        component.addKeyListener(focusAction);
    }

    class IO {
        private void println(String msg, SimpleAttributeSet sas) {
            try {
                Document doc = component.getDocument();
                doc.insertString(doc.getLength(), msg + nL, sas);
            } catch (BadLocationException e) {
                //non-recoverable error as there is no way to indicate it to the user if the console is broken
                System.err.println("An unknown error occurred when trying to append text to the document.");
                e.printStackTrace();
                System.exit(2);
            }
        }

        void println(String msg) {
            println(msg, new SimpleAttributeSet());
        }

        void printerr(String msg) {
            //make the text color of exceptions red
            SimpleAttributeSet errorStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(errorStyle, Color.red);
            println(msg, errorStyle);
        }

        /**
         * Routs the text entered by the user to a certain OutputStream.
         * @param os The OutputStream to route text to
         */
        void routeTo(OutputStream os) {
            KeyListener routeAction = new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {

                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        e.consume(); //prevent from the default Enter key behaviour

                        String text = component.getText();
                        String input = text.substring(text.lastIndexOf("\n")+1);
                        //if the automatic flushing is not enabled, the text will not get properly sent to the output stream
                        PrintWriter pw = new PrintWriter(os, true);
                        pw.println(input);
                        println(""); //print a new line in the user console after the input
                    } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        e.consume();
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {

                }
            };

            component.addKeyListener(routeAction);
//            Action testAction = new AbstractAction() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    String text = component.getText();
//                    String input = text.substring(text.lastIndexOf("\n")+1);
//                    System.out.println(input);
//                    PrintWriter pw = new PrintWriter(os, true); //this flushing thing caused me such a headache
//                    pw.println(input);
//                    Console.log("");
//                }
//            };
//
//            component.registerKeyboardAction(testAction, KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED);
        }

        void clear() {
            component.setText("");
        }

        void setEditable(boolean b) {
            component.setEditable(b);
        }
    }
}
