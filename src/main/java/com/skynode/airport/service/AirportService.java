package com.skynode.airport.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.skynode.airport.dto.AirportRequestDto;
import com.skynode.airport.dto.AirportResponseDto;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;

@Service 
public class AirportService {

    private final AirportRepository airportRepository;

    AirportService(AirportRepository airportRepository){
        this.airportRepository=airportRepository;
    }



    public AirportResponseDto addAirport(AirportRequestDto request){
        Airport airport = new Airport();
        airport.setName(request.getName());
        airport.setCountry(request.getCountry());
        airport.setCity(request.getCity());

        Airport savedAirport= airportRepository.save(airport);

        return new AirportResponseDto(
            savedAirport.getId(),
            savedAirport.getName(),
            savedAirport.getCountry(),
            savedAirport.getCity()
        );
    }

    public List<AirportResponseDto> getAllAirports(){
        List<Airport> airports = new ArrayList<>();
        airports=airportRepository.findAll();
        List<AirportResponseDto> response = new ArrayList<>();

        for(Airport airport : airports){
            AirportResponseDto dto = new AirportResponseDto();
            dto.setId(airport.getId());
            dto.setName(airport.getName());
            dto.setCountry(airport.getCountry());
            dto.setCity(airport.getCity());

            response.add(dto);

        }
    
        return response;
        
    }

    public AirportResponseDto getAirportById(Long id){
        Airport airport = airportRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Airport with id "+id+" not found"));

        AirportResponseDto response = new AirportResponseDto(
            airport.getId(),
            airport.getName(),
            airport.getCountry(),
            airport.getCity()
        );
       
            return response;
    
    }

    public String deleteAirportById(Long id){

        Airport airport = airportRepository.findById(id)
        .orElseThrow(()-> new RuntimeException("Airport with id "+id+" not found."));

        if (airport!=null) {
            airportRepository.delete(airport);
            return "Airport with id "+id+" deleted.";
        }

        return "deletion failed.";
    }

}