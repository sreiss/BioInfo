package views;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class MainWindow extends JFrame implements PropertyChangeListener {
    private JPanel mainPanel;
    private JTree kingdomTree;
    private JButton executeButton;
    private JProgressBar globalProgressionBar;
    private JPanel actionPannel;
    private JPanel topPannel;
    private JPanel progressionPannel;
    private JLabel progressionLabel;
    private JPanel progressionTextPanel;
    private JPanel progressionBarPannel;
    private JButton interruptButton;
    private JScrollPane kingdomScrollPane;
    private JButton optionsButton;
    private JLabel optionsLabel;
    private JPanel rightPannel;
    private JPanel optionsPannel;
    private JTextArea logTextArea;
    private JCheckBox virusesCheckBox;
    private JCheckBox prokaryotesCheckBox;
    private JCheckBox eukaryotaCheckBox;
    private JPanel bottomPannel;

    public JTree getKingdomTree() {
        return kingdomTree;
    }

    public JPanel getOptionsPannel() {
        return optionsPannel;
    }

    public JPanel getRightPannel() {
        return rightPannel;
    }

    public JLabel getProgressionLabel() {
        return progressionLabel;
    }

    private SwingWorker currentTask;

    public MainWindow() throws HeadlessException, InterruptedException {
        $$$setupUI$$$();
        this.setContentPane(mainPanel);
        PrintStream printStream = new PrintStream(new TextAreaOutputStream(logTextArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    public void addExecuteListener(ActionListener e) {
        executeButton.addActionListener(e);
    }

    public void addInteruptListener(ActionListener e) {
        interruptButton.addActionListener(e);
    }

    public void setKingdomTreeModel(TreeModel model) {
        kingdomTree.setModel(model);
    }

    public void setGlobalProgressionBar(int taskCount) {
        globalProgressionBar.setMaximum(taskCount);
        //globalProgressionBar.setIndeterminate(false);
//        if (taskCount > 0) {
//            progressionLabel.setText("Traitement de " + taskCount + " éléments.");
//        }
        progressionPannel.setVisible(true);
    }

    public void setGlobalProgressionBarTask(SwingWorker task) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        currentTask = task;
        currentTask.addPropertyChangeListener(this);
        currentTask.execute();
    }

    public void updateGlobalProgressionBar(int value) {
        if (value <= globalProgressionBar.getMaximum()) {
            globalProgressionBar.setValue(value);
            //progressionLabel.setText("Progression " + value + "/" + globalProgressionBar.getMaximum());
        } else {
            progressionPannel.setVisible(false);
        }
    }

    public void updateGlobalProgressionText(String text) {
        if (!text.equals("")) {
            progressionLabel.setText(text);
            //globalProgressionBar.setIndeterminate(true);
            progressionPannel.setVisible(true);
        } else {
            // globalProgressionBar.setIndeterminate(false);
            progressionPannel.setVisible(false);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //if (!done) {
        int progress = currentTask.getProgress();
        updateGlobalProgressionBar(progress);
        //}
    }

    public JButton getExecuteButton() {
        return executeButton;
    }

    public JButton getInterruptButton() {
        return interruptButton;
    }

    public JCheckBox getEukaryotaCheckBox() {
        return eukaryotaCheckBox;
    }

    public JCheckBox getProkaryotesCheckBox() {
        return prokaryotesCheckBox;
    }

    public JCheckBox getVirusesCheckBox() {
        return virusesCheckBox;
    }

    public JProgressBar getGlobalProgressionBar() {
        return globalProgressionBar;
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setFocusable(true);
        mainPanel.setPreferredSize(new Dimension(1000, 500));
        topPannel = new JPanel();
        topPannel.setLayout(new BorderLayout(0, 0));
        mainPanel.add(topPannel, BorderLayout.CENTER);
        optionsPannel = new JPanel();
        optionsPannel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        optionsPannel.setEnabled(true);
        topPannel.add(optionsPannel, BorderLayout.EAST);
        optionsLabel = new JLabel();
        optionsLabel.setText("Kingdoms");
        optionsPannel.add(optionsLabel);
        rightPannel = new JPanel();
        rightPannel.setLayout(new BorderLayout(0, 0));
        optionsPannel.add(rightPannel);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        rightPannel.add(panel1, BorderLayout.CENTER);
        eukaryotaCheckBox = new JCheckBox();
        eukaryotaCheckBox.setSelected(true);
        eukaryotaCheckBox.setText("Eukaryota");
        panel1.add(eukaryotaCheckBox);
        virusesCheckBox = new JCheckBox();
        virusesCheckBox.setSelected(true);
        virusesCheckBox.setText("Viruses");
        panel1.add(virusesCheckBox);
        prokaryotesCheckBox = new JCheckBox();
        prokaryotesCheckBox.setHorizontalAlignment(11);
        prokaryotesCheckBox.setSelected(true);
        prokaryotesCheckBox.setText("Prokaryota");
        panel1.add(prokaryotesCheckBox);
        kingdomScrollPane = new JScrollPane();
        topPannel.add(kingdomScrollPane, BorderLayout.CENTER);
        kingdomTree = new JTree();
        kingdomTree.setEditable(false);
        kingdomTree.setEnabled(true);
        kingdomTree.setRootVisible(false);
        kingdomTree.setShowsRootHandles(true);
        kingdomTree.putClientProperty("JTree.lineStyle", "");
        kingdomScrollPane.setViewportView(kingdomTree);
        bottomPannel = new JPanel();
        bottomPannel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(bottomPannel, BorderLayout.SOUTH);
        progressionPannel = new JPanel();
        progressionPannel.setLayout(new BorderLayout(0, 0));
        bottomPannel.add(progressionPannel);
        progressionTextPanel = new JPanel();
        progressionTextPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        progressionPannel.add(progressionTextPanel, BorderLayout.NORTH);
        progressionLabel = new JLabel();
        progressionLabel.setText("Progression");
        progressionTextPanel.add(progressionLabel);
        progressionBarPannel = new JPanel();
        progressionBarPannel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        progressionPannel.add(progressionBarPannel, BorderLayout.SOUTH);
        globalProgressionBar = new JProgressBar();
        globalProgressionBar.setPreferredSize(new Dimension(300, 12));
        globalProgressionBar.setString("");
        progressionBarPannel.add(globalProgressionBar);
        actionPannel = new JPanel();
        actionPannel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPannel.add(actionPannel);
        executeButton = new JButton();
        executeButton.setBackground(new Color(-1250068));
        executeButton.setEnabled(true);
        executeButton.setHideActionText(false);
        executeButton.setText("Execute");
        executeButton.setVerticalTextPosition(0);
        executeButton.putClientProperty("hideActionText", Boolean.FALSE);
        actionPannel.add(executeButton);
        interruptButton = new JButton();
        interruptButton.setEnabled(false);
        interruptButton.setText("Interrupt");
        actionPannel.add(interruptButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
