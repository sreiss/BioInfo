package services.impls;

import com.google.inject.Inject;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;
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

    /*
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
    */

    /*
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
    */

    /*
    private class CreateWorkbookCallable implements Callable<XSSFWorkbook> {
        @Override
        public XSSFWorkbook call() throws Exception {
            return new XSSFWorkbook();
        }
    }
    */

    @Inject
    public DefaultFileService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
    }

    @Override
    public Promise<List<Boolean>, Throwable, Void> createDirectories(final List<String> paths) {
        return deferredManager.when(new UtilService.VoidCallable())
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        List<Promise<Boolean, Throwable, Void>> promises = new ArrayList<Promise<Boolean, Throwable, Void>>();
                        for (String path: paths) {
                            promises.add(createDirectory(path));
                        }
                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, List<Boolean>, Throwable, Void>() {
                    @Override
                    public Promise<List<Boolean>, Throwable, Void> pipeDone(MultipleResults oneResults) {
                        List<Boolean> results = new ArrayList<Boolean>();
                        for (OneResult oneResult: oneResults) {
                            results.add((Boolean) oneResult.getResult());
                        }
                        return new DeferredObject<List<Boolean>, Throwable, Void>().resolve(results);
                    }
                });
    }

    @Override
    public Promise<Boolean, Throwable, Void> createDirectory(final String path) {
        return deferredManager.when(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return (new File(path)).mkdirs();
            }
        });
    }

    @Override
    public Promise<XSSFWorkbook, Throwable, Void> createWorkbook() {
        return deferredManager.when(new Callable<XSSFWorkbook>() {
            @Override
            public XSSFWorkbook call() throws Exception {
                return new XSSFWorkbook();
            }
        });
    }

    @Override
    public Promise<Void, Throwable, Object> writeWorkbook(final XSSFWorkbook workbook, final String path, final String fileName) {
        return deferredManager.when(new DeferredCallable<Void, Object>() {
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
