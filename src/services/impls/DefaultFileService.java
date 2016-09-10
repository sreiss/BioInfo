package services.impls;

import com.google.inject.Inject;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.*;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import services.contracts.FileService;
import services.contracts.UtilService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class DefaultFileService implements FileService {
    private final DeferredManager deferredManager;

    // Callables.

    private class CreateDirectoryCallable implements Callable<Boolean> {
        private final String path;

        public CreateDirectoryCallable(String path) {
            this.path = path;
        }

        @Override
        public Boolean call() throws Exception {
            return (new File(path)).mkdirs();
        }
    }

    private class WriteWorkbookCallable implements Callable<Void> {
        private final XSSFWorkbook workbook;
        private final String path;
        private final String fileName;

        public WriteWorkbookCallable(XSSFWorkbook workbook, String path, String fileName) {
            this.workbook = workbook;
            this.path = path;
            this.fileName = fileName;
        }

        @Override
        public Void call() throws Exception {
            FileOutputStream stream = new FileOutputStream(path + fileName);
            workbook.write(stream);
            stream.close();
            workbook.close();
            return null;
        }
    }

    private class CreateWorkbookCallable implements Callable<XSSFWorkbook> {
        @Override
        public XSSFWorkbook call() throws Exception {
            return new XSSFWorkbook();
        }
    }

    @Inject
    public DefaultFileService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
    }

    @Override
    public Promise<MultipleResults, OneReject, MasterProgress> createDirectories(final List<String> paths) {
        return deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        List<Callable<Boolean>> callables = new ArrayList<Callable<Boolean>>();
                        for (String path: paths) {
                            callables.add(new CreateDirectoryCallable(path));
                        }
                        return deferredManager.when(callables.toArray(new Callable[callables.size()]));
                    }
                });
    }

    @Override
    public Promise<Boolean, Throwable, Void> createDirectory(final String path) {
        return deferredManager.when(new CreateDirectoryCallable(path));
    }

    @Override
    public Promise<XSSFWorkbook, Throwable, Void> createWorkbook() {
        return deferredManager.when(new CreateWorkbookCallable());
    }

    @Override
    public Promise<Void, Throwable, Void> writeWorkbook(XSSFWorkbook workbook, String path, String fileName) {
        return deferredManager.when(new WriteWorkbookCallable(workbook, path, fileName));
    }

}
