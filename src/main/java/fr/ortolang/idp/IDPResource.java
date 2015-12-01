package fr.ortolang.idp;

import java.util.Collection;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/idps")
@RequestScoped
public class IDPResource {
    
    @EJB
    private IDPService service;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<IDPRepresentation> listIDPs() {
        return service.listIDPs();
    }

}
