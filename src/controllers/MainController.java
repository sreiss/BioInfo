package controllers;

import com.google.inject.Inject;
import models.Kingdom;
import org.jdeferred.*;
import services.contracts.DataService;
import services.contracts.TaskProgress;
import views.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MainController {
    private final DataService dataService;
    private final MainWindow view;

    @Inject
    public MainController(final DataService dataService) throws InterruptedException {
        this.dataService = dataService;
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
