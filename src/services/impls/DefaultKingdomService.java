package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Kingdom;
import models.Organism;
import services.contracts.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DefaultKingdomService implements KingdomService {
    private final ParseService parseService;
    private final FileService fileService;
    private final ConfigService configService;
    private final OrganismService organismService;
    private final HttpService httpService;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;

    @Inject
    public DefaultKingdomService(FileService fileService, ParseService parseService, ConfigService configService, OrganismService organismService, HttpService httpService, ListeningExecutorService listeningExecutorService, ProgressService progressService) {
        this.fileService = fileService;
        this.parseService = parseService;
        this.configService = configService;
        this.organismService = organismService;
        this.httpService = httpService;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
    }

    @Override
    public String generateKingdomGeneListUrl(Kingdom kingdom) {
        return "https://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
    }

    private ListenableFuture<HttpResponse> retrieveKingdom(Kingdom kingdom) {
        String url = generateKingdomGeneListUrl(kingdom);
        return httpService.get(url);
    }

    private ListenableFuture<InputStream> getInputStream(HttpResponse response) {
        return executorService.submit(response::getContent);
    }

    private ListenableFuture<List<Boolean>> createDirectories(final Kingdom kingdom, final InputStream inputStream) {
        String dataDir = configService.getProperty("dataDir");
        return Futures.transformAsync(parseService.extractOrganismList(inputStream, kingdom.getId()), organisms -> {
            List<String> paths = new ArrayList<>();
            for (Organism organism : organisms) {
                String path = dataDir
                        + kingdom.getLabel()
                        + "/" + organism.getGroup()
                        + "/" + organism.getSubGroup()
                        + "/" + organism.getName();
                organism.setPath(path);
                paths.add(path);
            }
            kingdom.setOrganisms(organisms);
            return fileService.createDirectories(paths);
        }, executorService);

    }

    private ListenableFuture<Kingdom> returnKingdom(Kingdom kingdom) {
        return executorService.submit(() -> kingdom);
    }

    private ListenableFuture<Kingdom> createKingdomTree(final Kingdom kingdom) {
        ListenableFuture<HttpResponse> responseFuture = retrieveKingdom(kingdom);
        ListenableFuture<InputStream> getInputStreamFuture = Futures.transformAsync(responseFuture, this::getInputStream, executorService);
        ListenableFuture<List<Boolean>> createDirectoriesFuture = Futures.transformAsync(getInputStreamFuture, inputStream -> createDirectories(kingdom, inputStream), executorService);
        ListenableFuture<Kingdom> processOrganismsFuture = Futures.transformAsync(createDirectoriesFuture, createdDirectories -> {
            progressService.getCurrentProgress().setStep(TaskProgress.Step.DirectoriesCreationFinished);
            progressService.invalidateProgress();
            return organismService.processOrganisms(kingdom);
        }, executorService);
        return Futures.transformAsync(processOrganismsFuture, this::returnKingdom, executorService);
    }

    public ListenableFuture<List<Kingdom>> createKingdomTrees(final List<Kingdom> kingdoms) {
        List<ListenableFuture<Kingdom>> acquireFutures = new ArrayList<>();
        for (Kingdom kingdom: kingdoms) {
            acquireFutures.add(createKingdomTree(kingdom));
        }
        return Futures.allAsList(acquireFutures);
    }
}
