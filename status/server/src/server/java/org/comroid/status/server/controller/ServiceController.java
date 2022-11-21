package org.comroid.status.server.controller;

import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.auth.TokenProvider;
import org.comroid.status.server.repo.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import javax.persistence.PostLoad;
import java.util.Optional;

@Controller
public class ServiceController {
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private TokenProvider tokenProvider;

    @ResponseBody
    @GetMapping("/services")
    public Iterable<Service> getServices() {
        return serviceRepository.findAll();
    }

    @ResponseBody
    @GetMapping("/service/{id}")
    public Optional<Service> getService(@PathVariable("id") String name) {
        return serviceRepository.findById(name);
    }

    @ResponseBody
    @PostMapping("/services")
    public Service createService(@RequestParam Service service, @RequestHeader("Authorization") String authorization) {
        if (!tokenProvider.isAuthorized(StatusServer.ADMIN_TOKEN_NAME, authorization))
            throw new HttpStatusCodeException(HttpStatus.UNAUTHORIZED) {};
        return serviceRepository.save(service);
    }

    @ResponseBody
    @PutMapping("/service/{id}")
    public Service updateService(@PathVariable("id") String name, @RequestParam Service service) {
        var found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new HttpStatusCodeException(HttpStatus.NOT_FOUND) {};
        if (!found.get().getName().equals(service.getName()))
            throw new HttpStatusCodeException(HttpStatus.NOT_ACCEPTABLE) {
                @Override
                public String getMessage() {
                    return "ID mismatch";
                }
            };
        return serviceRepository.save(service);
    }

    @ResponseBody
    @DeleteMapping("/service/{id}")
    public Service deleteService(@PathVariable("id") String name, @RequestHeader("Authorization") String authorization) {
        Optional<Service> found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new HttpStatusCodeException(HttpStatus.NOT_FOUND) {};
        if (!tokenProvider.isAuthorized(name, authorization))
            throw new HttpStatusCodeException(HttpStatus.UNAUTHORIZED) {};
        serviceRepository.deleteById(name);
        return found.get();
    }

    @ResponseBody
    @PostMapping("/service/{id}/poll")
    public Service pollService(@PathVariable("id") String name, @RequestHeader("Authorization") String authorization) {
        Optional<Service> found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new HttpStatusCodeException(HttpStatus.NOT_FOUND) {};
        if (!tokenProvider.isAuthorized(name, authorization))
            throw new HttpStatusCodeException(HttpStatus.UNAUTHORIZED) {};
        return found.get().poll();
    }
}
