package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.common.base.Function;
import com.google.common.primitives.Booleans;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Kingdom;
import models.Organism;
import services.contracts.*;

import javax.annotation.Nullable;
import java.io.IOException;
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

    private ListenableFuture<List<Boolean>> createDirectories(final Kingdom kingdom, final InputStream inputStream) {
        String dataDir = configService.getProperty("dataDir");
        return Futures.transform(parseService.extractOrganismList(inputStream, kingdom.getId()), new Function<List<Organism>, List<Boolean>>() {
            @Nullable
            @Override
            public List<Boolean> apply(@Nullable List<Organism> organisms) {
                List < String > paths = new ArrayList<>();
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
            }
        }, executorService);
    }

    private ListenableFuture<Kingdom> createKingdomTree(final Kingdom kingdom) {
        ListenableFuture<HttpResponse> responseFuture = retrieveKingdom(kingdom);
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
        ListenableFuture<Kingdom> progressFuture = Futures.transform(createDirectoriesFuture, new Function<List<Boolean>, Kingdom>() {
            @Nullable
            @Override
            public Kingdom apply(@Nullable List<Boolean> booleen) {
                progressService.getCurrentProgress().setStep(TaskProgress.Step.DirectoriesCreationFinished);
                progressService.invalidateProgress();

                TaskProgress progress = progressService.getCurrentProgress();
                progress.setStep(TaskProgress.Step.OrganismProcessing);
                progress.getTotal().addAndGet(kingdom.getOrganisms().size());
                progressService.invalidateProgress();

                return kingdom;
            }
        }, executorService);
        return Futures.transformAsync(progressFuture, kingdom1 -> organismService.processOrganisms(kingdom), executorService);
    }

    public ListenableFuture<List<Kingdom>> createKingdomTrees(final List<Kingdom> kingdoms) {
        List<ListenableFuture<Kingdom>> acquireFutures = new ArrayList<>();
        for (Kingdom kingdom: kingdoms) {
            acquireFutures.add(createKingdomTree(kingdom));
        }
        return Futures.successfulAsList(acquireFutures);
    }
}
