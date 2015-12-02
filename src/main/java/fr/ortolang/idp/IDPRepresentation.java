package fr.ortolang.idp;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.codec.digest.DigestUtils;

@XmlRootElement(name="idp")
@XmlType(name="idp", propOrder={"entityId", "alias", "name", "description", "registrationDate", "ssoURL", "logo"})
public class IDPRepresentation {

    private String entityId;
    private String alias;
    private String name;
    private String description;
    private String logo;
    private String certificate;
    private Date registrationDate;
    private String ssoURL;

    public IDPRepresentation() {
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
        this.certificate = certificate;
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

}
