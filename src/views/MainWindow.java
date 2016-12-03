package views;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
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
    private JPanel kingdomPanel;
    private JPanel optionsPannel;
    private JTextArea logTextArea;
    private JCheckBox virusesCheckBox;
    private JCheckBox prokaryotesCheckBox;
    private JCheckBox eukaryotaCheckBox;
    private JPanel bottomPannel;
    private JLabel timeRemainingLabel;
    private JPanel zipPanel;
    private JCheckBox genomesCheckBox;
    private JCheckBox genesCheckBox;
    private JPanel optionsPanel;
    private JPanel downloadProgressionPannel;
    private JPanel downloadProgressionTextPanel;
    private JPanel downloadProgressionBarPanel;
    private JLabel downloadLabel;
    private JProgressBar downloadProgressionBar;
    private JCheckBox plasmidsCheckBox;
    private JLabel downloadingLabel;
    private JLabel downloadedLabel;
    private JLabel apiStatusLabel;

    public JTree getKingdomTree() {
        return kingdomTree;
    }

    public JPanel getOptionsPannel() {
        return optionsPannel;
    }

    public JPanel getKingdomPanel() {
        return kingdomPanel;
    }

    public JLabel getProgressionLabel() {
        return progressionLabel;
    }

    private SwingWorker currentTask;

    public MainWindow() throws HeadlessException, InterruptedException {
        $$$setupUI$$$();
        this.setContentPane(mainPanel);
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


    public JLabel getTimeRemainingLabel() {
        return timeRemainingLabel;
    }

    public JProgressBar getDownloadProgressionBar() {
        return downloadProgressionBar;
    }

    public JLabel getDownloadProgressionLabel() {
        return downloadLabel;
    }

    public JCheckBox getGenomesCheckBox() {
        return genomesCheckBox;
    }

    public JCheckBox getGenesCheckBox() {
        return genesCheckBox;
    }

    public JCheckBox getPlasmidsCheckBox() {
        return plasmidsCheckBox;
    }

    public JLabel getDownloadedLabel() {
        return downloadedLabel;
    }

    public JLabel getDownloadingLabel() {
        return downloadingLabel;
    }

    public JLabel getApiStatusLabel() {
        return apiStatusLabel;
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
        downloadProgressionPannel = new JPanel();
        downloadProgressionPannel.setLayout(new BorderLayout(0, 0));
        bottomPannel.add(downloadProgressionPannel);
        downloadProgressionTextPanel = new JPanel();
        downloadProgressionTextPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        downloadProgressionPannel.add(downloadProgressionTextPanel, BorderLayout.NORTH);
        downloadLabel = new JLabel();
        downloadLabel.setText("Downloading");
        downloadProgressionTextPanel.add(downloadLabel);
        downloadProgressionBarPanel = new JPanel();
        downloadProgressionBarPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        downloadProgressionPannel.add(downloadProgressionBarPanel, BorderLayout.SOUTH);
        downloadProgressionBar = new JProgressBar();
        downloadProgressionBar.setPreferredSize(new Dimension(300, 12));
        downloadProgressionBarPanel.add(downloadProgressionBar);
        progressionPannel = new JPanel();
        progressionPannel.setLayout(new BorderLayout(0, 0));
        bottomPannel.add(progressionPannel);
        progressionTextPanel = new JPanel();
        progressionTextPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        progressionPannel.add(progressionTextPanel, BorderLayout.NORTH);
        progressionLabel = new JLabel();
        progressionLabel.setText("Progression");
        progressionTextPanel.add(progressionLabel);
        timeRemainingLabel = new JLabel();
        timeRemainingLabel.setText("");
        progressionTextPanel.add(timeRemainingLabel);
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
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridBagLayout());
        optionsPanel.setMinimumSize(new Dimension(452, 415));
        optionsPanel.setPreferredSize(new Dimension(452, 415));
        mainPanel.add(optionsPanel, BorderLayout.EAST);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        optionsPanel.add(panel1, gbc);
        panel1.setBorder(BorderFactory.createTitledBorder(null, "Options", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        kingdomPanel = new JPanel();
        kingdomPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        panel1.add(kingdomPanel, gbc);
        kingdomPanel.setBorder(BorderFactory.createTitledBorder(null, "Kingdoms", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
        eukaryotaCheckBox = new JCheckBox();
        eukaryotaCheckBox.setSelected(true);
        eukaryotaCheckBox.setText("Eukaryota");
        kingdomPanel.add(eukaryotaCheckBox);
        virusesCheckBox = new JCheckBox();
        virusesCheckBox.setSelected(true);
        virusesCheckBox.setText("Viruses");
        kingdomPanel.add(virusesCheckBox);
        prokaryotesCheckBox = new JCheckBox();
        prokaryotesCheckBox.setHorizontalAlignment(11);
        prokaryotesCheckBox.setSelected(true);
        prokaryotesCheckBox.setText("Prokaryota");
        kingdomPanel.add(prokaryotesCheckBox);
        plasmidsCheckBox = new JCheckBox();
        plasmidsCheckBox.setSelected(true);
        plasmidsCheckBox.setText("Plasmids");
        kingdomPanel.add(plasmidsCheckBox);
        zipPanel = new JPanel();
        zipPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        panel1.add(zipPanel, gbc);
        zipPanel.setBorder(BorderFactory.createTitledBorder(null, "Zip", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
        genomesCheckBox = new JCheckBox();
        genomesCheckBox.setText("Genomes");
        zipPanel.add(genomesCheckBox);
        genesCheckBox = new JCheckBox();
        genesCheckBox.setSelected(false);
        genesCheckBox.setText("Valid Genes");
        zipPanel.add(genesCheckBox);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        optionsPanel.add(panel2, gbc);
        panel2.setBorder(BorderFactory.createTitledBorder(null, "Progression", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        panel2.add(panel3, gbc);
        panel3.setBorder(BorderFactory.createTitledBorder("Download"));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 15, 10, 15);
        panel3.add(panel4, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Downloaded");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(label1, gbc);
        downloadedLabel = new JLabel();
        downloadedLabel.setText("-");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel4.add(downloadedLabel, gbc);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 15, 10, 15);
        panel3.add(panel5, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Downloading");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel5.add(label2, gbc);
        downloadingLabel = new JLabel();
        downloadingLabel.setText("-");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel5.add(downloadingLabel, gbc);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        optionsPanel.add(panel6, gbc);
        panel6.setBorder(BorderFactory.createTitledBorder("API Status"));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 3.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 10, 5, 10);
        panel6.add(panel7, gbc);
        apiStatusLabel = new JLabel();
        apiStatusLabel.setText("-");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel7.add(apiStatusLabel, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
