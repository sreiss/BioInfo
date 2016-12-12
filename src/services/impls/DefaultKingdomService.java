package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;

import models.Gene;
import models.Kingdom;
import models.Organism;

import services.contracts.*;
import services.exceptions.NothingToProcesssException;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DefaultKingdomService implements KingdomService {
    private final int PROCESS_STACK_SIZE = 50;
    private final StatisticsService statisticsService;
    private final ParseService parseService;
    private final FileService fileService;
    private final ConfigService configService;
    private final OrganismService organismService;
    private final HttpService httpService;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;
    private final ProgramStatsService programStatsService;
    private final ZipService zipService;
    private HashMap<Kingdom, Map<String, Date>> updates = new HashMap<>();
    private HashMap<Kingdom, ListenableFuture<List<Organism>>> currentFutures = new HashMap<>();
    private boolean shouldInterrupt = false;
    private Boolean genesCkBIsSelected;
    private Boolean genomesCkBIsSelected;

    @Inject
    public DefaultKingdomService(StatisticsService statisticsService,
    							 FileService fileService,
                                 ParseService parseService,
                                 ConfigService configService,
                                 OrganismService organismService,
                                 HttpService httpService,
                                 ListeningExecutorService listeningExecutorService,
                                 ProgressService progressService,
                                 ProgramStatsService programStatsService,
                                 ZipService zipService) {
    	this.statisticsService=statisticsService;
        this.fileService = fileService;
        this.parseService = parseService;
        this.configService = configService;
        this.organismService = organismService;
        this.httpService = httpService;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
        this.programStatsService = programStatsService;
        this.zipService = zipService;

        this.genomesCkBIsSelected = false;
        this.genesCkBIsSelected = false;
    }

    @Override
    public String generateKingdomGeneListUrl(Kingdom kingdom) {
        try {
            URL url;
            String urlString;
            if(Kingdom.Plasmids.equals(kingdom.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=plasmids&king=All&group=All&subgroup=All&format=";
            } else if(Kingdom.Viruses.equals(kingdom.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|%3B&host=All&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
            } else {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|%3B&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--&format=";
            }
            url = new URL(urlString);
            return url.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void interrupt() {
        for (Map.Entry<Kingdom, ListenableFuture<List<Organism>>> currentFuture: currentFutures.entrySet()) {
            currentFuture.getValue().cancel(true);
            currentFuture.getKey().setOrganisms(null);
            shouldInterrupt = true;
            writeUpdateFile(currentFuture.getKey());
        }
    }

    private ListenableFuture<List<Boolean>> createDirectories(final Kingdom kingdom, final InputStream inputStream) {
        String dataDir = configService.getProperty("dataDir");
        String zipGene = configService.getProperty("gene");
        String zipGenome = configService.getProperty("genome");
        Boolean tmpGenesCkBIsSelected = genesCkBIsSelected;
        Boolean tmpGenomesCkBIsSelected = genomesCkBIsSelected;

        return Futures.transform(parseService.extractOrganismList(inputStream, kingdom.getId()), (Function<List<Organism>, List<Boolean>>) organisms -> {
            List < String > paths = new ArrayList<>();
            for (Organism organism : organisms) {
                String path = dataDir
                        + kingdom.getLabel()
                        + "/" + organism.getGroup()
                        + "/" + organism.getSubGroup();
                organism.setPath(path);
                paths.add(path);

                String zipPath = null;

                if(tmpGenesCkBIsSelected){
                    zipPath = zipGene
                            + kingdom.getLabel()
                            + "/" + organism.getGroup()
                            + "/" + organism.getSubGroup()
                            + "/" + organism.getName();
                    paths.add(zipPath);
                }

                if(tmpGenomesCkBIsSelected){
                    zipPath = zipGenome
                            + kingdom.getLabel()
                            + "/" + organism.getGroup()
                            + "/" + organism.getSubGroup()
                            + "/" + organism.getName();
                    paths.add(zipPath);
                }
            }
            kingdom.setOrganisms(organisms);
            return fileService.createDirectories(paths);
        }, executorService);
    }

    private ListenableFuture<Kingdom> loadUpdateFile(Kingdom kingdom) {
        return executorService.submit(() -> {
            try {
                updates.putIfAbsent(kingdom, fileService.readUpdateFile(kingdom));
            } catch (IOException e) {
                e.printStackTrace();
            }
            updates.putIfAbsent(kingdom, new HashMap<>());
            return kingdom;
        });
    }

    /**
     * This is one of the most important methods.
     * It first loads the update file associated to the kingdom (under "./updates/{kingdomLabel}")
     * then it retrieves it from the eutils API, then reads the response, then creates or check if the directories exist,
     * then checks if an update is needed, then notifies the user interface through the progressService of the number of organisms to process,
     * then processes the organisms, and if the later is a success or a failure, writes the current updates file.
     */
    private ListenableFuture<Kingdom> createKingdomTree(Kingdom kingdom, String bioProject) {
        return executorService.submit(() -> {
            shouldInterrupt = false;
            Kingdom modifiedKingdom = loadUpdateFile(kingdom).get();
            String url = generateKingdomGeneListUrl(kingdom);
            HttpResponse httpResponse = httpService.get(url).get();
            InputStream content = httpResponse.getContent();
            List<Boolean> creationResults = createDirectories(kingdom, content).get();

            List<Organism> filteredOrganisms = kingdom.getOrganisms()
                    .stream()
                    .filter(organism -> {
                        if (organism == null) {
                            return false;
                        }
                        Date remoteUpdate = organism.getUpdatedDate();
                        Date localUpdate = updates.get(kingdom).get(organism.getName());
                        // No local update found
                        return (bioProject == null || (organism.getBioProject() != null && bioProject.equals(organism.getBioProject())))
                                && (remoteUpdate == null || localUpdate == null || localUpdate.before(remoteUpdate));
                    })
                    .collect(Collectors.toList());
            kingdom.setOrganisms(filteredOrganisms);

            if (filteredOrganisms.size() == 0) {
                throw new NothingToProcesssException();
            }

            progressService.getCurrentProgress().setStep(TaskProgress.Step.DirectoriesCreationFinished);
            progressService.invalidateProgress();

            TaskProgress progress = progressService.getCurrentProgress();
            progress.setStep(TaskProgress.Step.OrganismProcessing);
            progress.getTotal().addAndGet(kingdom.getOrganisms().size());
            progressService.invalidateProgress();

            programStatsService.setRemainingRequests(programStatsService.getRemainingRequests() + kingdom.getOrganisms().size());


            return processKingdom(modifiedKingdom, 0);
        });
    }

    private Kingdom processKingdom(Kingdom kingdom, int index) throws ExecutionException, InterruptedException {
        if (!shouldInterrupt) {
            int nextIndex = index + PROCESS_STACK_SIZE;
            if (nextIndex > kingdom.getOrganisms().size()) {
                nextIndex = kingdom.getOrganisms().size();
            }
            if (index < kingdom.getOrganisms().size()) {
                List<ListenableFuture<Organism>> organismFutures = new ArrayList<>();
                for (Organism organism : kingdom.getOrganisms().subList(index, nextIndex)) {
                    organismFutures.add(organismService.processOrganism(kingdom, organism));
                }

                ListenableFuture<List<Organism>> currentKingdomFuture = Futures.successfulAsList(organismFutures);
                currentFutures.put(kingdom, currentKingdomFuture);
                List<Organism> organisms = currentKingdomFuture.get();
                for (Organism organism : organisms) {
                    if (organism != null) {
                        updates.get(kingdom).put(organism.getName(), new Date());
                        writeUpdateFile(kingdom);
                    }
                }

                return processKingdom(kingdom, index + PROCESS_STACK_SIZE);
            }
            
            executorService.submit(()->
        	{
        		Map<Integer,AbstractQueue<String>> map=new ConcurrentHashMap<Integer, AbstractQueue<String>>();
                ListenableFuture<Boolean> bottoms=createParents(kingdom, map,configService.getProperty("dataDir"),kingdom.getLabel(),0,0);
        		try
        		{
        			bottoms.get();
				}
        		catch (InterruptedException | ExecutionException e)
        		{
					e.printStackTrace();
				}
        		
        		for(int i=map.size()-1;i>=0;i--)
                {
                	ListenableFuture<List<Boolean>> levels;
                	List<ListenableFuture<Boolean>> listFutures=new ArrayList<ListenableFuture<Boolean>>(map.get(i).size());
                	for(int j=0;j<map.get(i).size();j++)
                	{
                		ListenableFuture<Boolean> levelCurrent=createParents(kingdom,map,null,null,i+1,j);
                		listFutures.add(levelCurrent);
                	}
                	levels=Futures.allAsList(listFutures);
                	
            		try
            		{
            			levels.get();
    				}
            		catch (InterruptedException | ExecutionException e)
            		{
    					e.printStackTrace();
    				}
                }
    		});
        }
        return kingdom;
    }
    
    /**
     * Ne fonctionne pas
     */
    private synchronized void adding(Map<Integer,AbstractQueue<String>> map,int level, File folder)
    {
    	if(map.putIfAbsent(level, new ConcurrentLinkedQueue<String>())!=null)
		{
    		AbstractQueue<String> queue=map.get(level);
			if(!queue.contains(folder.getPath()))
			{
				queue.add(folder.getPath());
			}
		}
    }
    
    private ListenableFuture<Boolean> createParents(Kingdom kingdom, Map<Integer, AbstractQueue<String>> map, String folderPath, String folderName, int level, int max)
    {
    	return executorService.submit(()->{
    		File folder;
        	File[] childrenFolders;
        	
    		boolean good=true;
    		
    		if(level==0)
    		{
    			folder=new File(folderPath+"/"+folderName);
    			childrenFolders=folder.listFiles();
    			for(File childFolder : childrenFolders)
    			{
    				if(childFolder.isDirectory())
    				{
    					good=false;
    					break;
    				}
    			}
    		}
    		else
    		{
    			good=true;
    			folder=new File(map.get(level-1).poll());
    			childrenFolders=folder.listFiles();
    		}
    		
    		if(good)
    		{
    			Organism org=organismService.createOrganism("Total_"+folder.getName(), "", "", "", new Date(), new ArrayList<Tuple<String,String>>(), kingdom.getId());
    			
    			if(level==0)
    			{
    				org.setPath(folderPath);
    			}
    			else
    			{
    				org.setPath(folder.getParent());
    			}
    			
    			Map<String,Gene> mapGene=new Hashtable<String,Gene>();
    			for(File excel : childrenFolders)
    			{
    				if(excel.isFile()) // only necessary on level >0
    				{
    					mapGene=fileService.readWorkbooks(mapGene,excel);
    					for(String key : mapGene.keySet())
    					{
    						try
    						{
    							statisticsService.computeStatistics(kingdom, org, mapGene.get(key)).get();
    						}
    						catch (InterruptedException e)
    						{
    							e.printStackTrace();
    						} catch (ExecutionException e)
    						{
    							e.printStackTrace();
    						}
    					}
    				}
    			}
    			organismService.processOrganismWithoutGene(mapGene,kingdom, org).get();
    		}
    		else
    		{
    			List<ListenableFuture<Boolean>> list=new ArrayList<ListenableFuture<Boolean>>();
    			for(File childFolder : childrenFolders)
    			{
    				if(childFolder.isDirectory())
    				{
    					if(level==0)
    					{
    						adding(map,max,folder);
    						list.add(createParents(kingdom,map,folder.getPath(),childFolder.getName(),0,max+1));
    					}
    					else
    					{
    						list.add(createParents(kingdom,map,folder.getPath(),childFolder.getName(),level,max+1));
    					}
    				}
    			}
    			try{
    				Futures.allAsList(list).get();
    			}
    			catch(Exception e)
    			{
    				e.printStackTrace();
    			}
    		}
    		
    		return true;
    	});
    	
    }

    private void writeUpdateFile(Kingdom kingdom) {
        try {
            fileService.writeUpdateFile(kingdom, updates.get(kingdom));
        } catch (IOException e) {
            System.err.println("Unable to write update file for kingdom " + kingdom);
        }
    }

    /**
     * Creates the file trees for the given kingdoms and starts the time estimation.
     * The remaining time is estimated over the 100 last processings.
     */
    public ListenableFuture<List<Kingdom>> createKingdomTrees(final List<Kingdom> kingdoms, String bioProject) {
        programStatsService.resetAcquisitionTime();
        //programStatsService.beginAcquisitionTimeEstimation();
        List<ListenableFuture<Kingdom>> acquireFutures = new ArrayList<>();
        for (Kingdom kingdom: kingdoms) {
            acquireFutures.add(createKingdomTree(kingdom, bioProject));
        }
        return Futures.transformAsync(Futures.successfulAsList(acquireFutures), processedKingdoms -> {
            //programStatsService.endAcquisitionTimeEstimation();
            // We check if something was processed.
            if (processedKingdoms == null) {
                throw new NothingToProcesssException();
            }
            List<Kingdom> succefullyProcessedKingdoms = kingdoms.stream()
                    .filter(kingdom -> kingdom.getOrganisms().size() > 0)
                    .collect(Collectors.toList());
            if (succefullyProcessedKingdoms.size() == 0) {
                throw new NothingToProcesssException();
            }
            return Futures.immediateFuture(kingdoms);
        }, executorService);
    }

    @Override
    public boolean getShouldInterrupt() {
        return shouldInterrupt;
    }

    public void setGenomesCkBIsSelected(Boolean isSelected){
        this.genomesCkBIsSelected = isSelected;
    }

    public Boolean getGenomesCkBIsSelected(){
        return this.genomesCkBIsSelected;
    }

    public void setGenesCkBIsSelected(Boolean isSelected){
        this.genesCkBIsSelected = isSelected;
    }

    public Boolean getGenesCkBIsSelected(){
        return this.genesCkBIsSelected;
    }
}
