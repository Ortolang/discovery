package fr.ortolang.discovery.resource;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fr.ortolang.discovery.entity.EntityDescriptor;
import fr.ortolang.discovery.service.DiscoveryService;

@Path("/entities")
@RequestScoped
public class EntitiesResource {

    private static final Logger LOGGER = Logger.getLogger(EntitiesResource.class.getName());

    @EJB
    private DiscoveryService service;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection<EntityDescriptor> listIDPs() {
        LOGGER.log(Level.INFO, "GET /entities");
        return service.listAll();
    }

    @POST
    public void update() {
        LOGGER.log(Level.INFO, "POST /entities");
        service.update();
    }

}
