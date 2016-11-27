package controllers;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import models.Kingdom;
import services.contracts.*;
import views.MainWindow;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreeModel;
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
    ListenableFuture<List<Kingdom>> currentFuture;

    @Inject
    public MainController(final KingdomService kingdomService, final FileService fileService, final ConfigService configService, final ProgressService progressService, final ProgramStatsService programStatsService) throws InterruptedException {
        this.kingdomService = kingdomService;
        this.fileService = fileService;
        this.configService = configService;
        this.progressService = progressService;
        this.programStatsService = programStatsService;

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
            }
            resetProgressService();
            ((JButton) e.getSource()).setEnabled(false);
        });

        view.getKingdomTree().setModel(null);
        view.getKingdomTree().setRootVisible(false);

        refreshTree();
    }

    private void resetProgressService() {
        TaskProgress taskProgress = progressService.getCurrentProgress();
        taskProgress.getProgress().set(0);
        taskProgress.setMessage(null);
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

        ListenableFuture<List<Kingdom>> acquireFuture = kingdomService.createKingdomTrees(kingdoms);
        currentFuture = acquireFuture;
        Futures.addCallback(acquireFuture, new FutureCallback<List<Kingdom>>(){
            @Override
            public void onSuccess(@Nullable List<Kingdom> kingdoms) {
                view.updateGlobalProgressionText("Update finished.");
                progressService.getCurrentProgress().getTotal().set(0);
                progressService.getCurrentProgress().getProgress().set(0);
                programStatsService.endAcquisitionTimeEstimation();
                view.setGlobalProgressionBar(0);
                view.getExecuteButton().setEnabled(true);
                view.getTimeRemainingLabel().setText("");
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (throwable instanceof CancellationException) {
                    view.updateGlobalProgressionText("Processing interrupted.");
                    progressService.getCurrentProgress().getTotal().set(0);
                    progressService.getCurrentProgress().getProgress().set(0);
                    programStatsService.endAcquisitionTimeEstimation();
                    view.setGlobalProgressionBar(0);
                    view.getExecuteButton().setEnabled(true);
                    view.getInterruptButton().setEnabled(false);
                    view.getTimeRemainingLabel().setText("");
                } else {
                    System.err.println(throwable.toString());
                    view.updateGlobalProgressionText("An error occured.");
                    progressService.getCurrentProgress().getTotal().set(0);
                    progressService.getCurrentProgress().getProgress().set(0);
                    programStatsService.endAcquisitionTimeEstimation();
                    view.setGlobalProgressionBar(0);
                    view.getExecuteButton().setEnabled(true);
                    view.getInterruptButton().setEnabled(false);
                    view.getTimeRemainingLabel().setText("");
                }
                resetProgressService();
            }
        });
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof ProgressService) {
            if (arg instanceof DownloadTaskPogress) {
                DownloadTaskPogress progress = (DownloadTaskPogress) arg;
                view.getDownloadProgressionBar().setIndeterminate(false);
                if (view.getDownloadProgressionBar().getMaximum() != progress.getTotal().get()) {
                    view.getDownloadProgressionBar().setMaximum(progress.getTotal().get());
                }
                view.getDownloadProgressionBar().setValue(progress.getProgress().get());
                view.getDownloadProgressionLabel().setText(String.format("Downloading: %d/%d", progress.getProgress().get(), progress.getTotal().get()));
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
