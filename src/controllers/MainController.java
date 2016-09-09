package controllers;

import com.google.inject.Inject;
import org.jdeferred.*;
import org.jdeferred.multiple.MasterProgress;
import services.contracts.DataService;
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

        dataService.saveData()
                .done(new DoneCallback<Void>() {
                    @Override
                    public void onDone(Void aVoid) {
                        System.out.println("Test excel file created.");
                    }
                });
    }

    public void acquire() {
        view.updateGlobalProgressionText("Begining the acquisition, the startup might take some time...");
        dataService.acquire()
                .progress(new ProgressCallback<MasterProgress>() {
                    @Override
                    public void onProgress(MasterProgress masterProgress) {
                        view.setGlobalProgressionBar(masterProgress.getTotal());
                        view.updateGlobalProgressionBar(masterProgress.getDone());
                    }
                })
                .done(new DoneCallback<Void>() {
                    @Override
                    public void onDone(Void result) {
                        System.out.println(result);
                    }
                });
    }
}
