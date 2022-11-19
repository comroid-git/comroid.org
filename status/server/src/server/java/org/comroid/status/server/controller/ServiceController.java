package org.comroid.status.server.controller;

import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.util.PingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ServiceController {
    @GetMapping("/services")
    @ResponseBody
    public List<Service> getServices() {
        return
                StatusServer.instance
                        .getEntityCache()
                        .filterKey(name -> !name.equals("test-dummy"))
                        .flatMap(Service.class)
                        .sorted(Comparator.comparingInt(srv -> srv instanceof PingService ? 0 : 1))
                        .stream()
                        .collect(Collectors.toList());
    }
}
