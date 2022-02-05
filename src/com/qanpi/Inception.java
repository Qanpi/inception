package com.qanpi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class Inception extends JFrame {
    private JTextPane editor;
    private Console console;
    private Runner runner;
    private File currentFile;

    Inception() {
        super("Inception");
        //Create the main editor text pane
        editor = new JTextPane();
        //and the scroll pane for it
        JScrollPane editorScrollPane = new JScrollPane(editor);
        editorScrollPane.setPreferredSize(new Dimension(600, 300));

        //Create the console uneditable text pane
        //and the scroll pane for it
        console = new Console();
        JScrollPane consoleScrollPane = new JScrollPane(console.getComponent());
        consoleScrollPane.setPreferredSize(new Dimension(300, 200));


        //Split pane between editor and console - allows the panes to be resized
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1); //so that the console doesn't shrink with the window

        add(splitPane, BorderLayout.CENTER);

        //Create the menu bar at the top of the screen
        setJMenuBar(createMenuBar());
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new OpenFileAction());
        fileMenu.add(new SaveFileAction());

        JMenu codeMenu = new JMenu("Code");
        codeMenu.add(new RunAction());

        mb.add(fileMenu);
        mb.add(codeMenu);

        return mb;
    }

    private void updateCurrentFile(File f) {
        currentFile = f;
        setTitle(currentFile.getName());
    }


    class OpenFileAction extends AbstractAction {
        public OpenFileAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            String openPath = currentFile == null ? System.getProperty("user.dir") : currentFile.getPath();
            JFileChooser fc = new JFileChooser(openPath); //open current directory initially
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showOpenDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try {
                    editor.setPage(f.toURI().toURL());
                    updateCurrentFile(f);
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
            String openPath = currentFile == null ? System.getProperty("user.dir") : currentFile.getPath();
            JFileChooser fc = new JFileChooser(openPath); //open current file location
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showSaveDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String ext = fc.getFileFilter().getDescription();
                File f = fc.getSelectedFile();

                if (!f.getName().contains(ext)) //in case the file name is given as just "file" (with no .ext)
                    f = new File(fc.getSelectedFile() + ext);

                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(editor.getText());
                } catch (IOException e) {
                    Console.logErr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }
                updateCurrentFile(f);
            }
        }
    }

    class RunAction extends AbstractAction {
        RunAction() {
            super("Run");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (runner == null) runner = new Runner();
            runner.run(currentFile);
        }
    }


    public static void main(String[] args) {
        //I just copy-pasted the following from an example...

        // Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            createAndShowGUI();
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





