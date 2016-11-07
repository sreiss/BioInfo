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

        List<ListenableFuture<XSSFWorkbook>> processGenesFutures = new ArrayList<>();
        for (Organism organism: organisms) {
            processGenesFutures.add(processGenes(organism));
        }
        ListenableFuture<List<XSSFWorkbook>> organismFutures = Futures.allAsList(processGenesFutures);
        return Futures.transform(organismFutures, new Function<List<XSSFWorkbook>, Kingdom>() {
            @Nullable
            @Override
            public Kingdom apply(@Nullable List<XSSFWorkbook> workbooks) {
                return kingdom;
            }
        }, executorService);
    }

    private ListenableFuture<XSSFWorkbook> processGenes(Organism organism) {
        String[] geneIds = organism.getGeneIds();
        // We do not return the genes in order not to consume too much memory.
        XSSFWorkbook workbook = fileService.createWorkbook();
        ListenableFuture<XSSFWorkbook> genesFuture = geneService.processGenes(organism, workbook, geneIds, organism.getPath());
        return Futures.transform(genesFuture, new Function<XSSFWorkbook, XSSFWorkbook>() {
            @Nullable
            @Override
            public XSSFWorkbook apply(@Nullable XSSFWorkbook computedWorkbook) {
                progressService.getCurrentProgress().getProgress().incrementAndGet();
                progressService.invalidateProgress();
                try {
                    fileService.writeWorkbook(computedWorkbook, organism.getPath(), organism.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return computedWorkbook;
            }
        });
    }
}
