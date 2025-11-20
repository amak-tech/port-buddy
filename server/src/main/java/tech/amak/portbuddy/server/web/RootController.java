package tech.amak.portbuddy.server.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.amak.portbuddy.common.dto.root.RootIndexResponse;

@RestController
public class RootController {

    @GetMapping("/")
    public RootIndexResponse index() {
        return new RootIndexResponse("port-buddy-server", "ok", "https://portbuddy.dev/docs");
    }
}
