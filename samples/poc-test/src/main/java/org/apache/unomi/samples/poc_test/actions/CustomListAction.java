package org.apache.unomi.samples.poc_test.actions;

import org.apache.unomi.api.*;
import org.apache.unomi.api.actions.Action;
import org.apache.unomi.api.actions.ActionExecutor;
import org.apache.unomi.api.services.EventService;
import org.apache.unomi.api.services.ProfileService;

import java.util.List;
import java.util.Map;

public class CustomListAction implements ActionExecutor {

    private ProfileService service;

    @Override
    public int execute(Action action, Event event) {
        final Profile profile = event.getProfile();
        List<String> myList = (List<String>) profile.getProperty("myList");

        if (myList == null) {
            PropertyType propertyType = new PropertyType(new Metadata(event.getScope(), "myList", "myList", "List of all porducts viewed"));
            propertyType.setValueTypeId("list");
            propertyType.setTarget("target");
            service.setPropertyType(propertyType);
        }

        final List<String> updatedList = updateMyList(event, myList);

        profile.setProperty("myList", updatedList);

        return EventService.PROFILE_UPDATED;
    }

    private List<String> updateMyList(Event event, List<String> myList) {
        final Map<String, Object> properties = event.getProperties();

        String productId = (String) properties.get("productId");
        if (myList.contains(productId)) {
            myList.remove(productId);
        }
        myList.add(0, productId);

        return myList;
    }
}
