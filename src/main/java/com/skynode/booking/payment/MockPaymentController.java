package com.skynode.booking.payment;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skynode.InvalidRequestException;
import com.skynode.booking.dto.BookingRequestDto;
import com.skynode.booking.dto.BookingResponseDto;
import com.skynode.booking.service.BookingService;

@RestController
@RequestMapping("/api/v1/mock-payments")
public class MockPaymentController {
    private static final String DRAFT = "skynode.mockPayment";
    private static final int DEMO_AMOUNT = 4999;
    private final BookingService bookings;

    public MockPaymentController(BookingService bookings) {
        this.bookings = bookings;
    }

    public record StartRequest(@NotBlank String fullName, @NotBlank @Email String email,
                               @NotBlank @Pattern(regexp = "[0-9+() -]{7,20}") String phone,
                               @NotNull @Min(1) @Max(120) Integer age,
                               @NotBlank @Pattern(regexp = "Male|Female|Other|Prefer not to say") String gender,
                               @NotNull @Positive Long flightInstanceId, @NotNull @Positive Long seatId) {
    }

    public record VerifyRequest(@NotBlank String paymentId,
                                @NotBlank @Pattern(regexp = "[0-9]{4}") String otp) {
    }

    private static class Pending {
        final String id = UUID.randomUUID().toString();
        final StartRequest details;
        BookingResponseDto result;
        Pending(StartRequest details) { this.details = details; }
    }

    @PostMapping("/start")
    public Map<String, Object> start(@Valid @RequestBody StartRequest request, HttpSession session) {
        // Only passenger and seat details enter the session. The demo card stays in the browser.
        StartRequest details = new StartRequest(request.fullName().trim(), request.email().trim(),
            request.phone().trim(), request.age(), request.gender(), request.flightInstanceId(), request.seatId());
        Pending pending = new Pending(details);
        session.setAttribute(DRAFT, pending);
        return Map.of("paymentId", pending.id, "amount", DEMO_AMOUNT, "currency", "INR", "status", "OTP_REQUIRED");
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody VerifyRequest request, HttpSession session) {
        Pending pending = (Pending) session.getAttribute(DRAFT);
        if (pending == null || !pending.id.equals(request.paymentId())) {
            throw new InvalidRequestException("Mock payment session expired. Start again.");
        }
        synchronized (pending) {
            if (pending.result == null) {
                StartRequest details = pending.details;
                pending.result = bookings.createDemoBooking(new BookingRequestDto(
                    details.fullName(), details.flightInstanceId(), details.seatId()),
                    details.email(), details.phone(), details.age(), details.gender(), DEMO_AMOUNT);
            }
            return Map.of("status", "PAID_DEMO", "booking", pending.result);
        }
    }
}
