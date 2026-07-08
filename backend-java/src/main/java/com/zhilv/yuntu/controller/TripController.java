package com.zhilv.yuntu.controller;

import com.zhilv.yuntu.dto.ItineraryResponse;
import com.zhilv.yuntu.dto.TripRequest;
import com.zhilv.yuntu.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/trip")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/generate")
    public ItineraryResponse generate(@Valid @RequestBody TripRequest request) {
        return tripService.generate(request);
    }

    @PostMapping("/edit")
    public ItineraryResponse edit(@RequestBody ItineraryResponse itinerary) {
        return tripService.edit(itinerary);
    }

    @PostMapping("/save")
    public ItineraryResponse.TripSaveResponse save(@RequestBody ItineraryResponse.TripSaveRequest request) {
        String tripId = request.tripId() != null ? request.tripId() : request.itinerary().tripId();
        tripService.save(tripId, request.itinerary());
        return new ItineraryResponse.TripSaveResponse(tripId, "saved");
    }

    @GetMapping
    public ItineraryResponse.TripListResponse list() {
        var trips = tripService.list();
        return new ItineraryResponse.TripListResponse(trips, trips.size());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("saved_trip_count", tripService.list().size());
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ItineraryResponse.TripDetailResponse> detail(@PathVariable String tripId) {
        ItineraryResponse itinerary = tripService.get(tripId);
        if (itinerary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ItineraryResponse.TripDetailResponse(itinerary));
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> delete(@PathVariable String tripId) {
        tripService.delete(tripId);
        return ResponseEntity.noContent().build();
    }
}
