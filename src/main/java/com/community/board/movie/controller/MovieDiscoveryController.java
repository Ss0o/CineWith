package com.community.board.movie.controller;
import com.community.board.movie.discovery.DiscoveryHome;
import com.community.board.movie.discovery.MovieDiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/movies/discovery")
public class MovieDiscoveryController {
 private final MovieDiscoveryService service; public MovieDiscoveryController(MovieDiscoveryService service){this.service=service;}
 @GetMapping("/home") public DiscoveryHome home(){return service.getHome();}
}
