package com.hmkcode;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmkcode.vo.FileMeta;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

//this to be used with Java Servlet 3.0 API
@MultipartConfig 
public class FileUploadServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	// this will store uploaded files
	private static List<FileMeta> files = new LinkedList<FileMeta>();
	/***************************************************
	 * URL: /upload
	 * doPost(): upload the files and other parameters
	 ****************************************************/
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException{
	    
		// 1. Upload File Using Java Servlet API
		files.addAll(MultipartRequestHandler.uploadByJavaServletAPI(request));			
		
		// 1. Upload File Using Apache FileUpload

		
		// Remove some files
		while(files.size() > 20)
		{
			files.remove(0);
		}
		
		// 2. Set response type to json
		response.setContentType("application/json");
		
		// 3. Convert List<FileMeta> into JSON format
    	ObjectMapper mapper = new ObjectMapper();
    	
    	// 4. Send resutl to client
    	mapper.writeValue(response.getOutputStream(), files);
	
	}
	/***************************************************
	 * URL: /upload?f=value
	 * doGet(): get file of index "f" from List<FileMeta> as an attachment
	 ****************************************************/
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException{
		
		 // 1. Get f from URL upload?f="?"
		 String value = request.getParameter("f");
		 
		 // 2. Get the file of index "f" from the list "files"
		 FileMeta getFile = files.get(Integer.parseInt(value));
		 
		 try {		
			 	// 3. Set the response content type = file content type 
			 	response.setContentType(getFile.getFileType());
			 	
			 	// 4. Set header Content-disposition
			 	response.setHeader("Content-disposition", "attachment; filename=\""+getFile.getFileName()+"\"");
			 	
			 	// 5. Copy file inputstream to response outputstream
		        InputStream input =  getFile.getContent();
		        File outFile = new File(getFile.getFileName());
                        System.out.println(outFile.getAbsolutePath());
                        Class forname = Class.forName("com.mysql.jdbc.Driver");
                        Connection con = null;
                        con = DriverManager.getConnection("jdbc:mysql://localhost:3306/biblioteca","root","root");
                        con.setAutoCommit(false);
                        PreparedStatement psmt = null;
                        FileInputStream nuevoArchivo = new FileInputStream(outFile.getAbsolutePath());
                        POIFSFileSystem fs = new POIFSFileSystem(nuevoArchivo);
                        Workbook workbook;
                        workbook = WorkbookFactory.create(fs);
                        Sheet sheet = workbook.getSheetAt(0);
                        Row row;
                        for(int i = 1; i <= sheet.getLastRowNum();i++){
                            row = (Row) sheet.getRow(i);
                            int identificador = (int) row.getCell(0).getNumericCellValue();
                            String nombre = row.getCell(1).getStringCellValue();
                            String tipo = row.getCell(2).getStringCellValue();
                            String Curso = row.getCell(3).getStringCellValue();
                            String colegio = row.getCell(4).getStringCellValue();
                            
                            String sql = "INSERT INTO usuarios (identificador,nombreSol,tipo,cursoArea,colegio,clave,imagen) VALUES ('" + identificador + "','"+nombre+"','"+tipo+"','"+Curso+"','"+colegio+"','"+null+"','"+null+"')";
                            psmt = (PreparedStatement) con.prepareStatement(sql);
                            psmt.execute();
                            System.out.println("Import rows" + i);
                        }
                        con.commit();
                        psmt.close();
                        con.close();
		        input.close();
                        System.out.println("Succes import excel to mysql table");
		 }catch (IOException e) {
				e.printStackTrace();
		 } catch (ClassNotFoundException ex) {
                Logger.getLogger(FileUploadServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(FileUploadServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
		
	}
}
