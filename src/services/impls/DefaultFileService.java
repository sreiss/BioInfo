package services.impls;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import services.contracts.FileService;
import services.contracts.OrganismService;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.*;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

public class DefaultFileService implements FileService {
    private final ListeningExecutorService executorService;
    private final OrganismService organismService;

    @Inject
    public DefaultFileService(ListeningExecutorService listeningExecutorService, OrganismService organismService) {
        this.executorService = listeningExecutorService;
        this.organismService = organismService;
    }

    private DefaultMutableTreeNode buildTreeRoot(DefaultMutableTreeNode root, File file) {
        if (root == null) {
            root = new DefaultMutableTreeNode(file.getName());
        }
        DefaultMutableTreeNode children = new DefaultMutableTreeNode(file.getName());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (file.isDirectory()) {
                        root.add(buildTreeRoot(children, f));
                    } else {
                        root.add(children);
                    }
                }
            }
        }
        return root;
    }

    @Override
    public ListenableFuture<TreeModel> buildTree(String path) {
        return executorService.submit(() -> {
            DefaultMutableTreeNode root = buildTreeRoot(new DefaultMutableTreeNode(), new File(path));
            return new DefaultTreeModel(root);
        });
    }

    @Override
    public XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }

    @Override
    public XSSFSheet fillWorkbook(Organism organism, Gene gene, final XSSFWorkbook workbook) {
        String sheetName;
        if (gene.getType() != null) {
            sheetName = gene.getType().substring(0, 1).toUpperCase() + gene.getType().substring(1).toLowerCase() + "_" + gene.getName();
        } else {
            sheetName = gene.getName();
        }
        XSSFSheet sheet = workbook.createSheet(sheetName);
        sheet = fillFileDinu(gene, workbook, sheet);
        sheet = fillFileTrinu(gene, workbook, sheet);
        sheet = fillFileInfo(organism, gene, sheet);
        return sheet;
    }

    public XSSFWorkbook fillWorkbookSum(Organism organism, final XSSFWorkbook workbook) {
        String sheetName = "Sum";

        XSSFSheet sheet = workbook.createSheet(sheetName);
        fillOrganismInfo(organism, sheet);
        return workbook;
    }

    @Override
    public void writeWorkbook(XSSFWorkbook workbook, final String path, final String fileName) throws IOException {
        String filePath = Paths.get(path, sanitizeFileName(fileName) + ".xlsx").toString();
        FileOutputStream stream = new FileOutputStream(filePath);
        workbook.write(stream);
        stream.close();
        workbook.close();
    }

    private File createUpdateFile(Kingdom kingdom) throws IOException {
        String path = Paths.get("updates").toString();
        boolean directoryCreated = new File(path).mkdirs();
        File file = Paths.get(path, kingdom.getLabel() + ".json").toFile();
        file.createNewFile();
        return file;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replace("\\", " ")
                .replace("/", " ")
                .replace(":", " ")
                .replace("*", " ")
                .replace("?", " ")
                .replace("\"", " ")
                .replace("<", " ")
                .replace(">", " ")
                .replace("|", " ");
    }

    @Override
    public Map<String, Date> readUpdateFile(Kingdom kingdom) throws IOException {
        File file = createUpdateFile(kingdom);
        FileInputStream stream = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        Map<String, String> value = (Map<String, String>) JSONValue.parse(inputStreamReader);
        if (value == null) {
            return new HashMap<>();
        } else {
            Map<String, Date> parsedMap = new HashMap<>();
            for (Map.Entry<String, String> entry: value.entrySet()) {
                DateFormat format = organismService.getUpdateDateFormat();
                try {
                    parsedMap.putIfAbsent(entry.getKey(), format.parse(entry.getValue()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return parsedMap;
        }
    }

    @Override
    public void writeUpdateFile(Kingdom kingdom, Map<String, Date> updates) throws IOException {
        File file = createUpdateFile(kingdom);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        Map<String, String> json = new HashMap<>();
        DateFormat format = organismService.getUpdateDateFormat();
        for (Map.Entry<String, Date> entry: updates.entrySet()) {
            json.putIfAbsent(entry.getKey(), format.format(entry.getValue()));
        }
        JSONObject.writeJSONString(json, outputStreamWriter);
        outputStreamWriter.close();
        fileOutputStream.close();
    }

    //    @Override
//    public Promise<List<File>, Throwable, Object> readDirectory(final String path) {
//        return deferredManager.when(new DeferredCallable<List<File>, Object>() {
//            @Override
//            public List<File> call() throws Exception {
//                List<File> files = null;
//                File file = new File(path);
//                if (file.isDirectory()) {
//                    File[] filesArray = file.listFiles();
//                    if (filesArray != null) {
//                        files = new ArrayList<File>(Arrays.asList(filesArray));
//                    } else {
//                        throw new FileNotFoundException(path);
//                    }
//                } else {
//                    throw new NotDirectoryException(path);
//                }
//                return files;
//            }
//        });
//    }

    @Override
    public List<Boolean> createDirectories(final List<String> paths) {
        List<Boolean> results = new ArrayList<>();
        for (String path: paths) {
            results.add(createDirectory(path));
        }
        return results;
    }

    private Boolean createDirectory(final String path) {
        return new File(path).mkdirs();
    }

    private CellStyle buildCellStyleForProba(XSSFWorkbook workbook) {
        CellStyle probaStyle = workbook.createCellStyle();
        probaStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0.00"));
        return probaStyle;
    }

    private CellStyle buildCellStyleForNumber(XSSFWorkbook workbook) {
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0"));
        return numberStyle;
    }

    private XSSFSheet fillOrganismInfo(Organism organism, XSSFSheet sheet) {
        XSSFRow row;

        if ((row = sheet.getRow(1)) == null)
            row = sheet.createRow(1);

        row.createCell(12).setCellValue("Organism Name");
        row.createCell(13).setCellValue(organism.getName());

        sheet.autoSizeColumn(12);
        sheet.autoSizeColumn(13);

        return sheet;
    }

    private XSSFSheet fillFileInfo(Organism organism, Gene gene, XSSFSheet sheet) {

//        XSSFRow row;
//
//        if ((row = sheet.getRow(1)) == null)
//            row = sheet.createRow(1);
//
//        row.createCell(12).setCellValue("Organism Name");
//        row.createCell(13).setCellValue(organism.getName());
//
//        if ((row = sheet.getRow(3)) == null)
//            row = sheet.createRow(3);
//
//        row.createCell(12).setCellValue("Number of nucleotides");
//        row.createCell(13).setCellValue(organism.get);
//
//        if ((row = sheet.getRow(2)) == null)
//            row = sheet.createRow(2);
//
//        row.createCell(0).setCellValue("Nb CDS");
//        row.createCell(1).setCellValue(gene.getTotalCds());
//
//        if ((row = sheet.getRow(3)) == null)
//            row = sheet.createRow(3);
//
//        row.createCell(0).setCellValue("Nb CDS non traités");
//        row.createCell(1).setCellValue(gene.getTotalUnprocessedCds());
//
//        if ((row = sheet.getRow(4)) == null)
//            row = sheet.createRow(4);
//
//        row.createCell(0).setCellValue("Nb trinucléotides");
//        row.createCell(1).setCellValue(gene.getTotalUnprocessedCds());
//
//        if ((row = sheet.getRow(5)) == null)
//            row = sheet.createRow(5);
//
//        row.createCell(0).setCellValue("Nb dinucléotides");
//        row.createCell(1).setCellValue(gene.getTotalDinucleotide());

        return sheet;
    }

    private XSSFSheet fillFileTrinu(Gene g, XSSFWorkbook workbook, XSSFSheet sheet) {
        CellStyle numberStyle = buildCellStyleForNumber(workbook);
        CellStyle probaStyle = buildCellStyleForProba(workbook);
        XSSFCell tmpCell;
        XSSFRow row;

        if ((row = sheet.getRow(0)) == null)
            row = sheet.createRow(0);

        row.createCell(0).setCellValue("");
        row.createCell(1).setCellValue("Phase 0");
        row.createCell(2).setCellValue("Freq Phase 0");
        row.createCell(3).setCellValue("Phase 1");
        row.createCell(4).setCellValue("Freq Phase 1");
        row.createCell(5).setCellValue("Phase 2");
        row.createCell(6).setCellValue("Freq Phase 2");

        int i = 1;
        Set<String> keys = g.getTrinuStatPhase0().keySet();

        for (String key : keys) {
            if ((row = sheet.getRow(i)) == null)
                row = sheet.createRow(i);

            // Set Trinicludotide
            row.createCell(0).setCellValue(key);

            // NB phase 0
            tmpCell = row.createCell(1);
            tmpCell.setCellValue(g.getTrinuStatPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);
            //tmp0 += g.trinuStatPhase0.get(key);

            // Proba phase 0
            tmpCell = row.createCell(2);
            tmpCell.setCellValue(g.getTrinuProbaPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            // NB phase 1
            tmpCell = row.createCell(3);
            tmpCell.setCellValue(g.getTrinuStatPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);
            //tmp1 += g.trinuStatPhase1.get(key);

            // Proba phase 1
            tmpCell = row.createCell(4);
            tmpCell.setCellValue(g.getTrinuProbaPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);
            //tmpCell.setCellType(XSSFCell.CELL_TYPE_NUMERIC);

            // NB phase 2
            tmpCell = row.createCell(5);
            tmpCell.setCellValue(g.getTrinuStatPhase2().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);
            //tmp2 += g.trinuStatPhase2.get(key);

            // Proba phase 2
            tmpCell = row.createCell(6);
            tmpCell.setCellValue(g.getTrinuProbaPhase2().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            i++;
        }

        row = sheet.createRow(i);
        row.createCell(0).setCellValue("Total");

        tmpCell = row.createCell(1);
        tmpCell.setCellValue(g.getTotalTrinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(numberStyle);

        tmpCell = row.createCell(2);
        tmpCell.setCellValue(g.getTotalProbaTrinu0());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(probaStyle);

        tmpCell = row.createCell(3);
        tmpCell.setCellValue(g.getTotalTrinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(numberStyle);

        tmpCell = row.createCell(4);
        tmpCell.setCellValue(g.getTotalProbaTrinu1());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(probaStyle);

        tmpCell = row.createCell(5);
        tmpCell.setCellValue(g.getTotalTrinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(numberStyle);

        tmpCell = row.createCell(6);
        tmpCell.setCellValue(g.getTotalProbaTrinu2());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(probaStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);

        return sheet;
    }

    private XSSFSheet fillFileDinu(Gene g, XSSFWorkbook workbook, XSSFSheet sheet) {
        CellStyle numberStyle = buildCellStyleForNumber(workbook);
        CellStyle probaStyle = buildCellStyleForProba(workbook);
        XSSFCell tmpCell;
        XSSFRow row;

        int rowNumber =  g.getTrinuProbaPhase0().size() + 3;

        if ((row = sheet.getRow(rowNumber)) == null)
            row = sheet.createRow(rowNumber);

        Set<String> keys = g.getDinuStatPhase0().keySet();

        int i = rowNumber + 1;
        for (String key : keys) {
            if ((row = sheet.getRow(i)) == null)
                row = sheet.createRow(i);

            // Set Dinicludotide
            row.createCell(0).setCellValue(key);

            // NB phase 0
            tmpCell = row.createCell(1);
            tmpCell.setCellValue(g.getDinuStatPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);

            // Proba phase 0
            tmpCell = row.createCell(2);
            tmpCell.setCellValue(g.getDinuProbaPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            // NB phase 1
            tmpCell = row.createCell(3);
            tmpCell.setCellValue(g.getDinuStatPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);

            // Proba phase 1
            tmpCell = row.createCell(4);
            tmpCell.setCellValue(g.getDinuProbaPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            i++;
        }

//        if ((row = sheet.getRow(i)) == null)
//            row = sheet.createRow(i);
//
//        row.createCell(8).setCellValue("Total");
//
//        tmpCell = row.createCell(9);
//        tmpCell.setCellValue(g.getTotalDinucleotide());
//        tmpCell.setCellType(CellType.NUMERIC);
//        tmpCell.setCellStyle(numberStyle);
//
//        tmpCell = row.createCell(10);
//        tmpCell.setCellValue(g.getTotalProbaDinu0());
//        tmpCell.setCellType(CellType.NUMERIC);
//        tmpCell.setCellStyle(probaStyle);
//
//        tmpCell = row.createCell(11);
//        tmpCell.setCellValue(g.getTotalDinucleotide());
//        tmpCell.setCellType(CellType.NUMERIC);
//        tmpCell.setCellStyle(numberStyle);
//
//        tmpCell = row.createCell(12);
//        tmpCell.setCellValue(g.getTotalProbaDinu1());
//        tmpCell.setCellType(CellType.NUMERIC);
//        tmpCell.setCellStyle(probaStyle);

        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);

        return sheet;
    }

}
