package com.maiphuhai.DTO;

import java.util.List;

public record AiResponseDTO(
        String answer,
        List<AiProductDTO> products
) {}