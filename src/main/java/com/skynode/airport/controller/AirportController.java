import com.skynode.airport.dto.AirportRequestDto;
import com.skynode.airport.dto.AirportResponseDto;
import com.skynode.airport.repository.AirportRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skynode.airport.service.AirportService;

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

}