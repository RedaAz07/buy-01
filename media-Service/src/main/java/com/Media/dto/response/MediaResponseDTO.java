package com.Media.dto.response;

import java.util.List;

public record MediaResponseDTO(List<String> files, String ProductId, String UserID) {
}