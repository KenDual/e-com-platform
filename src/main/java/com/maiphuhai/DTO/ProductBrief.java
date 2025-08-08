package com.maiphuhai.DTO;

public record ProductBrief(
        int productId,
        String description,
        String imageCover,
        long priceVnd
) {}