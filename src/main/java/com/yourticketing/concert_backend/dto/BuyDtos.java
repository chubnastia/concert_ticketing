package com.yourticketing.concert_backend.dto;

public class BuyDtos {
    public static class BuyResponse {
        public long saleId;
        public BuyResponse(long saleId) { this.saleId = saleId; }
    }
}
