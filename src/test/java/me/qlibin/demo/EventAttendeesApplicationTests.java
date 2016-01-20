package me.qlibin.demo;

import me.qlibin.demo.domain.Event;
import me.qlibin.demo.domain.EventAttendee;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@TestPropertySource(properties = {"start_processor_on_run = false"})
public class EventAttendeesApplicationTests {

	@Autowired
    DataAccessService dataAccessService;

	@Test
	public void contextLoads() {
	}

	@Test
	public void storeEvent() {

        HashSet<Integer> attendees = new HashSet<>(Arrays.asList(1, 3, 4, 6));

        dataAccessService.createOrUpdateEvent(123, 12030, attendees);

        Event event = dataAccessService.getEvent(123);

        assertThat("Start time saved correctly", event.getStartTime(), is(12030));

        List<Event> events = dataAccessService.findEvents(100, 15000, new HashSet<>(Arrays.asList(3, 6)));

        assertTrue("Event filter works", events.stream().anyMatch(event1 -> event1.getId().equals(event.getId())));

	}

}
