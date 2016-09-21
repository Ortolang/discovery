package fr.ortolang.discovery.service;

import java.util.Collection;

import javax.ejb.Local;

import fr.ortolang.discovery.entity.EntityDescriptor;

@Local
public interface DiscoveryService {
    
    Collection<EntityDescriptor> listAll();
    
    void update();

}
