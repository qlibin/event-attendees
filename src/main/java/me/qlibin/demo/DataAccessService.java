package me.qlibin.demo;

import me.qlibin.demo.domain.Event;
import me.qlibin.demo.domain.EventAttendee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by alibin on 1/18/16.
 * Data access methods
 */
@Service
public class DataAccessService {

    private static final Logger log = LoggerFactory.getLogger(DataAccessService.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * If event with eventId doesn't exist it creates one with newStartTime and set of attendees
     * Otherwise it updates existing event with newStartTime and replaces its attendees with the new set
     * @param eventId event id
     * @param newStartTime startTime of event
     * @param attendees event attendee set
     */
    public void createOrUpdateEvent(int eventId, int newStartTime, Set<Integer> attendees) {
        // check if eventId exists
        log.trace("Check if eventId = {} exists", eventId);
        if (!hasEvent(eventId)) {
            log.trace("Create new eventId = {}", eventId);
            // if not exists create it
            createEvent(eventId, newStartTime);
        } else {
            log.trace("Update eventId = {}", eventId);
            // if exists update its startTime
            updateEvent(eventId, newStartTime);
            log.trace("Remove attendees from eventId = {}", eventId);
            // remove all event_attendees with eventId
            removeEventAttendees(eventId);
        }
        log.trace("Add attendees to eventId = {}", eventId);
        // write new attendee set
        if (attendees.size() > 0) {
            createEventAttendees(eventId, attendees);
        }
    }

    /**
     * Request all events with startTime in [t1, t2] interval and all attendees from {@code attendees}
     * Big size of attendees collection impacts performance very much
     * cause each attendees collection item adds another join to sql-query
     * @param t1 startTime lower bound
     * @param t2 startTime higher bound
     * @param attendees attendees of event
     * @return List of requested events
     */
    public List<Event> findEvents(int t1, int t2, Set<Integer> attendees) {
        StringBuilder join = new StringBuilder();
        List<Object> args = new ArrayList<>(attendees.stream().sorted().collect(Collectors.toList()));
        // in order to filter events with all attendees from request
        // join event_attendee table as many times as attendees count in a request
        args.forEach(attendee -> {
            String alias = "ea" + attendee;
            join.append("join event_attendee ").append(alias)
                    .append(" on e.id=").append(alias).append(".eventId and ")
                    .append(alias).append(".attendeeId=? ");
        });
        args.add(t1);
        args.add(t2);
        return jdbcTemplate.query(
                "SELECT e.* FROM event e " + join.toString() +
                        "WHERE e.startTime >= ? and e.startTime <= ?",
                args.toArray(),
                (rs, rowNum) -> new Event(rs.getInt("id"), rs.getInt("startTime"))
        );
    }

    @Cacheable(value = "hasEventCache")
    public boolean hasEvent(int eventId) {
        List result = jdbcTemplate.queryForList(
                "SELECT * FROM event WHERE id = ?", eventId);
        return result.size() > 0;
    }

    public Event getEvent(int eventId) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT * FROM event WHERE id = ?", new Object[] { eventId },
                (rs, rowNum) -> new Event(rs.getInt("id"), rs.getInt("startTime"))
        ));
    }

    public List<EventAttendee> getEventAttendees(int eventId) {
        return jdbcTemplate.query(
                "SELECT * FROM event_attendee " +
                        "WHERE eventId = ? ",
                new Object[] {eventId},
                (rs, rowNum) -> new EventAttendee(rs.getInt("eventId"), rs.getInt("attendeeId"))
        );
    }

    private void createEvent(int eventId, int newStartTime) {
        jdbcTemplate.update("insert into event values(?, ?)", eventId, newStartTime);
    }

    private void updateEvent(int eventId, int newStartTime) {
        jdbcTemplate.update("update event set startTime = ? where id = ?", newStartTime, eventId);
    }

    private void createEventAttendees(int eventId, Set<Integer> attendees) {
        jdbcTemplate.batchUpdate(
                "insert into event_attendee values(?, ?)",
                attendees.stream().sorted().map(attendeeId -> new Object[] {eventId, attendeeId})
                        .collect(Collectors.toList())
        );
    }

    private void removeEventAttendees(int eventId) {
        jdbcTemplate.update("delete from event_attendee where eventId = ?", eventId);
    }

}
