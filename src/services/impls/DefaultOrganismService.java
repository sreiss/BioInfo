package services.impls;

import com.google.api.client.xml.atom.Atom;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultOrganismService implements OrganismService {
    private final int PROCESS_STACK_SIZE = 40;
    private final GeneService geneService;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;
    private final FileService fileService;
    private final ProgramStatsService programStatsService;
    private final StatisticsService statisticsService;
    private HashMap<Kingdom, AtomicInteger> kingdomOffsets = new HashMap<Kingdom, AtomicInteger>();

    @Inject
    public DefaultOrganismService(GeneService geneService, ListeningExecutorService listeningExecutorService, ProgressService progressService, FileService fileService, ProgramStatsService programStatsService, StatisticsService statisticsService) {
        this.geneService = geneService;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
        this.fileService = fileService;
        this.programStatsService = programStatsService;
        this.statisticsService = statisticsService;
    }

    @Override
    public Organism createOrganism(final String name, final String group, final String subGroup, final Date updateDate, final List<Tuple<String, String>> geneIds, final String kingdomId) {
        return new Organism(name, group, subGroup, updateDate, geneIds, kingdomId);
    }

    @Override
    public ListenableFuture<Kingdom> processOrganisms(Kingdom kingdom) {
        List<Organism> organisms = kingdom.getOrganisms();

        List<ListenableFuture<XSSFWorkbook>> processGenesFutures = new ArrayList<>();

        kingdomOffsets.putIfAbsent(kingdom, new AtomicInteger());
        AtomicInteger currentOffset = kingdomOffsets.get(kingdom);

        int startIndex = currentOffset.get();
        int endIndex;
        boolean isLastLoop;
        if ((startIndex + PROCESS_STACK_SIZE) < (organisms.size() - 1)) {
            endIndex = (startIndex + PROCESS_STACK_SIZE);
            isLastLoop = false;
        } else {
            endIndex = (organisms.size() - 1);
            isLastLoop = true;
        }


        for (Organism organism: organisms.subList(startIndex, endIndex)) {
            ListenableFuture<XSSFWorkbook> processGenesFuture = processGenes(organism);
            ListenableFuture<XSSFWorkbook> processOrganismFuture = Futures.transformAsync(processGenesFuture, workbook -> processOrganism(organism, workbook), executorService);
            processGenesFutures.add(processOrganismFuture);
        }
        ListenableFuture<List<XSSFWorkbook>> organismFutures = Futures.allAsList(processGenesFutures);
        ListenableFuture<Kingdom> writeFuture = Futures.transform(organismFutures, new Function<List<XSSFWorkbook>, Kingdom>() {
                @Nullable
                @Override
                public Kingdom apply(@Nullable List<XSSFWorkbook> workbooks) {
                    if (workbooks != null) {
                        for (int i = 0; i < workbooks.size(); i++) {
                            Organism organism = organisms.get(i);
                            try {
                                fileService.writeWorkbook(workbooks.get(i), organism.getPath(), organism.getName());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return kingdom;
                }
            }, executorService);

        if (isLastLoop) {
            return writeFuture;
        } else {
            currentOffset.set(currentOffset.get() + PROCESS_STACK_SIZE);
            return Futures.transformAsync(writeFuture, kingdom1 -> processOrganisms(kingdom), executorService);
        }
    }

    private ListenableFuture<XSSFWorkbook> processOrganism(Organism organism, XSSFWorkbook workbook) {
        return statisticsService.computeSum(organism, workbook);
    }

    private ListenableFuture<XSSFWorkbook> processGenes(Organism organism) {
        List<Tuple<String, String>> geneIds = organism.getGeneIds();
        XSSFWorkbook workbook = fileService.createWorkbook();
        return Futures.transform(geneService.processGenes(organism, workbook, geneIds, organism.getPath()), new Function<XSSFWorkbook, XSSFWorkbook>() {
            @Nullable
            @Override
            public XSSFWorkbook apply(@Nullable XSSFWorkbook workbook) {
                programStatsService.addDate(ZonedDateTime.now());
                programStatsService.setRemainingRequests(programStatsService.getRemainingRequests());
                progressService.getCurrentProgress().getProgress().incrementAndGet();
                progressService.invalidateProgress();
                return workbook;
            }
        });
    }
}
