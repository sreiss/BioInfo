package Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;

public class ZipUtils
{
	private List<String> fileList;
	// Pour le test
	private static final String OUTPUT_ZIP_FILE = "Folder.zip";
	private static final String SOURCE_FOLDER = "Download";
	
	// Pour le projet
	private String outputZipFile;
	private String sourceFolder;

	public ZipUtils()
	{
		fileList = new ArrayList<String>();
	}
	
	public ZipUtils(String source, String output)
	{
		fileList = new ArrayList<String>();
		this.outputZipFile = output;
		this.sourceFolder = source;
	}

	public void zipIt(String zipFile)
	{
		byte[] buffer = new byte[1024];
		String source = "";
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		try
		{
			try
			{
				source = SOURCE_FOLDER.substring(SOURCE_FOLDER.lastIndexOf("\\") + 1, SOURCE_FOLDER.length());
			}
			catch (Exception e)
			{
				source = SOURCE_FOLDER;
			}
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);

			System.out.println("Output to Zip : " + zipFile);
			FileInputStream in = null;

			for (String file : this.fileList)
			{
				System.out.println("File Added : " + file);
				ZipEntry ze = new ZipEntry(source + File.separator + file);
				zos.putNextEntry(ze);
				try
				{
					in = new FileInputStream(SOURCE_FOLDER + File.separator + file);
					int len;
					while ((len = in.read(buffer)) > 0)
					{
						zos.write(buffer, 0, len);
					}
				}
				finally
				{
					in.close();   
				} 
			}

			zos.closeEntry();

			// Suppression du dossier temporaire
			FileUtils.forceDelete(new File(SOURCE_FOLDER));

			System.out.println("Folder successfully compressed");

		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				zos.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void generateFileList(File node)
	{
		// add file only
		if (node.isFile())
		{
			fileList.add(generateZipEntry(node.toString()));

		}

		if (node.isDirectory())
		{
			String[] subNote = node.list();
			for (String filename : subNote)
			{
				generateFileList(new File(node, filename));
			}
		}
	}

	private String generateZipEntry(String file)
	{
		return file.substring(SOURCE_FOLDER.length() + 1, file.length());
	}
	
	public void ExecuteZip(){
		this.generateFileList(new File(this.sourceFolder));
    	this.zipIt(this.outputZipFile);
	}
	
	//#region getter-setter
	public String getOutputZipFile() {
		return outputZipFile;
	}

	public void setOutputZipFile(String outputZipFile) {
		this.outputZipFile = outputZipFile;
	}

	public String getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(String sourceFolder) {
		this.sourceFolder = sourceFolder;
	}
	//#endRegion getter-setter
	
	public static void main(String[] args)
	{
		ZipUtils appZip = new ZipUtils();
		appZip.generateFileList(new File(SOURCE_FOLDER));
		appZip.zipIt(OUTPUT_ZIP_FILE);
	}
}    