package me.qlibin.demo.domain;

import java.io.Serializable;

/**
 * Created by alibin on 1/18/16.
 */
public class EventAttendee implements Serializable {

    private Integer eventId;
    private Integer attendeeId;

    public EventAttendee(Integer eventId, Integer attendeeId) {
        this.eventId = eventId;
        this.attendeeId = attendeeId;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Integer attendeeId) {
        this.attendeeId = attendeeId;
    }
}
