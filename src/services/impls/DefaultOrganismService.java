package services.impls;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class DefaultOrganismService implements OrganismService {
    private final GeneService geneService;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;
    private final FileService fileService;

    @Inject
    public DefaultOrganismService(GeneService geneService, ListeningExecutorService listeningExecutorService, ProgressService progressService, FileService fileService) {
        this.geneService = geneService;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
        this.fileService = fileService;
    }

    @Override
    public Organism createOrganism(final String name, final String group, final String subGroup, final Date updateDate, final String[] geneIds, final String kingdomId) {
        return new Organism(name, group, subGroup, updateDate, geneIds, kingdomId);
    }

    @Override
    public ListenableFuture<Kingdom> processOrganisms(Kingdom kingdom) {
        List<Organism> organisms = kingdom.getOrganisms();

        TaskProgress progress = progressService.getCurrentProgress();
        progress.setStep(TaskProgress.Step.OrganismProcessing);
        progress.getTotal().addAndGet(organisms.size());
        progressService.invalidateProgress();

        List<ListenableFuture<Organism>> processGenesFutures = new ArrayList<>();
        for (Organism organism: organisms) {
            processGenesFutures.add(processGenes(organism));
        }
        ListenableFuture<List<Organism>> organismFutures = Futures.allAsList(processGenesFutures);
        return Futures.transformAsync(organismFutures, processedOrganisms -> {
            kingdom.setOrganisms(processedOrganisms);
            return returnKingdom(kingdom);
        }, executorService);
    }

    private ListenableFuture<Kingdom> returnKingdom(Kingdom kingdom) {
        return executorService.submit(() -> kingdom);
    }

    private ListenableFuture<Organism> processGenes(Organism organism) {
        String[] geneIds = organism.getGeneIds();
        // We do not return the genes in order not to consume too much memory.
        XSSFWorkbook workbook = fileService.createWorkbook();
        ListenableFuture<XSSFWorkbook> genesFuture = geneService.processGenes(organism, workbook, geneIds, organism.getPath());
        return Futures.transform(genesFuture, new Function<XSSFWorkbook, Organism>() {
            @Nullable
            @Override
            public Organism apply(@Nullable XSSFWorkbook computedWorkbook) {
                try {
                    progressService.getCurrentProgress().getProgress().incrementAndGet();
                    progressService.invalidateProgress();
                    fileService.writeWorkbook(computedWorkbook, organism.getPath(), organism.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return organism;
            }
        });
    }

//                })
//                .then((DonePipe<List<HttpResponse>, MultipleResults, OneReject, MasterProgress>) responses -> {
//                    List<Promise<Gene, Throwable, Object>> genes = new ArrayList<>();
//                    for (HttpResponse response: responses) {
//                        genes.add(geneService.createGene(organism.getName(), 0, 0));
//                    }
//                    return deferredManager.when(genes.toArray(new Promise[genes.size()]));
//                })
//                .then(oneResults -> {
//                    List<Gene> genes = new ArrayList<Gene>();
//                    for (int i = 0; i < oneResults.size(); i++) {
//                        organism.getGenes().add((Gene) oneResults.get(i).getResult());
//                    }
//                    return new DeferredObject<Organism, Throwable, Object>().resolve(organism);
//                }, (FailPipe<OneReject, Organism, Throwable, Object>) oneReject -> {
//                    return new DeferredObject<Organism, Throwable, Object>().reject((Throwable) oneReject.getReject());
//                });

//
//    @Override
//    public ListenableFuture<List<Organism>> extractGenes(final List<Organism> organisms) {
//        return deferredManager.when(new UtilService.VoidCallable())
//                .then((DonePipe<Void, MultipleResults, OneReject, MasterProgress>) aVoid -> {
//                    List<Promise<Organism, Throwable, Object>> promises = new ArrayList<>();
//                    for (Organism organism: organisms) {
//                        promises.add(extractGenes(organism));
//                    }
//                    return deferredManager.when(promises.toArray(new Promise[promises.size()]));
//                })
//                .then((DonePipe<MultipleResults, List<Organism>, Throwable, Object>) oneResults -> {
//                    List<Organism> organisms1 = new ArrayList<Organism>();
//                    for (OneResult oneResult: oneResults) {
//                        organisms1.add((Organism) oneResult.getResult());
//                    }
//                    return new DeferredObject<List<Organism>, Throwable, Object>().resolve(organisms1);
//                }, oneReject -> {
//                    return new DeferredObject<List<Organism>, Throwable, Object>().reject((Throwable) oneReject.getReject());
//                });
//
//    }
//
//    @Override
//    public ListenableFuture<List<Organism>> processGenes(final List<Organism> organisms) {
//        return deferredManager.when(new UtilService.VoidCallable())
//                .then((DonePipe<Void, MultipleResults, OneReject, MasterProgress>) aVoid -> {
//                    List<Promise<List<Gene>, Throwable, Object>> genes = new ArrayList<Promise<List<Gene>, Throwable, Object>>();
//                    for (Organism organism: organisms) {
//                        if (organism.getGeneIds() != null) {
//                            genes.add(geneService.processGenes(Arrays.asList(organism.getGeneIds())));
//                        } else {
//                            genes.add(new DeferredObject<List<Gene>, Throwable, Object>().resolve(null).promise());
//                        }
//                    }
//                    return deferredManager.when(genes.toArray(new Promise[genes.size()]));
//                })
//                .then((DonePipe<MultipleResults, List<Organism>, Throwable, Object>) oneResults -> {
//                    for (int i = 0; i < organisms.size(); i++) {
//                        organisms.get(i).setGenes((ArrayList<Gene>) oneResults.get(i).getResult());
//                    }
//                    return new DeferredObject<List<Organism>, Throwable, Object>().resolve(organisms).promise();
//                }, oneReject -> {
//                    return new DeferredObject<List<Organism>, Throwable, Object>().reject((Throwable) oneReject.getReject()).promise();
//                });
//    }
//
//    @Override
//    public ListenableFuture<Organism> extractGenes(final Organism organism) {
//        return deferredManager.when(new UtilService.VoidCallable())
//                .then((DonePipe<Void, List<HttpResponse>, Throwable, Object>) aVoid -> {
//                    String[] geneIds = organism.getGeneIds();
//                    List<String> urls = new ArrayList<String>();
//                    if (geneIds != null && geneIds.length > 0) {
//                        for (String geneId: geneIds) {
//                            urls.add(geneService.generateUrlForGene(geneId));
//                        }
//                    }
//                    return deferredManager.when(httpService.get(urls));
//                })
//                .then((DonePipe<List<HttpResponse>, MultipleResults, OneReject, MasterProgress>) responses -> {
//                    List<Promise<Gene, Throwable, Object>> genes = new ArrayList<>();
//                    for (HttpResponse response: responses) {
//                        genes.add(geneService.createGene(organism.getName(), 0, 0));
//                    }
//                    return deferredManager.when(genes.toArray(new Promise[genes.size()]));
//                })
//                .then(oneResults -> {
//                    List<Gene> genes = new ArrayList<Gene>();
//                    for (int i = 0; i < oneResults.size(); i++) {
//                        organism.getGenes().add((Gene) oneResults.get(i).getResult());
//                    }
//                    return new DeferredObject<Organism, Throwable, Object>().resolve(organism);
//                }, (FailPipe<OneReject, Organism, Throwable, Object>) oneReject -> {
//                    return new DeferredObject<Organism, Throwable, Object>().reject((Throwable) oneReject.getReject());
//                });
//    }
}
