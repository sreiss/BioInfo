package controllers;

import com.google.inject.Inject;
import models.Kingdom;
import models.Organism;
import org.jdeferred.*;
import org.jdeferred.multiple.MasterProgress;
import services.contracts.DataService;
import services.contracts.TaskProgress;
import views.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        Kingdom[] kingdoms = {
                Kingdom.Eukaryota
        };
        dataService.acquire(kingdoms)
                .progress(new ProgressCallback<Object>() {
                    @Override
                    public void onProgress(Object progress) {
                        view.setGlobalProgressionBar(((TaskProgress) progress).getTotal());
                    }
                })
                .done(new DoneCallback<Void>() {
                    @Override
                    public void onDone(Void result) {
                        System.out.println(result);
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
