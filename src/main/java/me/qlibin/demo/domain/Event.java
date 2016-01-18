package me.qlibin.demo.domain;

import java.io.Serializable;

/**
 * Created by alibin on 1/17/16.
 *
 */
public class Event implements Serializable {

    private Integer id;

    private Integer startTime;

    public Event() {
    }

    public Event(Integer id, Integer startTime) {
        this.id = id;
        this.startTime = startTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

}
