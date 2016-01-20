package me.qlibin.demo;

import com.google.common.base.Joiner;
import me.qlibin.demo.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
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
            createEvent(eventId, newStartTime, serializeAttendees(attendees));
        } else {
            log.trace("Update eventId = {}", eventId);
            // if exists update its startTime
            updateEvent(eventId, newStartTime, serializeAttendees(attendees));
        }
    }

    private String serializeAttendees(Set<Integer> attendees) {
        return Joiner.on(' ').join(integersToWords(attendees));
    }

    private String attendeesToQuery(Set<Integer> attendees) {
        return "+" + Joiner.on(" +").join(integersToWords(attendees));
    }

    private Collection<String> integersToWords(Collection<Integer> attendees) {
        return attendees
                .stream()
                .sorted()
                .map(attendeeId -> "att_" + attendeeId)
                .collect(Collectors.toList());
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
        String like = attendeesToQuery(attendees);
        return jdbcTemplate.query(
                "select * from event where startTime >= ? and startTime <= ? and MATCH(attendees) AGAINST(? IN BOOLEAN MODE) > 0 ",
                new Object[]{t1, t2, like},
                (rs, rowNum) -> new Event(rs.getInt("id"), rs.getInt("startTime"), rs.getString("attendees"))
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
                (rs, rowNum) -> new Event(rs.getInt("id"), rs.getInt("startTime"), rs.getString("attendees"))
        ));
    }

    private void createEvent(int eventId, int newStartTime, String attendees) {
        jdbcTemplate.update("insert into event values(?, ?, ?)", eventId, newStartTime, attendees);
    }

    private void updateEvent(int eventId, int newStartTime, String attendees) {
        jdbcTemplate.update("update event set startTime = ?, attendees = ? where id = ?", newStartTime, attendees, eventId);
    }

}
