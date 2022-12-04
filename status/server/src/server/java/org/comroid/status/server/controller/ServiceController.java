package org.comroid.status.server.controller;

import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.auth.TokenProvider;
import org.comroid.status.server.exception.InvalidDataException;
import org.comroid.status.server.exception.InvalidTokenException;
import org.comroid.status.server.exception.ServiceNotFoundException;
import org.comroid.status.server.repo.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
public class ServiceController {
    private final Map<String, Runnable> pollCancellation = new ConcurrentHashMap<>();
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private ScheduledExecutorService scheduler;

    @ResponseBody
    @GetMapping("/services")
    public Iterable<Service> getServices() {
        return serviceRepository.findAll();
    }

    @PostConstruct
    private void init() {
        serviceRepository.findAll().forEach(srv -> {
            if (!tokenProvider.hasToken(srv.getName()))
                tokenProvider.generate(srv.getName());
        });
    }

    @ResponseBody
    @GetMapping("/service/{id}")
    public Optional<Service> getService(@PathVariable("id") String name) {
        return serviceRepository.findById(name);
    }

    @ResponseBody
    @PostMapping("/services")
    public Service createService(@RequestBody Service service, @RequestHeader("Authorization") String authorization) {
        if (!tokenProvider.isAuthorized(StatusServer.ADMIN_TOKEN_NAME, authorization))
            throw new InvalidTokenException();
        return serviceRepository.save(service);
    }

    @ResponseBody
    @PostMapping("/service/{id}")
    public Service updateService(@PathVariable("id") String name, @RequestBody Service service) {
        var found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new ServiceNotFoundException(name);
        if (!found.get().getName().equals(service.getName()))
            throw new InvalidDataException("ID mismatch");
        return serviceRepository.save(service);
    }

    @ResponseBody
    @PostMapping("/service/{id}/status")
    public Service updateStatus(@PathVariable("id") String name, @RequestBody Service.Status status, @RequestHeader("Authorization") String authorization) {
        var found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new ServiceNotFoundException(name);
        if (!tokenProvider.isAuthorized(name, authorization))
            throw new InvalidTokenException();
        return updateStatus(found.get(), status);
    }

    @ResponseBody
    @DeleteMapping("/service/{id}")
    public Service deleteService(@PathVariable("id") String name, @RequestHeader("Authorization") String authorization) {
        Optional<Service> found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new ServiceNotFoundException(name);
        if (!tokenProvider.isAuthorized(name, authorization))
            throw new InvalidTokenException();
        serviceRepository.deleteById(name);
        return found.get();
    }

    @ResponseBody
    @PostMapping("/service/{id}/poll")
    public Service pollService(@PathVariable("id") String name, @RequestHeader("Authorization") String authorization, @RequestBody long rate) {
        Optional<Service> found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new ServiceNotFoundException(name);
        if (!tokenProvider.isAuthorized(name, authorization))
            throw new InvalidTokenException();
        Service srv = found.get();
        handlePoll(srv, rate);
        return srv;
    }

    @ResponseBody
    @DeleteMapping("/service/{id}/poll")
    public void deletePollService(@PathVariable("id") String name, @RequestHeader("Authorization") String authorization) {
        Optional<Service> found = serviceRepository.findById(name);
        if (found.isEmpty())
            throw new ServiceNotFoundException(name);
        if (!tokenProvider.isAuthorized(name, authorization))
            throw new InvalidTokenException();
        Service srv = found.get();
        stopPoll(srv);
    }

    private Service updateStatus(Service service, Service.Status status) {
        service.setStatus(status);
        return serviceRepository.save(service);
    }

    private void handlePoll(Service service, long rate) {
        stopPoll(service);
        updateStatus(service, Service.Status.ONLINE);
        final var close1 = scheduler.schedule(() -> updateStatus(service, Service.Status.NOT_RESPONDING), rate * 2, TimeUnit.SECONDS);
        final var close2 = scheduler.schedule(() -> updateStatus(service, Service.Status.CRASHED), rate * 3, TimeUnit.SECONDS);
        pollCancellation.put(service.getName(), () -> {
            close1.cancel(true);
            close2.cancel(true);
        });
    }

    private void stopPoll(Service service) {
        if (!pollCancellation.containsKey(service.getName()))
            return;
        pollCancellation.get(service.getName()).run();
    }
}
