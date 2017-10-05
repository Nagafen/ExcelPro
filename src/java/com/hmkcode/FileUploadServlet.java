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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//this to be used with Java Servlet 3.0 API
@MultipartConfig
public class FileUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // this will store uploaded files
    private static List<FileMeta> files = new LinkedList<FileMeta>();

    /**
     * *************************************************
     * URL: /upload doPost(): upload the files and other parameters
	 ***************************************************
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Upload File Using Java Servlet API
        files.addAll(MultipartRequestHandler.uploadByJavaServletAPI(request));
        // Remove some files
        while (files.size() > 20) {
            files.remove(0);
        }

        // 2. Set response type to json
        response.setContentType("application/json");

        // 3. Convert List<FileMeta> into JSON format
        ObjectMapper mapper = new ObjectMapper();

        // 4. Send resutl to client
        mapper.writeValue(response.getOutputStream(), files);

    }

    /**
     * *************************************************
     * URL: /upload?f=value doGet(): get file of index "f" from List<FileMeta>
     * as an attachment
	 ***************************************************
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Get f from URL upload?f="?"
        String value = request.getParameter("f");

        // 2. Get the file of index "f" from the list "files"
        FileMeta getFile = files.get(Integer.parseInt(value));

        try {
            // 3. Set the response content type = file content type 
            response.setContentType(getFile.getFileType());

            // 4. Set header Content-disposition
            response.setHeader("Content-disposition", "attachment; filename=\"" + getFile.getFileName() + "\"");

            // 5. Copy file inputstream to response outputstream
            //lectura del archivo
            InputStream input = getFile.getContent();
            File outFile = new File(getFile.getFileName());
            System.out.println(outFile.getAbsolutePath());
            FileOutputStream output = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024 * 10];

            for (int length = 0; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
            int c;
            while ((c = input.read()) != -1) {
                output.write(c);
            }

            //ingreso de base de datos
            Connection conexion = Util.conexion.getConnection();
            FileInputStream nuevoArchivo = new FileInputStream(outFile.getAbsolutePath());
            PreparedStatement statement = null;
            OPCPackage pkg = OPCPackage.open(nuevoArchivo);
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            XSSFSheet sheet = wb.getSheetAt(0);
            Row row;
            DataFormatter formatter = new DataFormatter(Locale.US);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i++);
                String identificador = formatter.formatCellValue(row.getCell(0));
                String nombre = formatter.formatCellValue(row.getCell(1));
                String tipo = formatter.formatCellValue(row.getCell(2));
                String Curso = formatter.formatCellValue(row.getCell(3));
                String colegio = formatter.formatCellValue(row.getCell(4));

                String sql = "insert into usuarios (identificador,nombreSol,tipo,cursoArea,colegio,clave,imagen) values (?,?,?,?,?,?,?)";
                statement = conexion.prepareStatement(sql);
                statement.setString(1, identificador);
                statement.setString(2, nombre);
                statement.setString(3, tipo);
                statement.setString(4, Curso);
                statement.setString(5, colegio);
                statement.setString(6, null);
                statement.setString(7, null);
                statement.execute();
                System.out.println("Import rows" + i);
            }
            conexion.close();
            statement.close();
            output.close();
            input.close();
            System.out.println("Succes import excel to mysql table");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException ex) {
            Logger.getLogger(FileUploadServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidFormatException ex) {
            Logger.getLogger(FileUploadServlet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
