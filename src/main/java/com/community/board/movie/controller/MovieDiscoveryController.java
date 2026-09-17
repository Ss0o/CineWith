package com.community.board.movie.controller;
import com.community.board.movie.discovery.DiscoveryHome;
import com.community.board.movie.discovery.DiscoveryPage;
import com.community.board.movie.discovery.MovieDiscoveryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
@Validated @RestController @RequestMapping("/api/movies/discovery")
public class MovieDiscoveryController {
 private final MovieDiscoveryService service; public MovieDiscoveryController(MovieDiscoveryService service){this.service=service;}
 @GetMapping("/home") public DiscoveryHome home(){return service.getHome();}
 @GetMapping("/{section}") public DiscoveryPage page(@PathVariable String section, @RequestParam(defaultValue="0") @Min(0) @Max(499) int page){return service.getPage(section,page);}
}
