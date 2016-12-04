package services.impls;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Gene;
import models.Kingdom;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import services.contracts.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;

public class DefaultOrganismService implements OrganismService {
    private final GeneService geneService;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;
    private final FileService fileService;
    private final ProgramStatsService programStatsService;
    private final StatisticsService statisticsService;

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

    @Override
    public ListenableFuture<Organism> processOrganism(Kingdom kingdom, Organism organism) {
        return executorService.submit(() -> {
            HashMap<String, Sum> organismSums = new HashMap<>();
            List<Tuple<String, String>> geneIds = organism.getGeneIds();
            List<ListenableFuture<Gene>> geneFutures = new ArrayList<>();
            for (Tuple<String, String> geneId: geneIds) {
                geneFutures.add(geneService.processGene(kingdom, organism, organismSums, geneId));
            }
            List<Gene> genes = Futures.successfulAsList(geneFutures).get();

            XSSFWorkbook workbook = fileService.createWorkbook();
            for (Gene gene: genes) {
                if (gene != null) {
                    fileService.fillWorkbook(organism, gene, workbook);
                }
            }
            fileService.fillWorkbookSum(organism, organismSums, workbook);
            fileService.writeWorkbook(workbook, organism.getPath(), organism.getName());

            for (Map.Entry<String, Sum> organismSumEntry: organismSums.entrySet()) {
                statisticsService.computeProbabilitiesFromSum(organism, organismSumEntry.getValue());
            }

            programStatsService.addDate(ZonedDateTime.now());
            programStatsService.setRemainingRequests(programStatsService.getRemainingRequests());
            progressService.getCurrentProgress().getProgress().incrementAndGet();
            progressService.invalidateProgress();

            System.out.print(organism.getName());

            return organism;
        });
    }
}
