package com.maiphuhai.DTO;

import java.util.List;

public class SearchResponse {
    private List<AiProduct> products;

    public List<AiProduct> getProducts() {
        return products;
    }
    public void setProducts(List<AiProduct> products) {
        this.products = products;
    }
}
