package services.impls;

import com.google.inject.Inject;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import services.contracts.*;

public class DefaultDataService implements DataService {
    private final HttpService httpService;
    private final ParseService parseService;
    private final ExcelService excelService;
    private final ConfigService configService;
    private final DeferredManager deferredManager;

    @Inject
    public DefaultDataService(HttpService httpService, ParseService parseService, ExcelService excelService, ConfigService configService, DeferredManager deferredManager) {
        this.httpService = httpService;
        this.parseService = parseService;
        this.excelService = excelService;
        this.configService = configService;
        this.deferredManager = deferredManager;
    }

    @Override
    public Promise<Void, Throwable, MasterProgress> acquire() {
        String[] str = new String[1000000];
        for (int i = 0; i < 100000; i++) {
            str[i] = "http://localhost:3000/api/wines/count";
        }

        return this.httpService.get(str)
                .always(new AlwaysCallback<MultipleResults, OneReject>() {
                    @Override
                    public void onAlways(Promise.State state, MultipleResults oneResults, OneReject oneReject) {
                        //httpService.deleteObserver(MainController.this);
                    }
                })
                .then(new DonePipe<MultipleResults, Void, Throwable, MasterProgress>() {
                    @Override
                    public Promise<Void, Throwable, MasterProgress> pipeDone(MultipleResults oneResults) {
                        return saveData();
                    }
                });
    }

    public Promise<Void, Throwable, MasterProgress> saveData() {
        return deferredManager.when(configService.getProperty("dataDir"), excelService.createWorkbook())
                .then(new DonePipe<MultipleResults, Void, Throwable, MasterProgress>() {
                    @Override
                    public Promise<Void, Throwable, MasterProgress> pipeDone(MultipleResults oneResults) {
                        String dataDir = ((String) oneResults.get(0).getResult());
                        XSSFWorkbook workbook = ((XSSFWorkbook) oneResults.get(1).getResult());

                        workbook.createSheet("test");
                        return excelService.writeWorkbook(workbook, dataDir, "test.xlsx");
                    }
                });
    }
}
