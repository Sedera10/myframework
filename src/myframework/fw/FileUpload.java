package myframework.fw;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class FileUpload {
    private String fieldName;
    private String fileName;
    private String contentType;
    private byte[] fileBytes;
    private ServletContext servletContext;  // ← Stocké automatiquement par le framework

    public FileUpload() {
    }

    public FileUpload(String fieldName, String fileName, String contentType, byte[] fileBytes) {
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileBytes = fileBytes;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public int getSize() {
        return fileBytes != null ? fileBytes.length : 0;
    }

    /**
     * Retourne l'extension du fichier (ex: "jpg", "pdf", "txt")
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Génère un nom de fichier unique en utilisant UUID + extension originale
     * Ex: "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
     */
    public String generateUniqueFileName() {
        String extension = getFileExtension();
        String uniqueName = UUID.randomUUID().toString();
        return extension.isEmpty() ? uniqueName : uniqueName + "." + extension;
    }

    /**
     * Sauvegarde le fichier dans un chemin relatif au projet web avec le nom original
     * Ex: saveTo("upload") → sauvegarde dans /webapp/upload/
     * Ex: saveTo("img/upload") → sauvegarde dans /webapp/img/upload/
     * 
     * @param relativePath Chemin relatif au projet (ex: "upload", "img/upload")
     * @return Le chemin complet du fichier sauvegardé
     */
    public String saveTo(String relativePath) throws IOException {
        return saveTo(relativePath, this.fileName);
    }

    /**
     * Sauvegarde le fichier dans un chemin relatif au projet web avec un nom personnalisé
     * 
     * @param relativePath Chemin relatif au projet (ex: "upload", "img/upload")
     * @param customFileName Nom personnalisé pour le fichier
     * @return Le chemin complet du fichier sauvegardé
     */
    public String saveTo(String relativePath, String customFileName) throws IOException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IOException("Aucun contenu de fichier à sauvegarder");
        }

        if (servletContext == null) {
            throw new IOException("ServletContext non disponible. Le fichier doit être créé par le framework.");
        }

        String realPath = servletContext.getRealPath("/" + relativePath);
        
        if (realPath == null) {
            throw new IOException("Impossible de résoudre le chemin: " + relativePath);
        }

        // Créer le répertoire s'il n'existe pas
        File directory = new File(realPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Créer le fichier de destination
        File destinationFile = new File(directory, customFileName);
        
        // Écrire les bytes dans le fichier
        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            fos.write(fileBytes);
        }

        return destinationFile.getAbsolutePath();
    }

    /**
     * Sauvegarde le fichier avec un nom unique généré automatiquement
     * 
     * @param relativePath Chemin relatif au projet (ex: "upload", "img/upload")
     * @return Le chemin complet du fichier sauvegardé
     */
    public String saveWithUniqueName(String relativePath) throws IOException {
        String uniqueFileName = generateUniqueFileName();
        return saveTo(relativePath, uniqueFileName);
    }

    /**
     * Sauvegarde le fichier dans un chemin absolu avec le nom original
     * UTILITÉ: Sauvegarder dans le projet source pour que les fichiers soient inclus dans le WAR
     * Ex: saveToAbsolutePath("D:/MonProjet/src/main/webapp/upload")
     * 
     * @param absolutePath Chemin absolu du répertoire
     * @return Le chemin complet du fichier sauvegardé
     */
    public String saveToAbsolutePath(String absolutePath) throws IOException {
        return saveToAbsolutePath(absolutePath, this.fileName);
    }

    /**
     * Sauvegarde le fichier dans un chemin absolu avec un nom personnalisé
     * 
     * @param absolutePath Chemin absolu du répertoire
     * @param customFileName Nom personnalisé pour le fichier
     * @return Le chemin complet du fichier sauvegardé
     */
    public String saveToAbsolutePath(String absolutePath, String customFileName) throws IOException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IOException("Aucun contenu de fichier à sauvegarder");
        }

        // Créer le répertoire s'il n'existe pas
        File directory = new File(absolutePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Créer le fichier de destination
        File destinationFile = new File(directory, customFileName);
        
        // Écrire les bytes dans le fichier
        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            fos.write(fileBytes);
        }

        return destinationFile.getAbsolutePath();
    }

    /**
     * Sauvegarde le fichier avec un nom unique dans un chemin absolu
     * 
     * @param absolutePath Chemin absolu du répertoire
     * @return Le chemin complet du fichier sauvegardé
     */
    public String saveToAbsolutePathWithUniqueName(String absolutePath) throws IOException {
        String uniqueFileName = generateUniqueFileName();
        return saveToAbsolutePath(absolutePath, uniqueFileName);
    }
}
