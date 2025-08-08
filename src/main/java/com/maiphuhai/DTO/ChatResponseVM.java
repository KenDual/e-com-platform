package com.maiphuhai.DTO;

import java.util.List;

public record ChatResponseVM(
        String answer,
        List<ChatProductVM> products
) {}