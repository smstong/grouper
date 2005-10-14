package edu.internet2.middleware.grouper;

import java.io.Serializable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/** 
 * Group math factors.
 * @author blair christensen.
 *     
*/
public class Factor implements Serializable {

    /** identifier field */
    private String id;

    /** persistent field */
    private String node_a_id;

    /** persistent field */
    private String node_b_id;

    /** nullable persistent field */
    private Integer version;

    /** full constructor */
    public Factor(String node_a_id, String node_b_id, Integer version) {
        this.node_a_id = node_a_id;
        this.node_b_id = node_b_id;
        this.version = version;
    }

    /** default constructor */
    public Factor() {
    }

    /** minimal constructor */
    public Factor(String node_a_id, String node_b_id) {
        this.node_a_id = node_a_id;
        this.node_b_id = node_b_id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** 
     * Get node a.
     *       
     */
    public String getNode_a_id() {
        return this.node_a_id;
    }

    public void setNode_a_id(String node_a_id) {
        this.node_a_id = node_a_id;
    }

    /** 
     * Get node b.
     *       
     */
    public String getNode_b_id() {
        return this.node_b_id;
    }

    public void setNode_b_id(String node_b_id) {
        this.node_b_id = node_b_id;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String toString() {
        return new ToStringBuilder(this)
            .append("node_a_id", getNode_a_id())
            .append("node_b_id", getNode_b_id())
            .toString();
    }

    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof Factor) ) return false;
        Factor castOther = (Factor) other;
        return new EqualsBuilder()
            .append(this.getNode_a_id(), castOther.getNode_a_id())
            .append(this.getNode_b_id(), castOther.getNode_b_id())
            .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .append(getNode_a_id())
            .append(getNode_b_id())
            .toHashCode();
    }

}
