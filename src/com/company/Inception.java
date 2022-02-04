package com.company;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


public class Inception extends JFrame {
    JTextPane editor;
    JFileChooser fileChooser;

    Inception() {
        //Create the main editor text pane
        editor = new JTextPane();
        //and the scroll pane for it
        JScrollPane editorScrollPane = new JScrollPane(editor);
        editorScrollPane.setPreferredSize(new Dimension(500, 300));

        //Create the console uneditable text pane
        //and the scroll pane for it
        JScrollPane consoleScrollPane = new JScrollPane(Console.getComponent());
        consoleScrollPane.setPreferredSize(new Dimension(300, 200));

        //Split pane between editor and console - allows the panes to be resized
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1); //so that the console doesn't shrink with the window

        add(splitPane, BorderLayout.CENTER);

        //Create the menu bar at the top of the screen
        setJMenuBar(createMenuBar());

        //Create the file chooser to allow user to open/save files
        fileChooser = new JFileChooser(System.getProperty("user.dir")); //open current directory initially
        fileChooser.addChoosableFileFilter(new JavaExtensionFilter());
        fileChooser.addChoosableFileFilter(new TextExtensionFilter());
        fileChooser.setAcceptAllFileFilterUsed(false); //disallow user to open a file of any other extension
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new OpenFileAction());
        fileMenu.add(new SaveFileAction());

//        JMenu editMenu = new JMenu("Edit");

        mb.add(fileMenu);

        return mb;
    }

    class OpenFileAction extends AbstractAction {
        public OpenFileAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            int returnVal = fileChooser.showOpenDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                try {
                    editor.setPage(f.toURI().toURL());
                } catch (FileNotFoundException e) {
                    Console.logErr("Requested file not found.");
                    e.printStackTrace();
                } catch (IOException e) {
                    Console.logErr("The contents of the file are not supported by the editor.");
                    e.printStackTrace();
                }
            }
        }
    }

    class SaveFileAction extends AbstractAction {
        public SaveFileAction() {
            super("Save As...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            int returnVal = fileChooser.showSaveDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String ext = fileChooser.getFileFilter().getDescription();
                File f = fileChooser.getSelectedFile();

                if (!f.getName().contains(ext)) //in case the file name is given as just "file" (with no .ext)
                    f = new File(fileChooser.getSelectedFile() + ext);

                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(editor.getText());
                } catch (IOException e) {
                    Console.logErr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        //I just copy-pasted the following from an example...

        // Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }

    //Open the editor's GUI at startup
    private static void createAndShowGUI() {
        final JFrame IDE = new Inception();
        IDE.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        IDE.pack();
        IDE.setVisible(true);
    }

}





