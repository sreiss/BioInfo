package services.impls;

import com.google.inject.Inject;
import models.Kingdom;
import models.Organism;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MultipleResults;
import services.contracts.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultKingdomService implements KingdomService {
    private final ParseService parseService;
    private final FileService fileService;
    private final DeferredManager deferredManager;
    private final ConfigService configService;

    @Inject
    public DefaultKingdomService(FileService fileService, DeferredManager deferredManager, ParseService parseService, ConfigService configService) {
        this.fileService = fileService;
        this.deferredManager = deferredManager;
        this.parseService = parseService;
        this.configService = configService;
    }

    @Override
    public Promise<List<Organism>, Throwable, Object> createKingdomTree(final Kingdom kingdom, final InputStream inputStream) {
        final Organism[][] organisms = new Organism[1][1];
        return deferredManager.when(configService.getProperty("dataDir"), parseService.extractOrganismList(inputStream, kingdom.getId()))
                .then(new DonePipe<MultipleResults, List<Boolean>, Throwable, Object>() {
                    @Override
                    public Promise<List<Boolean>, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        String dataDir = (String) oneResults.get(0).getResult();
                        List<Organism> organismList = (List<Organism>) oneResults.get(1).getResult();
                        organisms[0] = organismList.toArray(new Organism[organismList.size()]);

                        List<String> paths = new ArrayList<String>();
                        for (Organism organism: organisms[0]) {
                            String path = dataDir
                                    + "/" + kingdom.getLabel()
                                    + "/" + organism.getGroup()
                                    + "/" + organism.getSubGroup()
                                    + "/" + organism.getName();
                            organism.setPath(path);
                            paths.add(path);
                        }
                        return fileService.createDirectories(paths);
                    }
                })
                .then(new DonePipe<List<Boolean>, List<Organism>, Throwable, Object>() {
                    @Override
                    public Promise<List<Organism>, Throwable, Object> pipeDone(List<Boolean> booleen) {
                        return new DeferredObject<List<Organism>, Throwable, Object>().resolve(Arrays.asList(organisms[0]));
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
