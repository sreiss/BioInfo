package services.impls;

import com.google.inject.Inject;
import models.Kingdom;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.*;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;
import services.contracts.*;

import java.util.ArrayList;
import java.util.List;

public class DefaultDataService implements DataService {
    private final HttpService httpService;
    private final ParseService parseService;
    private final FileService fileService;
    private final ConfigService configService;
    private final DeferredManager deferredManager;

    @Inject
    public DefaultDataService(HttpService httpService, ParseService parseService, FileService fileService, ConfigService configService, DeferredManager deferredManager) {
        this.httpService = httpService;
        this.parseService = parseService;
        this.fileService = fileService;
        this.configService = configService;
        this.deferredManager = deferredManager;
    }

    private String generateKingdomGeneListUrl(Kingdom kingdom) {
        return "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
    }

    @Override
    public Promise<Void, Throwable, MasterProgress> acquire(Kingdom[] kingdoms) {
        List<String> urls = new ArrayList<String>();
        for (Kingdom kingdom: kingdoms) {
            urls.add(generateKingdomGeneListUrl(kingdom));
        }

        return this.httpService.get(urls)
                .always(new AlwaysCallback<MultipleResults, OneReject>() {
                    @Override
                    public void onAlways(Promise.State state, MultipleResults oneResults, OneReject oneReject) {
                        //httpService.deleteObserver(MainController.this);
                    }
                })
                .then(new DonePipe<MultipleResults, Void, Throwable, MasterProgress>() {
                    @Override
                    public Promise<Void, Throwable, MasterProgress> pipeDone(MultipleResults oneResults) {
                        for (OneResult oneResult: oneResults) {

                        }
                        return saveData();
                    }
                });
    }

    public Promise<Void, Throwable, MasterProgress> saveData() {
        return deferredManager.when(configService.getProperty("dataDir"), fileService.createWorkbook())
                .then(new DonePipe<MultipleResults, Void, Throwable, MasterProgress>() {
                    @Override
                    public Promise<Void, Throwable, MasterProgress> pipeDone(MultipleResults oneResults) {
                        String dataDir = ((String) oneResults.get(0).getResult());
                        XSSFWorkbook workbook = ((XSSFWorkbook) oneResults.get(1).getResult());

                        workbook.createSheet("test");
                        return fileService.writeWorkbook(workbook, dataDir, "test.xlsx");
                    }
                });
    }
}
