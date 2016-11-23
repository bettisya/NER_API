package com.example.loading;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Created by aning on 16-11-23.
 */
public class ApiCount {
//    private int id;
    private String ip;
    private String apitype;
    private Date date;
    public ApiCount(String ip, String apitype, Date date){
        this.ip = ip;
        this.apitype = apitype;
        this.date = date;
    }
    public void getIP(String ip) { this.ip = ip;}
    public void getApitype(String apitype) { this.apitype = apitype;}
    public void getetDate(Date date) { this.date = date;}

}
