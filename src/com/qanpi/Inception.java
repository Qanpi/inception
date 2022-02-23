package com.qanpi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


public class Inception extends JFrame implements WindowListener {
    private final JTextPane editor;
    private final Runner runner;

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
        JScrollPane consoleScrollPane = new JScrollPane(new Console().getComponent());
        consoleScrollPane.setPreferredSize(new Dimension(300, 200));

        //Split pane between editor and console - allows the panes to be resized
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1); //so that the console doesn't shrink with the window

        add(splitPane, BorderLayout.CENTER);
        setJMenuBar(createMenuBar());
        addWindowListener(this);

        runner = new Runner();

        //ONLY FOR DEBUG
//        try {
//            loadFile(new File("C:\\Users\\aleks\\OneDrive - Suomalaisen Yhteiskoulun Osakeyhtiö\\Tiedostot\\School\\Pre-IB\\Term 3\\ComSci\\Inception\\test\\src\\HelloWorld.java"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new OpenFileAction());
        fileMenu.add(new SaveAction());
        fileMenu.add(new SaveAsAction());

        JMenu codeMenu = new JMenu("Code");
        codeMenu.add(new RunAction());

        mb.add(fileMenu);
        mb.add(codeMenu);

        return mb;
    }

    private void loadFile(File f) throws IOException {
        editor.setPage(f.toURI().toURL());
        currentFile = f;
        setTitle(currentFile.getName());
    }

    class OpenFileAction extends AbstractAction {
        public OpenFileAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            String startPath = currentFile == null ? System.getProperty("user.dir") : currentFile.getPath();
            JFileChooser fc = new JFileChooser(startPath); //open current directory initially
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showOpenDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try {
                    loadFile(f);
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

    class SaveAction extends AbstractAction {
        public SaveAction() {super("Save");}

        @Override
        public void actionPerformed(ActionEvent ev) {
            if (currentFile != null && currentFile.exists()) {
                try (FileWriter fw = new FileWriter(currentFile)) {
                    fw.write(editor.getText());
                    fw.close();
                    loadFile(currentFile);
                } catch (IOException e) {
                    Console.logErr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }
            } else {
                //redirect to save as, if no file opened (file doesn't exist)
                new SaveAsAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        }
    }

    class SaveAsAction extends AbstractAction {
        public SaveAsAction() {
            super("Save As...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            String startPath = currentFile == null ? System.getProperty("user.dir") : currentFile.getPath();
            JFileChooser fc = new JFileChooser(startPath); //open current file location
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
                    fw.close();
                    loadFile(f);
                } catch (IOException e) {
                    Console.logErr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }

            }
        }
    }

    class RunAction extends AbstractAction {
        RunAction() {
            super("Run");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                runner.run(currentFile);
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
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
        IDE.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        IDE.pack();
        IDE.setVisible(true);
    }

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        if (runner.isRunning()) {
            int returnVal = JOptionPane.showConfirmDialog(this, "A process is currently running. Do you wish to terminate and exit?");
            if (returnVal == JOptionPane.YES_OPTION) {
                runner.terminate();
                dispose();
            }
        } else {
            dispose();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}
}





