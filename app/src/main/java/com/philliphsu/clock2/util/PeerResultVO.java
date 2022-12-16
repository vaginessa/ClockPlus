package com.philliphsu.clock2.util;

import java.io.Serializable;
import java.util.List;

public class PeerResultVO implements Serializable {

    private String code;

    private String status;

    private String message;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private TransferFileVO data;

    public TransferFileVO getData() {
        return data;
    }

    public void setData(TransferFileVO data) {
        this.data = data;
    }



    public static class TransferFileVO {
        private String url;
        private List<String> piecces;
        private List<Integer> orders;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public List<String> getPiecces() {
            return piecces;
        }

        public void setPiecces(List<String> piecces) {
            this.piecces = piecces;
        }

        public List<Integer> getOrders() {
            return orders;
        }

        public void setOrders(List<Integer> orders) {
            this.orders = orders;
        }
    }
}
