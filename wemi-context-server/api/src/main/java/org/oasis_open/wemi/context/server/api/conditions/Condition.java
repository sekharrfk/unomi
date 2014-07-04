package org.oasis_open.wemi.context.server.api.conditions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node in the segment definition expression tree
 */
@XmlRootElement
public class Condition {

    ConditionType conditionType;
    String conditionTypeId;
    Map<String, ParameterValue> parameterValues = new HashMap<String, ParameterValue>();

    public Condition() {
    }

    @XmlTransient
    public ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionType conditionType) {
        this.conditionType = conditionType;
        this.conditionTypeId = conditionType.id;
    }

    @XmlElement(name="type")
    public String getConditionTypeId() {
        return conditionTypeId;
    }

    public void setConditionTypeId(String conditionTypeId) {
        this.conditionTypeId = conditionTypeId;
    }

    public Map<String, ParameterValue> getParameterValues() {
        return parameterValues;
    }

    public void setParameterValues(Map<String, ParameterValue> parameterValues) {
        this.parameterValues = parameterValues;
    }

}