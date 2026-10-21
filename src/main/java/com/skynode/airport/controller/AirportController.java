package com.skynode.airport.controller;

import com.skynode.airport.dto.AirportRequestDto;
import com.skynode.airport.dto.AirportResponseDto;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;
import com.skynode.airport.service.AirportService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AirportController{

    private final AirportService airportService;
    private final AirportRepository airportRepository;


    public AirportController(AirportService airportService, AirportRepository airportRepository){
        this.airportService=airportService;
        this.airportRepository= airportRepository;
    }
        @PostMapping("/addAirport")
        public AirportResponseDto addAirport(@RequestBody AirportRequestDto airportRequestDto){
            AirportResponseDto response = airportService.addAirport(airportRequestDto);
            return response;

    }

    @GetMapping("/getAirport")
    public AirportResponseDto getAirport(@RequestParam Long id){

        AirportResponseDto airport=airportService.getAirportById(id);
        return airport;

    }

    @GetMapping("/getAllAirports")
    public List<AirportResponseDto> getAllAirports() {
        List<AirportResponseDto> airports = new ArrayList<>();
        airports=airportService.getAllAirports();
        return airports;
    }

    @DeleteMapping ("/deleteAirportById")
    public String deleteAirportById(@RequestParam Long id){
        String response=airportService.deleteAirportById(id);
        return response;
    }
    

}