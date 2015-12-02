package fr.ortolang.idp;

import java.util.Collection;

import javax.ejb.Local;

@Local
public interface IDPService {
    
    public Collection<IDPRepresentation> listIDPs();
    
    public void updateIDPs();
    
}
