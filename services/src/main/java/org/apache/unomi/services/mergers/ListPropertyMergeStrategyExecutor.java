package org.apache.unomi.services.mergers;

import org.apache.unomi.api.Profile;
import org.apache.unomi.api.PropertyMergeStrategyExecutor;
import org.apache.unomi.api.PropertyType;
import org.apache.unomi.persistence.spi.PropertyHelper;

import java.util.*;

public class ListPropertyMergeStrategyExecutor implements PropertyMergeStrategyExecutor  {
    @Override
    public boolean mergeProperty(String propertyName, PropertyType propertyType, List<Profile> profilesToMerge, Profile targetProfile) {
        boolean modified = false;
        List<Object> list = new ArrayList<>(convertObjectToList(targetProfile.getNestedProperty(propertyName)));
        for(Profile profileToMerge: profilesToMerge) {
            if (profileToMerge.getNestedProperty(propertyName) != null && profileToMerge.getNestedProperty(propertyName).toString().length() > 0) {
                List<?> x = convertObjectToList(profileToMerge.getNestedProperty(propertyName));
                list.addAll(x);
                modified = true;
            }
        }
        PropertyHelper.setProperty(targetProfile, "properties." + propertyName, list, "alwaysSet");
        return modified;
    }

    public static List<?> convertObjectToList(Object obj) {
        List<?> list = new ArrayList<>();
        if (obj instanceof Collection) {
            list = new ArrayList<>((Collection<?>)obj);
        }
        return list;
    }
}
