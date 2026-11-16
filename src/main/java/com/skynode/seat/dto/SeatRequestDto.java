package com.skynode.seat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeatRequestDto(@NotBlank @Size(max = 255) String seatNumber,
                             @Size(max = 20) String seatClass) {
}
