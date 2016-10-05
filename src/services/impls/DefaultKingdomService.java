package services.impls;

import com.google.inject.Inject;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.*;
import services.contracts.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DefaultKingdomService implements KingdomService {
    private final ParseService parseService;
    private final FileService fileService;
    private final DeferredManager deferredManager;
    private final ConfigService configService;
    private final OrganismService organismService;

    @Inject
    public DefaultKingdomService(FileService fileService, DeferredManager deferredManager, ParseService parseService, ConfigService configService, OrganismService organismService) {
        this.fileService = fileService;
        this.deferredManager = deferredManager;
        this.parseService = parseService;
        this.configService = configService;
        this.organismService = organismService;
    }

    @Override
    public String generateKingdomGeneListUrl(Kingdom kingdom) {
        return "https://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
    }

    @Override
    public Promise<List<Kingdom>, Throwable, Object> createKingdomTrees(final List<Kingdom> kingdoms, final List<InputStream> inputStreams) {
        final DeferredObject<List<Kingdom>, Throwable, Object> deferred = new DeferredObject<List<Kingdom>, Throwable, Object>();
        deferred.notify(new TaskProgress(0, TaskProgress.Type.Text, TaskProgress.Step.KingdomsCreation));
        deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        if (kingdoms.size() != inputStreams.size()) {
                            DeferredObject<MultipleResults, OneReject, MasterProgress> deferred = new DeferredObject<MultipleResults, OneReject, MasterProgress>();
                            deferred.reject(new OneReject(0, deferred.promise(), new IllegalArgumentException("kingdoms must be the same size as inputStreams.")));
                            return deferred.promise();
                        }

                        List<Promise<Kingdom, Throwable, Object>> promises = new ArrayList<Promise<Kingdom, Throwable, Object>>();
                        for(int i = 0; i < kingdoms.size(); i++) {
                            promises.add(createKingdomTree(kingdoms.get(i), inputStreams.get(i)));
                        }

                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
                    }
                })
                .then(new DoneCallback<MultipleResults>() {
                    @Override
                    public void onDone(MultipleResults oneResults) {
                        synchronized (this) {
                            List<Kingdom> kingdoms = new ArrayList<Kingdom>();
                            for (OneResult oneResult : oneResults) {
                                kingdoms.add((Kingdom) oneResult.getResult());
                            }
                            deferred.notify(new TaskProgress(0, TaskProgress.Type.Text, TaskProgress.Step.DirectoriesCreationFinished));
                            deferred.resolve(kingdoms);
                        }
                    }
                })
                .fail(new FailCallback<OneReject>() {
                    @Override
                    public void onFail(OneReject oneReject) {
                        deferred.reject((Throwable) oneReject.getReject());
                    }
                });

        return deferred;
    }

    @Override
    public Promise<List<Kingdom>, Throwable, Object> processGenes(final List<Kingdom> kingdoms) {
        return deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        List<Promise<Kingdom, Throwable, Object>> promises = new ArrayList<Promise<Kingdom, Throwable, Object>>();
                        for (Kingdom kingdom: kingdoms) {
                            promises.add(processGenes(kingdom));
                        }
                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, List<Kingdom>, Throwable, Object>() {
                    @Override
                    public Promise<List<Kingdom>, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        return new DeferredObject<List<Kingdom>, Throwable, Object>()
                                .resolve(kingdoms)
                                .promise();
                    }
                }, new FailPipe<OneReject, List<Kingdom>, Throwable, Object>() {
                    @Override
                    public Promise<List<Kingdom>, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<List<Kingdom>, Throwable, Object>().reject((Throwable) oneReject.getReject()).promise();
                    }
                });
    }

    @Override
    public Promise<Kingdom, Throwable, Object> processGenes(final Kingdom kingdom) {
        return organismService.processGenes(kingdom.getOrganisms())
                .then(new DonePipe<List<Organism>, Kingdom, Throwable, Object>() {
                    @Override
                    public Promise<Kingdom, Throwable, Object> pipeDone(List<Organism> organisms) {
                        kingdom.setOrganisms(organisms);
                        return new DeferredObject<Kingdom, Throwable, Object>().resolve(kingdom).promise();
                    }
                });

    }

    @Override
    public Promise<Kingdom, Throwable, Object> createKingdomTree(final Kingdom kingdom, final InputStream inputStream) {
        return deferredManager.when(configService.getProperty("dataDir"), parseService.extractOrganismList(inputStream, kingdom.getId()))
                .then(new DonePipe<MultipleResults, List<Boolean>, Throwable, Object>() {
                    @Override
                    public Promise<List<Boolean>, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        String dataDir = (String) oneResults.get(0).getResult();
                        List<Organism> organisms = (List<Organism>) oneResults.get(1).getResult();

                        List<String> paths = new ArrayList<String>();
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
                }, new FailPipe<OneReject, List<Boolean>, Throwable, Object>() {
                    @Override
                    public Promise<List<Boolean>, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<List<Boolean>, Throwable, Object>().reject((Throwable) oneReject.getReject());
                    }
                })
                .then(new DonePipe<List<Boolean>, Kingdom, Throwable, Object>() {
                    @Override
                    public Promise<Kingdom, Throwable, Object> pipeDone(List<Boolean> booleen) {
                        DeferredObject<Kingdom, Throwable, Object> deferred = new DeferredObject<Kingdom, Throwable, Object>();
                        deferred.resolve(kingdom);
                        return deferred.promise();
                    }
                });

//                    @Override
//                    public Promise<List<Boolean>, Throwable, Object> pipeDone(List<Organism> organisms) {
//                        List<String> paths = new ArrayList<String>();
//                        for (Organism organism: organisms) {
//                            paths.add(
//                                    + "/" + kingdom.getLabel()
//                                    + "/" + organism.getGroup()
//                                    + "/" + organism.getSubGroup()
//                                    + "/" + organism.getName());
//                        }
                /*
                String line = bufferedReader.readLine();
                while ((line = bufferedReader.readLine()) != null)
                {
                    organism = new Organism(line, kingdoId);
                    addr = "Download/" + kingdom.getLabel()
                            + "/" + organism.group + "/"
                            + organism.subGroup + "/" + organism.name;

                    (new File(addr)).mkdirs();

                    if(organism.id != null)
                    {
                        for (String ids : organism.id)
                        {
                            res.add(new Ids(ids, organism.name, organism.group, organism.subGroup, organism.updatedDate, kingdom));
                        }
                    }
                }
                */
    }
}
