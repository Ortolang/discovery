package fr.ortolang.discovery.entity;

/*-
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2017 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.codec.digest.DigestUtils;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import fr.ortolang.discovery.DiscoveryConfig;

@XmlRootElement(name = "idp")
@XmlType(name = "idp")
public class EntityDescriptor {

    private String entityId;
    private String alias;
    private String name;
    private String description;
    private String logo;
    private String certificate;
    private Date registrationDate;
    private String ssoURL;

    public EntityDescriptor() {
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
        this.alias = DigestUtils.md5Hex(entityId);
    }

    public String getAlias() {
        return alias;
    }
    
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        //this.alias = name.replaceAll("[^A-Za-z0-9]", "");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    @XmlTransient
    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate.trim().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "");
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getSsoURL() {
        return ssoURL;
    }

    public void setSsoURL(String ssoURL) {
        this.ssoURL = ssoURL;
    }

    public IdentityProviderRepresentation toIdentityProviderRepresentation() {
        IdentityProviderRepresentation out = new IdentityProviderRepresentation();
        out.setAlias(alias);
        out.setProviderId("saml");
        out.setEnabled(true);
        out.setTrustEmail(false);
        out.setStoreToken(false);
        out.setAddReadTokenRoleOnCreate(false);
        out.setAuthenticateByDefault(false);
        Map<String, String> config = new HashMap<String, String>();
        config.put("nameIDPolicyFormat", DiscoveryConfig.getInstance().getProperty(DiscoveryConfig.Property.NAME_ID_POLICY_FORMAT));
        config.put("singleSignOnServiceUrl", ssoURL);
        config.put("validateSignature", "false");
        config.put("signingCertificate", certificate);
        config.put("postBindingResponse", "true");
        config.put("postBindingAuthnRequest", "true");
        config.put("forceAuthn", "false");
        config.put("wantAuthnRequestsSigned", "true");
        out.setConfig(config);
        return out;
    }
    
    @Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append("{alias:").append(alias);
        out.append(", entityId:").append(entityId);
        out.append(", name:").append(name);
        out.append(", url:").append(ssoURL);
        out.append("}");
        return out.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((certificate == null) ? 0 : certificate.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
        result = prime * result + ((logo == null) ? 0 : logo.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((registrationDate == null) ? 0 : registrationDate.hashCode());
        result = prime * result + ((ssoURL == null) ? 0 : ssoURL.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityDescriptor other = (EntityDescriptor) obj;
        if (certificate == null) {
            if (other.certificate != null)
                return false;
        } else if (!certificate.equals(other.certificate))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (entityId == null) {
            if (other.entityId != null)
                return false;
        } else if (!entityId.equals(other.entityId))
            return false;
        if (logo == null) {
            if (other.logo != null)
                return false;
        } else if (!logo.equals(other.logo))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (registrationDate == null) {
            if (other.registrationDate != null)
                return false;
        } else if (!registrationDate.equals(other.registrationDate))
            return false;
        if (ssoURL == null) {
            if (other.ssoURL != null)
                return false;
        } else if (!ssoURL.equals(other.ssoURL))
            return false;
        return true;
    }
    
}
