package services.impls;

import com.google.inject.Inject;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.DeferredCallable;
import org.jdeferred.DeferredManager;
import org.jdeferred.DeferredRunnable;
import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;
import services.contracts.ExcelService;

import java.io.FileOutputStream;
import java.util.concurrent.Callable;

public class DefaultExcelService implements ExcelService {
    private final DeferredManager deferredManager;

    @Inject
    public DefaultExcelService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
    }

    public Promise<XSSFWorkbook, Throwable, Void> createWorkbook() {
        return deferredManager.when(new Callable<XSSFWorkbook>() {
            @Override
            public XSSFWorkbook call() throws Exception {
                return new XSSFWorkbook();
            }
        });
    }

    public Promise<Void, Throwable, MasterProgress> writeWorkbook(final XSSFWorkbook workbook, final String path, final String fileName) {
        return deferredManager.when(new DeferredCallable<Void, MasterProgress>() {
            @Override
            public Void call() throws Exception {
                FileOutputStream stream = new FileOutputStream(path + fileName);
                workbook.write(stream);
                stream.close();
                workbook.close();
                return null;
            }
        });
    }

}
