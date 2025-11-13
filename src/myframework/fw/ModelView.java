package myframework.fw;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;

    public ModelView() {}

    public ModelView(String page) {
        this.view = page;
    }

    public String getView() {
        return view;
    }

    public void setView(String page) {
        this.view = page;
    }

}
