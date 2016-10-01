package controllers;

import com.google.inject.Inject;
import models.Kingdom;
import org.apache.poi.ss.formula.functions.T;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;
import services.contracts.ConfigService;
import services.contracts.DataService;
import services.contracts.FileService;
import services.contracts.TaskProgress;
import views.MainWindow;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MainController {
    private final DataService dataService;
    private final MainWindow view;
    private final FileService fileService;
    private final ConfigService configService;
    private final DeferredManager deferredManager;

    @Inject
    public MainController(final DataService dataService, final FileService fileService, final ConfigService configService, final DeferredManager deferredManager) throws InterruptedException {
        this.dataService = dataService;
        this.fileService = fileService;
        this.configService = configService;
        this.deferredManager = deferredManager;

        view = new MainWindow();
        view.setTitle("Test");
        view.pack();
        view.setVisible(true);

        view.addExecuteListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acquire();
                ((JButton) e.getSource()).setEnabled(false);
                view.getInterruptButton().setEnabled(true);
            }
        });

        view.addInteruptListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((JButton) e.getSource()).setEnabled(false);
            }
        });

        view.getKingdomTree().setModel(null);
        view.getKingdomTree().setRootVisible(false);

        refreshTree();
    }

    private void refreshTree() {
        configService.getProperty("dataDir")
                .then(new DonePipe<String, TreeModel, Throwable, Object>() {
                    @Override
                    public Promise<TreeModel, Throwable, Object> pipeDone(String path) {
                        return fileService.buildTree(path);
                    }
                })
                .then(new DoneCallback<TreeModel>() {
                    @Override
                    public void onDone(TreeModel treeModel) {
                        view.getKingdomTree().setModel(treeModel);
                    }
                });
    }

    public void acquire() {
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

        dataService.acquire(kingdoms)
                .always(new AlwaysCallback<Void, Throwable>() {
                    @Override
                    public void onAlways(Promise.State state, Void aVoid, Throwable throwable) {
                        view.getExecuteButton().setEnabled(true);
                        view.getInterruptButton().setEnabled(false);
                        view.setGlobalProgressionBar(0);
                    }
                })
                .progress(new ProgressCallback<Object>() {
                    @Override
                    public void onProgress(Object progress) {
                        view.setGlobalProgressionBar(((TaskProgress) progress).getTotal());
                    }
                })
                .done(new DoneCallback<Void>() {
                    @Override
                    public void onDone(Void result) {
                        view.updateGlobalProgressionText("Update finished.");
                        view.setGlobalProgressionBar(0);
                    }
                })
                .fail(new FailCallback<Throwable>() {
                    @Override
                    public void onFail(Throwable throwable) {
                        System.err.println(throwable.toString());
                        view.updateGlobalProgressionText("An error occured.");
                        view.setGlobalProgressionBar(0);
                    }
                });
    }
}
