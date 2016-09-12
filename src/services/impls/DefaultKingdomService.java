package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.inject.Inject;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xpath.internal.operations.Or;
import models.Kingdom;
import models.Organism;
import org.jdeferred.*;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import services.contracts.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

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
    public Promise<Void, Throwable, Void> createKingdomTree(final Kingdom kingdom, final HttpResponse response) {
        return deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, List<Organism>, Throwable, Void>() {
                    @Override
                    public Promise<List<Organism>, Throwable, Void> pipeDone(Void aVoid) {
                        try {
                            return parseService.extractOrganismList(response.getContent(), kingdom.getId());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .then(new DonePipe<List<Organism>, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(List<Organism> organisms) {
                        List<String> paths = new ArrayList<String>();
                        for (Organism organism: organisms) {
                            paths.add(configService.getProperty("dataDir")
                                    + "/" + kingdom.getLabel()
                                    + "/" + organism.getGroup()
                                    + "/" + organism.getSubGroup()
                                    + "/" + organism.getName());
                        }
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

                        return fileService.createDirectories(paths);
                    }
                }).then(new DonePipe<MultipleResults, Void, Throwable, Void>() {
                    @Override
                    public Promise<Void, Throwable, Void> pipeDone(MultipleResults oneResults) {
                        return null;
                    }
                });
    }
}
