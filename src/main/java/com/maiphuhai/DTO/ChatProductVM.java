package com.maiphuhai.DTO;

public record ChatProductVM(
        Long id,
        String name,
        String company,
        Long priceVnd,      // có thể null nếu AI có price riêng; ưu tiên DB
        String specs,
        String image,       // ưu tiên AI, fallback DB ImageCover
        String description  // lấy từ DB
) {}