package tech.amak.portbuddy.server.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
            "name", "port-buddy-server",
            "status", "ok",
            "docs", "https://portbuddy.dev/docs"
        );
    }
}
