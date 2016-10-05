package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.inject.Inject;
import com.sun.tools.javac.jvm.Gen;
import models.Gene;
import models.Organism;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;
import services.contracts.GeneService;
import services.contracts.HttpService;
import services.contracts.OrganismService;
import services.contracts.UtilService;

import java.util.*;
import java.util.concurrent.Callable;

public class DefaultOrganismService implements OrganismService {
    private final DeferredManager deferredManager;
    private final GeneService geneService;
    private final HttpService httpService;

    @Inject
    public DefaultOrganismService(DeferredManager deferredManager, GeneService geneService, HttpService httpService) {
        this.deferredManager = deferredManager;
        this.geneService = geneService;
        this.httpService = httpService;
    }

    @Override
    public Promise<Organism, Throwable, Object> createOrganism(final String name, final String group, final String subGroup, final Date updateDate, final String[] geneIds, final String kingdomId) {
        return deferredManager.when(new DeferredCallable<Organism, Object>() {
            @Override
            public Organism call() throws Exception {
                return new Organism(name, group, subGroup, updateDate, geneIds, kingdomId);
            }
        });
    }

    @Override
    public Promise<List<Organism>, Throwable, Object> extractGenes(final List<Organism> organisms) {
        return deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        List<Promise<Organism, Throwable, Object>> promises = new ArrayList<Promise<Organism, Throwable, Object>>();
                        for (Organism organism: organisms) {
                            promises.add(extractGenes(organism));
                        }
                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, List<Organism>, Throwable, Object>() {
                    @Override
                    public Promise<List<Organism>, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        List<Organism> organisms = new ArrayList<Organism>();
                        for (OneResult oneResult: oneResults) {
                            organisms.add((Organism) oneResult.getResult());
                        }
                        return new DeferredObject<List<Organism>, Throwable, Object>().resolve(organisms);
                    }
                }, new FailPipe<OneReject, List<Organism>, Throwable, Object>() {
                    @Override
                    public Promise<List<Organism>, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<List<Organism>, Throwable, Object>().reject((Throwable) oneReject.getReject());
                    }
                });

    }

    @Override
    public Promise<List<Organism>, Throwable, Object> processGenes(final List<Organism> organisms) {
        return deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        List<Promise<List<Gene>, Throwable, Object>> genes = new ArrayList<Promise<List<Gene>, Throwable, Object>>();
                        for (Organism organism: organisms) {
                            if (organism.getGeneIds() != null) {
                                genes.add(geneService.processGenes(Arrays.asList(organism.getGeneIds())));
                            } else {
                                genes.add(new DeferredObject<List<Gene>, Throwable, Object>().resolve(null).promise());
                            }
                        }
                        return deferredManager.when(genes.toArray(new Promise[genes.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, List<Organism>, Throwable, Object>() {
                    @Override
                    public Promise<List<Organism>, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        for (int i = 0; i < organisms.size(); i++) {
                            organisms.get(i).setGenes((ArrayList<Gene>) oneResults.get(i).getResult());
                        }
                        return new DeferredObject<List<Organism>, Throwable, Object>().resolve(organisms).promise();
                    }
                }, new FailPipe<OneReject, List<Organism>, Throwable, Object>() {
                    @Override
                    public Promise<List<Organism>, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<List<Organism>, Throwable, Object>().reject((Throwable) oneReject.getReject()).promise();
                    }
                });
    }

    @Override
    public Promise<Organism, Throwable, Object> extractGenes(final Organism organism) {
        return deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, List<HttpResponse>, Throwable, Object>() {
                    @Override
                    public Promise<List<HttpResponse>, Throwable, Object> pipeDone(Void aVoid) {
                        String[] geneIds = organism.getGeneIds();
                        List<String> urls = new ArrayList<String>();
                        if (geneIds != null && geneIds.length > 0) {
                            for (String geneId: geneIds) {
                                urls.add(geneService.generateUrlForGene(geneId));
                            }
                        }
                        return deferredManager.when(httpService.get(urls));
                    }
                })
                .then(new DonePipe<List<HttpResponse>, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(List<HttpResponse> responses) {
                        List<Promise<Gene, Throwable, Object>> genes = new ArrayList<Promise<Gene, Throwable, Object>>();
                        for (HttpResponse response: responses) {
                            genes.add(geneService.createGene(organism.getName(), 0, 0));
                        }
                        return deferredManager.when(genes.toArray(new Promise[genes.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, Organism, Throwable, Object>() {
                    @Override
                    public Promise<Organism, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        List<Gene> genes = new ArrayList<Gene>();
                        for (int i = 0; i < oneResults.size(); i++) {
                            organism.getGenes().add((Gene) oneResults.get(i).getResult());
                        }
                        return new DeferredObject<Organism, Throwable, Object>().resolve(organism);
                    }
                }, new FailPipe<OneReject, Organism, Throwable, Object>() {
                    @Override
                    public Promise<Organism, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<Organism, Throwable, Object>().reject((Throwable) oneReject.getReject());
                    }
                });
    }
}
