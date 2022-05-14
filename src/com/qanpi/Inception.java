package com.qanpi;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.formdev.flatlaf.FlatDarculaLaf;

public class Inception extends JFrame implements WindowListener {
    private final Editor editor;
    private JMenuItem runButton;
    private CompletableFuture<Void> process;

    Inception() {
        super("Inception - No file open");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        //Initialize the editor
        editor = new Editor();
        JScrollPane editorScrollPane = new JScrollPane(editor.getComponent());
        editorScrollPane.setPreferredSize(new Dimension(600, 300));

        //Initialize the console
        Console cl = new Console();
        JScrollPane consoleScrollPane = new JScrollPane(cl.getComponent());
        consoleScrollPane.setPreferredSize(new Dimension(300, 200));

        //Split pane between editor and console
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1); //so that the console doesn't shrink with the window

        add(splitPane, BorderLayout.CENTER);
        setJMenuBar(createMenuBar());
        addWindowListener(this);

        pack();
        setVisible(true);

        welcomeMessage();
    }

    void welcomeMessage() {
        JLabel link = new JLabel("<HTML><U>GitHub.</U></HTML>");
        link.setForeground(Color.blue);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setToolTipText("https://github.com/Qanpi/inception");
        link.setAlignmentY(0.75F);
        link.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/Qanpi/inception"));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setPreferredSize(new Dimension(300, 100));
        textPane.setText("Inception is a small and rugged, but (as I'd like to think) an intuitive and functional Java IDE. \n\n" +
                "It is not meant for projects or actual use, but more so as a sandbox tool. " +
                "Feel free to play around and learn more about the project on ");
        textPane.insertComponent(link);

        JOptionPane.showConfirmDialog(this,
                textPane,
                "Welcome!", JOptionPane.DEFAULT_OPTION);
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new OpenFileAction());
        fileMenu.add(new SaveAction());
        fileMenu.add(new SaveAsAction());

        JMenu codeMenu = new JMenu("Code");
        runButton = new JMenuItem("Stop");
        runButton.setAction(new RunAction());
        runButton.setEnabled(false);
        codeMenu.add(runButton);

        JMenu settingsMenu = new JMenu("Settings");
        JMenu themeMenu = new JMenu("Themes");

        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            themeMenu.add(new SetThemeAction(info.getName()));
        }
        settingsMenu.add(themeMenu);

        mb.add(fileMenu);
        mb.add(codeMenu);
        mb.add(settingsMenu);

        return mb;
    }

    private void loadFile(File f) throws IOException {
        editor.openFile(f);
        runButton.setEnabled(true);
        setTitle("Inception - " + f.getName());
    }

    class OpenFileAction extends AbstractAction {
        public OpenFileAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            File cf = editor.getCurrentFile();

            String firstPath = System.getProperty("user.dir");
            if (cf != null) firstPath = cf.getPath();

            JFileChooser fc = new JFileChooser(firstPath);
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showOpenDialog(Inception.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try {
                    if (!f.getCanonicalPath().contains(File.separatorChar + "src" + File.separatorChar)){ //for future purposes when executing
                        JOptionPane.showMessageDialog(Inception.this,
                                "Please place your .java file inside a folder named 'src'. Sorry for the inconvenience!",
                                "Error!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    loadFile(f);
                } catch (IOException e) {
                    Console.io.printerr("Error while trying to open file.");
                    e.printStackTrace();
                }
            }
        }
    }

    class SaveAction extends AbstractAction {
        public SaveAction() {super("Save");}

        @Override
        public void actionPerformed(ActionEvent ev) {
            File cf = editor.getCurrentFile();
            if (cf != null && cf.exists()) {
                try (FileWriter fw = new FileWriter(cf)) {
                    fw.write(editor.getText());
                } catch (IOException e) {
                    Console.io.printerr("Unable to save the editor contents to the provided file.");
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
            File cf = editor.getCurrentFile();

            String firstPath = System.getProperty("user.dir");
            if (cf != null) firstPath = cf.getPath();

            JFileChooser fc = new JFileChooser(firstPath);
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showSaveDialog(Inception.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String ext = fc.getFileFilter().getDescription();
                File f = fc.getSelectedFile();

                if (!f.getName().contains(ext)) //in case the file name is given as just "file" (with no .ext)
                    f = new File(fc.getSelectedFile() + ext);

                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(editor.getText());
                } catch (IOException e) {
                    Console.io.printerr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }

                setTitle(f.getName());
            }
        }
    }

    void setRunButton() {
        runButton.setText("Run");
        runButton.setAction(new RunAction());
    }

    void setStopButton() {
        runButton.setText("Stop");
        runButton.setAction(new StopAction());
    }

    void toggleConsole() {Console.io.setEditable(!Console.io.isEditable());}

    class RunAction extends AbstractAction {
        RunAction() {
            super("Run");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Console.io.clear(); //remove previous output
            setStopButton();
            toggleConsole();

            File currentFile = editor.getCurrentFile();
            process = CompletableFuture.runAsync(() -> Runner.run(currentFile));

            process.thenRun(() -> {
                Runner.finish();
                setRunButton();
                toggleConsole();
            });
        }
    }

    class StopAction extends AbstractAction {
        StopAction() {
            super("Stop");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            process.complete(null);
        }
    }

    class SetThemeAction extends AbstractAction {
        SetThemeAction (String themeName) {
            super(themeName);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (e.getActionCommand().equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ex) {
                        Console.io.printerr("The selected theme is not supported.");
                        ex.printStackTrace();
                    }
                    break;
                }
            }
            updateUI();
         }
    }

    private void updateUI() {
        SwingUtilities.updateComponentTreeUI(this);
        pack();
    }

    public static void main(String[] args) {
        //I just copy-pasted the following from an example...

        // Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            UIManager.installLookAndFeel("Darcula", FlatDarculaLaf.class.getName());
            //Set Nimbus as the default Look and Feel
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Darcula".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final JFrame IDE = new Inception();
        });
    }

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        if (process != null && !process.isDone()) {
            int returnVal = JOptionPane.showConfirmDialog(this, "A process is currently running. Do you wish to terminate and exit?");
            if (returnVal == JOptionPane.YES_OPTION) {
                Runner.finish();
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

class JavaExtensionFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;

        int i = f.getName().indexOf(".");
        if (i != -1) {
            String ext = f.getName().substring(i);
            return ext.equals(".java");
        }
        return false;
    }

    @Override
    public String getDescription() {
        return ".java";
    }
}





