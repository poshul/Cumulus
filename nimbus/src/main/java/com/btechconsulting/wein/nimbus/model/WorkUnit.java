//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.11.30 at 03:58:49 PM EST 
//


package com.btechconsulting.wein.nimbus.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for WorkUnit complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WorkUnit">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="pointerToMolecule" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="pointerToReceptor" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ownerID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="jobID" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="workUnitID" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="vinaParams" type="{http://www.b-techconsulting.com/cumulus}VinaParams"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WorkUnit", propOrder = {
    "pointerToMolecule",
    "pointerToReceptor",
    "ownerID",
    "jobID",
    "workUnitID",
    "vinaParams"
})
@XmlRootElement
public class WorkUnit {

    @XmlElement(required = true)
    protected String pointerToMolecule;
    @XmlElement(required = true)
    protected String pointerToReceptor;
    @XmlElement(required = true)
    protected String ownerID;
    @XmlElement(required = true)
    protected Integer jobID;
    @XmlElement(required = true)
    protected Integer workUnitID;
    @XmlElement(required = true)
    protected VinaParams vinaParams;

    /**
     * Gets the value of the pointerToMolecule property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPointerToMolecule() {
        return pointerToMolecule;
    }

    /**
     * Sets the value of the pointerToMolecule property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPointerToMolecule(String value) {
        this.pointerToMolecule = value;
    }

    /**
     * Gets the value of the pointerToReceptor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPointerToReceptor() {
        return pointerToReceptor;
    }

    /**
     * Sets the value of the pointerToReceptor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPointerToReceptor(String value) {
        this.pointerToReceptor = value;
    }

    /**
     * Gets the value of the ownerID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOwnerID() {
        return ownerID;
    }

    /**
     * Sets the value of the ownerID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOwnerID(String value) {
        this.ownerID = value;
    }

    /**
     * Gets the value of the jobID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Integer getJobID() {
        return jobID;
    }

    /**
     * Sets the value of the jobID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJobID(Integer value) {
        this.jobID = value;
    }

    /**
     * Gets the value of the workUnitID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Integer getWorkUnitID() {
        return workUnitID;
    }

    /**
     * Sets the value of the workUnitID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWorkUnitID(Integer value) {
        this.workUnitID = value;
    }

    /**
     * Gets the value of the vinaParams property.
     * 
     * @return
     *     possible object is
     *     {@link VinaParams }
     *     
     */
    public VinaParams getVinaParams() {
        return vinaParams;
    }

    /**
     * Sets the value of the vinaParams property.
     * 
     * @param value
     *     allowed object is
     *     {@link VinaParams }
     *     
     */
    public void setVinaParams(VinaParams value) {
        this.vinaParams = value;
    }

}
