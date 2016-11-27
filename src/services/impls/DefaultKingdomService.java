package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.common.base.Function;
import com.google.common.primitives.Booleans;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Kingdom;
import models.Organism;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import services.contracts.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DefaultKingdomService implements KingdomService {
    private final ParseService parseService;
    private final FileService fileService;
    private final ConfigService configService;
    private final OrganismService organismService;
    private final HttpService httpService;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;
    private final ProgramStatsService programStatsService;
    private HashMap<Kingdom, Map<String, Date>> updates = new HashMap<>();

    @Inject
    public DefaultKingdomService(FileService fileService,
                                 ParseService parseService,
                                 ConfigService configService,
                                 OrganismService organismService,
                                 HttpService httpService,
                                 ListeningExecutorService listeningExecutorService,
                                 ProgressService progressService,
                                 ProgramStatsService programStatsService) {
        this.fileService = fileService;
        this.parseService = parseService;
        this.configService = configService;
        this.organismService = organismService;
        this.httpService = httpService;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
        this.programStatsService = programStatsService;
    }

    @Override
    public String generateKingdomGeneListUrl(Kingdom kingdom) {
        try {
            URL url;
            String urlString;
            String host = (kingdom.equals(Kingdom.Viruses) ? "%3B&host=All" : "");
            if(kingdom.getId().equals("plasmids"))
            {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=plasmids&king=All&group=All&subgroup=All&format=";
            }
            else
            {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|" + host + "&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";

            }
            url = new URL(urlString);
            return url.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ListenableFuture<HttpResponse> retrieveKingdom(Kingdom kingdom) {
        String url = generateKingdomGeneListUrl(kingdom);
        return httpService.get(url);
    }

    private ListenableFuture<List<Boolean>> createDirectories(final Kingdom kingdom, final InputStream inputStream) {
        String dataDir = configService.getProperty("dataDir");
        return Futures.transform(parseService.extractOrganismList(inputStream, kingdom.getId()), new Function<List<Organism>, List<Boolean>>() {
            @Override
            public List<Boolean> apply(List<Organism> organisms) {
                List < String > paths = new ArrayList<>();
                for (Organism organism : organisms) {
                    String path = dataDir
                            + kingdom.getLabel()
                            + "/" + organism.getGroup()
                            + "/" + organism.getSubGroup();
                    organism.setPath(path);
                    paths.add(path);
                }
                kingdom.setOrganisms(organisms);
                return fileService.createDirectories(paths);
            }
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
    private ListenableFuture<Kingdom> createKingdomTree(final Kingdom kingdom) {
        ListenableFuture<Kingdom> loadUpdateFileFuture = loadUpdateFile(kingdom);
        ListenableFuture<HttpResponse> responseFuture = Futures.transformAsync(loadUpdateFileFuture, this::retrieveKingdom, executorService);
        ListenableFuture<InputStream> inputStreamFuture = Futures.transform(responseFuture, new Function<HttpResponse, InputStream>() {
            @Nullable
            @Override
            public InputStream apply(@Nullable HttpResponse httpResponse) {
                try {
                    return httpResponse.getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        ListenableFuture<List<Boolean>> createDirectoriesFuture = Futures.transformAsync(inputStreamFuture, inputStream -> createDirectories(kingdom, inputStream), executorService);
        ListenableFuture<List<Organism>> checkIfNeedsUpdateFuture = Futures.transform(createDirectoriesFuture, new Function<List<Boolean>, List<Organism>>() {
            @Nullable
            @Override
            public List<Organism> apply(@Nullable List<Boolean> booleans) {
                // Here we check if the organism has been updated since the last program run.
                kingdom.setOrganisms(kingdom.getOrganisms().stream().filter(organism -> {
                    Date remoteUpdate = organism.getUpdatedDate();
                    Date localUpdate = updates.get(kingdom).get(organism.getName());
                    // No local update found
                    return remoteUpdate == null || localUpdate == null || localUpdate.before(remoteUpdate);
                }).collect(Collectors.toList()));
                return kingdom.getOrganisms();
            }
        }, executorService);
        ListenableFuture<Kingdom> progressFuture = Futures.transform(checkIfNeedsUpdateFuture, new Function<List<Organism>, Kingdom>() {
            @Nullable
            @Override
            public Kingdom apply(@Nullable List<Organism> organisms) {
                progressService.getCurrentProgress().setStep(TaskProgress.Step.DirectoriesCreationFinished);
                progressService.invalidateProgress();

                TaskProgress progress = progressService.getCurrentProgress();
                progress.setStep(TaskProgress.Step.OrganismProcessing);
                progress.getTotal().addAndGet(kingdom.getOrganisms().size());
                progressService.invalidateProgress();

                programStatsService.setRemainingRequests(programStatsService.getRemainingRequests() + kingdom.getOrganisms().size());

                return kingdom;
            }
        }, executorService);
        ListenableFuture<Kingdom> kingdomProcessedFuture = Futures.transformAsync(progressFuture, kingdom1 -> organismService.processOrganisms(kingdom, updates.getOrDefault(kingdom, new HashMap<>())), executorService);
        Futures.addCallback(kingdomProcessedFuture, new FutureCallback<Kingdom>() {
            @Override
            public void onSuccess(@Nullable Kingdom kingdom) {
                writeUpdateFile(kingdom);
            }

            @Override
            public void onFailure(Throwable throwable) {
                writeUpdateFile(kingdom);
            }
        }, executorService);
        return kingdomProcessedFuture;
    }

    private void writeUpdateFile(Kingdom kingdom) {
        try {
            fileService.writeUpdateFile(kingdom, updates.get(kingdom));
        } catch (IOException e) {
            System.err.println("Unable to write update file for kingdom " + kingdom);
        }
    }

    public ListenableFuture<List<Kingdom>> createKingdomTrees(final List<Kingdom> kingdoms) {
        programStatsService.resetAcquisitionTime();
        programStatsService.beginAcquisitionTimeEstimation();
        List<ListenableFuture<Kingdom>> acquireFutures = new ArrayList<>();
        for (Kingdom kingdom: kingdoms) {
            acquireFutures.add(createKingdomTree(kingdom));
        }
        return Futures.transform(Futures.successfulAsList(acquireFutures), new Function<List<Kingdom>, List<Kingdom>>() {
            @Nullable
            @Override
            public List<Kingdom> apply(@Nullable List<Kingdom> kingdoms) {
                programStatsService.endAcquisitionTimeEstimation();
                return kingdoms;
            }
        });
    }
}
