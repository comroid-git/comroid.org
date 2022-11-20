package org.comroid.status.server.controller;

import org.comroid.status.entity.Service;
import org.comroid.status.server.db.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ServiceController {
    @Autowired
    private ServiceRepository serviceRepository;

    @GetMapping("/services")
    @ResponseBody
    public Iterable<Service> getServices() {
        return serviceRepository.findAll();
    }
}
