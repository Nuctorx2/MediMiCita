package co.edu.usco.medimicita.controller.api;

import co.edu.usco.medimicita.dto.AvailableSlotDto;
import co.edu.usco.medimicita.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/patient/appointments") // Ruta base para esta API
public class AvailabilityApiController {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityApiController.class);
    private final AvailabilityService availabilityService;


    @GetMapping("/available-dates")
    public ResponseEntity<List<String>> getAvailableDates(
            @RequestParam Integer specialtyId,
            @RequestParam(required = false) Long doctorId, // Opcional
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        try {
            List<LocalDate> dates = availabilityService.getAvailableDates(specialtyId, Optional.ofNullable(doctorId), month);
            List<String> dateStrings = dates.stream().map(LocalDate::toString).collect(Collectors.toList());
            return ResponseEntity.ok(dateStrings);
        } catch (Exception e) {
            log.error("Error al obtener fechas disponibles API: specId={}, docId={}, month={}", specialtyId, doctorId, month, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<AvailableSlotDto>> getAvailableSlots(
            @RequestParam Integer specialtyId,
            @RequestParam(required = false) Long doctorId, // Opcional
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<AvailableSlotDto> slots = availabilityService.getAvailableTimeSlots(specialtyId, Optional.ofNullable(doctorId), date);
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            log.error("Error al obtener slots disponibles API: specId={}, docId={}, date={}", specialtyId, doctorId, date, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}