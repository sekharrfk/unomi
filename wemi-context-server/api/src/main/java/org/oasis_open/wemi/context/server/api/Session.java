package org.oasis_open.wemi.context.server.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Session extends Item implements TimestampedItem {

    public static final String ITEM_TYPE = "session";

    private String userId;

    private User user;

    private Map<String,Object> properties;

    private Date timeStamp;

    private Date lastEventDate;

    private long duration = 0;

    public Session() {
    }

    public Session(String itemId, User user, Date timeStamp) {
        super(itemId);
        this.user = user;
        this.userId = user.getId();
        properties = new HashMap<String,Object>();
        this.timeStamp = timeStamp;
    }

    public String getId() {
        return itemId;
    }

    public void setId(String id) {
        this.itemId = id;
    }

    public String getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.userId = user.getId();
        this.user = user;
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public Date getLastEventDate() {
        return lastEventDate;
    }

    public void setLastEventDate(Date lastEventDate) {
        this.lastEventDate = lastEventDate;
        if (lastEventDate != null) {
            duration = lastEventDate.getTime() - timeStamp.getTime();
        }
    }

    public long getDuration() {
        return duration;
    }
}
