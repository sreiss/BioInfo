package controllers;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import Utils.ZipUtils;
import models.Kingdom;
import services.contracts.*;
import services.exceptions.NothingToProcesssException;
import views.MainWindow;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.event.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CancellationException;

public class MainController implements Observer {
    private final KingdomService kingdomService;
    private final MainWindow view;
    private final FileService fileService;
    private final ConfigService configService;
    private final ProgressService progressService;
    private final ProgramStatsService programStatsService;
    private final ListeningExecutorService executorService;
    ListenableFuture<List<Kingdom>> currentFuture;

    @Inject
    public MainController(final KingdomService kingdomService, final FileService fileService, final ConfigService configService, final ProgressService progressService, final ProgramStatsService programStatsService, ListeningExecutorService executorService) throws InterruptedException {
        this.kingdomService = kingdomService;
        this.fileService = fileService;
        this.configService = configService;
        this.progressService = progressService;
        this.programStatsService = programStatsService;
        this.executorService = executorService;

        progressService.addObserver(this);
        programStatsService.addObserver(this);

        view = new MainWindow();
        view.setTitle("Test");
        view.pack();
        view.setVisible(true);

        view.addExecuteListener(e -> {
            acquire();
            ((JButton) e.getSource()).setEnabled(false);
            view.getInterruptButton().setEnabled(true);
        });

        view.addInteruptListener(e -> {
            if (currentFuture != null) {
                currentFuture.cancel(true);
                kingdomService.interrupt();
            }
            resetProgressService();
            ((JButton) e.getSource()).setEnabled(false);
        });

        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (currentFuture != null) {
                    currentFuture.cancel(true);
                    kingdomService.interrupt();
                }
                System.exit(0);
            }
        });

        view.getKingdomTree().setModel(null);
        view.getKingdomTree().setRootVisible(false);

        view.getFilterBioProjectTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                JTextField textField = (JTextField) e.getSource();
                if (textField.getText().equals("BioProject...")) {
                    textField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                JTextField textField = (JTextField) e.getSource();
                if (textField.getText().trim().equals("")) {
                    textField.setText("BioProject...");
                }
            }
        });

        refreshTree();
    }

    private void resetProgressService() {
        TaskProgress taskProgress = progressService.getCurrentProgress();
        taskProgress.getProgress().set(0);
        taskProgress.setStep(null);
        taskProgress.getTotal().set(0);
    }

    private void refreshTree() {
        String dataDir = configService.getProperty("dataDir");
        Futures.addCallback(fileService.buildTree(dataDir), new FutureCallback<TreeModel>() {
            @Override
            public void onSuccess(@Nullable TreeModel treeModel) {
                view.getKingdomTree().setModel(treeModel);
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });
    }

    private void acquire() {

        view.getTimeRemainingLabel().setText("Estimating ETA...");
        view.updateGlobalProgressionText("Begining the acquisition, the startup might take some time...");

        List<Kingdom> kingdoms = new ArrayList<Kingdom>();
        String bioProject = null;

        if (view.getEukaryotaCheckBox().isSelected()) {
            kingdoms.add(Kingdom.Eukaryota);
        }
        if (view.getProkaryotesCheckBox().isSelected()) {
            kingdoms.add(Kingdom.Prokaryotes);
        }
        if (view.getVirusesCheckBox().isSelected()) {
            kingdoms.add(Kingdom.Viruses);
        }
        if (view.getPlasmidsCheckBox().isSelected()) {
            kingdoms.add(Kingdom.Plasmids);
        }
        if (view.getFilterBioProjectCheckBox().isSelected()) {
            bioProject = view.getFilterBioProjectTextField().getText();
        }

        // Necessaire au zippage
        JCheckBox genomesCkb = view.getGenomesCheckBox();
        JCheckBox genesCkb = view.getGenesCheckBox();
        kingdomService.setGenesCkBIsSelected(genesCkb.isSelected());
        kingdomService.setGenomesCkBIsSelected(genomesCkb.isSelected());

        //gene
        String zipGene = configService.getProperty("gene");
        String[] explodeGenePath = zipGene.split("/");
        //genome
        String zipGenome = configService.getProperty("genome");
        String[] explodeGenomePath = zipGenome.split("/");

        ZipUtils.cleanSaveFolder(zipGene,explodeGenePath);
        ZipUtils.cleanSaveFolder(zipGenome,explodeGenomePath);

        Boolean genesBool = genesCkb.isSelected();
        Boolean genomesBool = genomesCkb.isSelected();

        ListenableFuture<List<Kingdom>> acquireFuture = kingdomService.createKingdomTrees(kingdoms, bioProject);
        currentFuture = acquireFuture;
        Futures.addCallback(acquireFuture, new FutureCallback<List<Kingdom>>(){
            @Override
            public void onSuccess(@Nullable List<Kingdom> kingdoms) {
                if (!kingdomService.getShouldInterrupt()) {
                    view.updateGlobalProgressionText("Update finished.");
                } else {
                    view.updateGlobalProgressionText("Processing interrupted.");
                }
                progressService.getCurrentProgress().getTotal().set(0);
                progressService.getCurrentProgress().getProgress().set(0);
                programStatsService.endAcquisitionTimeEstimation();
                view.setGlobalProgressionBar(0);
                view.getExecuteButton().setEnabled(true);
                view.getTimeRemainingLabel().setText("");

                if(genesBool){
                    if (new File(zipGene).exists()) {
                        ZipUtils zip = new ZipUtils(zipGene, explodeGenePath[1] + ".zip");
                        zip.ExecuteZip();
                    }
                }

                if(genomesBool){
                    if (new File(zipGenome).exists()) {
                        ZipUtils zip = new ZipUtils(zipGenome, explodeGenomePath[1] + ".zip");
                        zip.createGenomeDirectory(new File(zipGenome));
                        zip.ExecuteZip();
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {

                String zipGene = configService.getProperty("zipDir");
                String zipGenome = configService.getProperty("zipDir");

                if (throwable instanceof CancellationException) {
                    view.updateGlobalProgressionText("Processing interrupted.");
                } else if (throwable instanceof NothingToProcesssException) {
                    view.updateGlobalProgressionText("Nothing to process.");
                } else {
                    view.updateGlobalProgressionText("An error occured.");
                }
                progressService.getCurrentProgress().getTotal().set(0);
                progressService.getCurrentProgress().getProgress().set(0);
                programStatsService.endAcquisitionTimeEstimation();
                view.setGlobalProgressionBar(0);
                view.getExecuteButton().setEnabled(true);
                view.getInterruptButton().setEnabled(false);
                view.getTimeRemainingLabel().setText("");


                if(new File(zipGene).exists() && genesBool){
                    if (new File(zipGene).exists()) {
                        ZipUtils zip = new ZipUtils(zipGene, explodeGenePath[1] + ".zip");
                        zip.ExecuteZip();
                    }
                }

                if(new File(zipGenome).exists() && genomesBool){
                    if (new File(zipGenome).exists()) {
                        ZipUtils zip = new ZipUtils(zipGenome, explodeGenomePath[1] + ".zip");
                        zip.createGenomeDirectory(new File(zipGenome));
                        zip.ExecuteZip();
                    }
                }

                resetProgressService();
            }
        }, executorService);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof ProgressService) {
            if (arg instanceof DownloadTaskPogress) {
                DownloadTaskPogress progress = (DownloadTaskPogress) arg;
                view.getDownloadedLabel().setText(progress.getDownloaded());
                view.getDownloadingLabel().setText(progress.getDownloading());
            } else if (arg instanceof TaskProgress) {
                TaskProgress progress = (TaskProgress) arg;
                switch (progress.getStep()) {
                    case KingdomGathering:
                        view.updateGlobalProgressionText("Gathering kingdoms.");
                        break;
                    case KingdomsCreation:
                        break;
                    case DirectoriesCreationFinished:
                        view.updateGlobalProgressionText("Directories created.");
                        refreshTree();
                        break;
                    case OrganismProcessing:
                        view.updateGlobalProgressionText("Processing organisms.");
                        break;
                    default:
                        break;
                }
                if (view.getGlobalProgressionBar().getMaximum() != progress.getTotal().get()) {
                    view.setGlobalProgressionBar(progress.getTotal().get());
                }
                view.updateGlobalProgressionBar(progress.getProgress().get());
                view.updateGlobalProgressionText(String.format("Progression: %d/%d", progress.getProgress().get(), progress.getTotal().get()));
            } else if (arg instanceof ApiStatus) {
                ApiStatus apiStatus = (ApiStatus) arg;
                view.getApiStatusLabel().setText("<html>" + apiStatus.getMessage() + "</html>");
                view.getApiStatusLabel().setForeground(apiStatus.getColor());
            }
        } else if (o instanceof ProgramStatsService) {
            ProgramStat programStat = (ProgramStat) arg;

            int seconds = (int) (programStat.getTimeRemaining() / 1000) % 60 ;
            int minutes = (int) ((programStat.getTimeRemaining() / (1000*60)) % 60);
            int hours   = (int) ((programStat.getTimeRemaining() / (1000*60*60)) % 24);

            view.getTimeRemainingLabel().setText("ETA: " + hours + "h " + minutes + "min " + seconds + "s");
        }
    }
}
