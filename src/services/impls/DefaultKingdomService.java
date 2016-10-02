package services.impls;

import com.google.inject.Inject;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
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
                        deferred.notify(new TaskProgress(0, TaskProgress.Type.Text, TaskProgress.Step.DirectoriesCreationFinished));
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
