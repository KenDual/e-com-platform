package com.maiphuhai.DTO;

public record AiProductDTO(
        Long id,
        String name,
        String company,
        Integer price,
        String specs,
        String image
) {}