package com.icthh.xm.commons.util;

public class HeaderUtil {

    public static String getContentType(String fileFormat) {
        return switch (fileFormat.toLowerCase()) {
            case "csv" -> "text/csv";
            case "xlsx", "xls" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }
}
