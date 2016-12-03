package services.impls;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Kingdom;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultOrganismService implements OrganismService {
    private final int PROCESS_STACK_SIZE = 10;
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
    public DateFormat getUpdateDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * This method processes the organisms of the given kingdom.
     * The second argument are the updates of the kingdom. The key is the organism and the value, the date.
     */
    @Override
    public ListenableFuture<Kingdom> processOrganisms(Kingdom kingdom, Map<String, Date> updates) {
        List<Organism> organisms = kingdom.getOrganisms();

        List<ListenableFuture<XSSFWorkbook>> processGenesFutures = new ArrayList<>();

        kingdomOffsets.putIfAbsent(kingdom, new AtomicInteger());
        AtomicInteger currentOffset = kingdomOffsets.get(kingdom);

        // This part is to avoid a too high pressure on the memory.
        // The number of simultaneously processed organisms is determined by PROCESS_STACK_SIZE.
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
            processGenesFutures.add(processGenesFuture);
        }
        ListenableFuture<List<XSSFWorkbook>> organismFutures = Futures.successfulAsList(processGenesFutures);
        ListenableFuture<Kingdom> writeFuture = Futures.transform(organismFutures, new Function<List<XSSFWorkbook>, Kingdom>() {
            @Nullable
            @Override
            public Kingdom apply(@Nullable List<XSSFWorkbook> workbooks) {
                if (workbooks != null) {
                    for (int i = 0; i < workbooks.size(); i++) {
                        try {
                            int offset = Math.abs(currentOffset.get() - PROCESS_STACK_SIZE) + i;
                            Organism organism = organisms.get(offset);
                            fileService.writeWorkbook(workbooks.get(i), organism.getPath(), organism.getName());
                            //Writes the update file
                            Date now = new Date();
                            updates.putIfAbsent(organism.getName(), now);
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
            return Futures.transformAsync(writeFuture, kingdom1 -> processOrganisms(kingdom, updates), executorService);
        }
    }

    /**
     * Creates the workbook for an organism then starts to process its genes.
     */
    private ListenableFuture<XSSFWorkbook> processGenes(Organism organism) {
        List<Tuple<String, String>> geneIds = organism.getGeneIds();
        XSSFWorkbook workbook = fileService.createWorkbook();
        HashMap<String, Sum> organismSums = new HashMap<>();
        ListenableFuture<XSSFWorkbook> processGenesFuture = Futures.transform(geneService.processGenes(organism, workbook, geneIds, organism.getPath(), organismSums), new Function<XSSFWorkbook, XSSFWorkbook>() {
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
        return Futures.transformAsync(processGenesFuture, processedWorkbook -> statisticsService.computeProbabilitiesFromSum(organism, organismSums, workbook), executorService);
    }
}
