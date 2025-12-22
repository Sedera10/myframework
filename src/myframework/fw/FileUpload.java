package myframework.fw;

public class FileUpload {
    private String fieldName;
    private String fileName;
    private String contentType;
    private byte[] fileBytes;

    public FileUpload() {
    }

    public FileUpload(String fieldName, String fileName, String contentType, byte[] fileBytes) {
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileBytes = fileBytes;
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
}
