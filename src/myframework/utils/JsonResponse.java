package myframework.utils;


public class JsonResponse {
    private Object data;
    private String status;
    private int code;
    private Integer count;

    public JsonResponse() {}

    public JsonResponse(Object data, String status, int code, Integer count) {
        this.data = data;
        this.status = status;
        this.code = code;
        this.count = count;
    }

    // Getters & setters
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
