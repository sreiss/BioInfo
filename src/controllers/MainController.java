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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public class MainController implements Observer {
    private final KingdomService kingdomService;
    private final MainWindow view;
    private final FileService fileService;
    private final ConfigService configService;
    private final ProgressService progressService;
    private final ProgramStatsService programStatsService;
    private final ListeningExecutorService executorService;
    private final ZipService zipService;
    ListenableFuture<List<Kingdom>> currentFuture;

    @Inject
    public MainController(final KingdomService kingdomService, final FileService fileService, final ConfigService configService, final ProgressService progressService, final ProgramStatsService programStatsService, ListeningExecutorService executorService, final ZipService zipService) throws InterruptedException {
        this.kingdomService = kingdomService;
        this.fileService = fileService;
        this.configService = configService;
        this.progressService = progressService;
        this.programStatsService = programStatsService;
        this.executorService = executorService;
        this.zipService = zipService;

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
        view.getKingdomTree().addTreeSelectionListener(e -> {
            Object[] nodePath = e.getPath().getPath();
            if (nodePath.length > 0) {
                String fileName = nodePath[nodePath.length - 1].toString();
                if (fileName.endsWith(".xlsx")) {
                    Path pathToOpen = Paths.get(".");
                    for (Object node : nodePath) {
                        pathToOpen = pathToOpen.resolve(node.toString());
                    }

                    try {
                        Desktop.getDesktop().open(new File(pathToOpen.toAbsolutePath().toString()));
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(view, "Unable to open the selected file " + fileName, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

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
                refreshTree();
                
                if(genesCkb.isSelected()){
                    if (new File(zipGene).exists()) {
                    	view.updateGlobalProgressionText("Zipping in progress.");
                    	ZipUtils zip = new ZipUtils(explodeGenePath[1], explodeGenePath[1] + ".zip");
                        zip.ExecuteZip();
                        view.updateGlobalProgressionText("Zipping finished.");
                    }
                }

                if(genomesCkb.isSelected()){
                	File folderGenome = new File(zipGenome);
                    if (folderGenome.exists()) {
//                    	view.updateGlobalProgressionText("Treatment Genome in progress.");
//                    	ListenableFuture<Boolean> genomeDirectory = zipService.createGenomeDirectory(folderGenome);
//                    	executorService.submit(() -> {
//                    		try{
//                    			genomeDirectory.get();
//                    			view.updateGlobalProgressionText("Treatment Genome Finished.");
//                    			
//                    			view.updateGlobalProgressionText("Zipping in progress.");
//                        		ZipUtils zip = new ZipUtils(explodeGenomePath[1], explodeGenomePath[1] + ".zip");
//                            	view.updateGlobalProgressionText("Zipping in progress.");
//                            	zip.ExecuteZip();
//                            	view.updateGlobalProgressionText("Zipping finished.");
//                    		} catch(Exception e){
//                    			e.printStackTrace();
//                    			view.updateGlobalProgressionText("Something goes wrong.");
//                    		}
//                    	});
//                    	view.updateGlobalProgressionText("Zipping in progress.");
//                    	                    	
                		ZipUtils zip = new ZipUtils(explodeGenomePath[1], explodeGenomePath[1] + ".zip");
                		
                		view.updateGlobalProgressionText("Treatment Genome in progress.");
                    	zip.createGenomeDirectory(new File(zipGenome));
                    	view.updateGlobalProgressionText("Treatment Genome Finished.");
                    	
                    	view.updateGlobalProgressionText("Zipping in progress.");
                    	zip.ExecuteZip();
                    	view.updateGlobalProgressionText("Zipping finished.");
                    
                	}
                }
                /*executorService.submit(()->
            	{*/
            		for(Kingdom kingdom : kingdoms)
            		{
                		updateText(true);
                		Map<Integer,List<String>> map=new Hashtable<Integer, List<String>>();
                        List<String> list=new ArrayList<String>();
                        list.add(configService.getProperty("dataDir")+"/"+kingdom);
                        map.put(0, list);
                        kingdomService.createParents(kingdom, map,configService.getProperty("dataDir"),kingdom.getLabel(),0,0);
                        for(int i=map.keySet().size()-1;i>=1;i--)
                        {
                        	for(int j=0;j<map.get(i).size();j++)
                        	{
                        		kingdomService.createParents(kingdom,map,null,null,i+1,j);
                        	}
                        }
                        updateText(false);
            		}
            		
        		//});
            }

            @Override
            public void onFailure(Throwable throwable) {

                if (throwable instanceof CancellationException) {
                    view.updateGlobalProgressionText("Processing interrupted.");
                } else if (throwable instanceof NothingToProcesssException) {
                    view.updateGlobalProgressionText("Nothing to process.");
                } else {
                    view.updateGlobalProgressionText("An error occured.");
                }
                refreshTree();
                progressService.getCurrentProgress().getTotal().set(0);
                progressService.getCurrentProgress().getProgress().set(0);
                //programStatsService.endAcquisitionTimeEstimation();
                view.setGlobalProgressionBar(0);
                view.getExecuteButton().setEnabled(true);
                view.getInterruptButton().setEnabled(false);
                view.getTimeRemainingLabel().setText("");

                if(genesCkb.isSelected()){
                    if (new File(zipGene).exists()) {
                    	view.updateGlobalProgressionText("Zipping in progress.");
                    	ZipUtils zip = new ZipUtils(explodeGenePath[1], explodeGenePath[1] + ".zip");
                        zip.ExecuteZip();
                        view.updateGlobalProgressionText("Zipping finished.");
                    }
                }

                if(genomesCkb.isSelected()){
                	File folderGenome = new File(zipGenome);
                    if (folderGenome.exists()) {
//                    	view.updateGlobalProgressionText("Treatment Genome in progress.");
//                    	ListenableFuture<Boolean> genomeDirectory = zipService.createGenomeDirectory(folderGenome);
//                    	executorService.submit(() -> {
//                    		try{
//                    			genomeDirectory.get();
//                    			view.updateGlobalProgressionText("Treatment Genome Finished.");
//                    			
//                    			view.updateGlobalProgressionText("Zipping in progress.");
//                        		ZipUtils zip = new ZipUtils(explodeGenomePath[1], explodeGenomePath[1] + ".zip");
//                            	view.updateGlobalProgressionText("Zipping in progress.");
//                            	zip.ExecuteZip();
//                            	view.updateGlobalProgressionText("Zipping finished.");
//                    		} catch(Exception e){
//                    			e.printStackTrace();
//                    			view.updateGlobalProgressionText("Something goes wrong.");
//                    		}
//                    	});
//                    	view.updateGlobalProgressionText("Zipping in progress.");
//                    	                    	
                		ZipUtils zip = new ZipUtils(explodeGenomePath[1], explodeGenomePath[1] + ".zip");
                		
                		view.updateGlobalProgressionText("Treatment Genome in progress.");
                    	zip.createGenomeDirectory(new File(zipGenome));
                    	view.updateGlobalProgressionText("Treatment Genome Finished.");
                    	
                    	view.updateGlobalProgressionText("Zipping in progress.");
                    	zip.ExecuteZip();
                    	view.updateGlobalProgressionText("Zipping finished.");
                    
                	}
                }

                resetProgressService();
            }
        }, executorService);
    }
    
    public void updateText(boolean creatingExcelParents)
    {
    	if(creatingExcelParents)
    	{
    		view.updateGlobalProgressionText("Creating excel parents in progress ...");
    	}
    	else
    	{
    		view.updateGlobalProgressionText("excel parents done");
    	}
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
            int hours   = (int) ((programStat.getTimeRemaining() / (1000*60*60)));

            view.getTimeRemainingLabel().setText("ETA: " + hours + "h " + minutes + "min " + seconds + "s");
        }
    }
}
