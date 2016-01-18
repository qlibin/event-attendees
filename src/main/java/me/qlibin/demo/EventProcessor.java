package me.qlibin.demo;

import com.google.common.util.concurrent.Striped;
import me.qlibin.demo.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

/**
 * Created by alibin on 1/18/16.
 * Starts two pools of threads: Readers and Writers
 * Writers create events with random properties
 * Readers perform random selections from events
 */
@Service
public class EventProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

    @Autowired
    DataAccessService dataAccessService;

    @Value("${writers.count}")
    private Integer writersCount;

    @Value("${readers.count}")
    private Integer readersCount;

    @Value("${log.rate.every}")
    private Integer logEvery; // seconds between rate log records

    // performance counters
    private AtomicLong writesCounter = new AtomicLong(0);
    private AtomicLong readsCounter = new AtomicLong(0);
    private AtomicLong writesInPeriod = new AtomicLong(0);
    private AtomicLong readsInPeriod = new AtomicLong(0);

    private Striped<Lock> locks;

    public void start() throws InterruptedException {

        // locks for concurrent event creating
        locks = Striped.lock(writersCount * 2);

        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executorWriters = Executors.newFixedThreadPool(writersCount);

        for (int i = 0; i < writersCount; i++) {
            final int taskNumber = i + 1;
            log.info("Prepare writer " + taskNumber);
            executorWriters.submit((Runnable) () -> {
                try {
                    start.await();
                    log.info("Start writer " + taskNumber);
                    new Thread(new Writer()).start();
                } catch (InterruptedException e) {
                    log.error("Writer start exception", e);
                }
            });
        }

        ExecutorService executorReaders = Executors.newFixedThreadPool(readersCount);

        for (int i = 0; i < readersCount; i++) {
            final int taskNumber = i + 1;
            log.info("Prepare reader " + taskNumber);
            executorReaders.submit((Runnable) () -> {
                try {
                    start.await();
                    log.info("Start reader " + taskNumber);
                    new Thread(new Reader()).start();
                } catch (InterruptedException e) {
                    log.error("Reader start exception", e);
                }
            });
        }

        start.countDown(); // start all workers

        startRateLog(); // start periodically log writers and readers capacity

    }

    /**
     * Log Readers and Writers rate
     * @throws InterruptedException
     */
    private void startRateLog() throws InterruptedException {

        long writesCnt = writesCounter.get();
        long readsCnt = readsCounter.get();

        while (!Thread.currentThread().isInterrupted()) {

            Thread.sleep(logEvery * 1000);

            writesInPeriod.set(writesCounter.get() - writesCnt);
            readsInPeriod.set(readsCounter.get() - readsCnt);
            writesCnt = writesCounter.get();
            readsCnt = readsCounter.get();

            log.info(
                    "\n=================================="+
                    "\n\tWrites per second: {}"+
                    "\n\tReads per second: {}"+
                    "\n==================================\n",
                    writesInPeriod.get() / logEvery,
                    readsInPeriod.get() / logEvery
            );
        }
    }

    @Value("${events.max}")
    private Integer maxEventCount;

    @Value("${time.max}")
    private Integer maxTime;

    @Value("${attendees.max}")
    private Integer maxAttendeeId;

    @Value("${event_attendees.max}")
    private Integer maxEventAttendees;

    public static int getRandom(int n) {
        return ThreadLocalRandom.current().nextInt(1, n + 1);
    }

    public static int getRandom(int s, int n) {
        return ThreadLocalRandom.current().nextInt(s, n + 1);
    }

    /**
     * Writer worker
     */
    public class Writer implements Runnable {

        @Override
        public void run() {
            // in an infinite loop
            // pick a random event id = 1..maxEventCount
            // if event does't exists creates one
            // modifies startTime of the event with random value from range 1..maxTime
            // make attendee set to a random subset of 1..maxAttendeeId of max size maxEventAttendees
            while (!Thread.currentThread().isInterrupted()) {
                if (iteration()) {
                    writesCounter.incrementAndGet();
                }
            }
        }

        private boolean iteration() {
            int eventId = getRandom(maxEventCount);
            Lock lock = locks.get(eventId);
            if (lock.tryLock()) {
                try {
                    int newStartTime = getRandom(maxTime);
                    Set<Integer> attendees = new HashSet<>();
                    for (int i = 0; i < getRandom(maxEventAttendees); i++) {
                        attendees.add(getRandom(maxAttendeeId));
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Write event {}", eventId);
                    }
                    dataAccessService.createOrUpdateEvent(eventId, newStartTime, attendees);
                } finally {
                    lock.unlock();
                }
                return true;
            } else {
                log.debug("Skip event with Id = {} ", eventId);
                return false;
            }
        }

    }

    /**
     * Reader worker
     */
    public class Reader implements Runnable {

        @Override
        public void run() {
            // in an infinite loop
            // pick a random time range t1 < t2 <= maxTime
            // pick a random subset of attendees
            // find all events from the time range and which contains the subset of attendees
            while (!Thread.currentThread().isInterrupted()) {
                int t1 = getRandom(maxTime - 1);
                int t2 = getRandom(t1 + 1, maxTime);
                Set<Integer> attendees = new HashSet<>();
                for (int i = 0; i < getRandom(maxEventAttendees); i++) {
                    attendees.add(getRandom(maxAttendeeId));
                }
                List<Event> events = dataAccessService.getEvents(t1, t2, attendees);
                if (log.isDebugEnabled()) {
                    log.debug("Find events in a range of [{}, {}] with attendees ({})\n\t Events found: {}",
                            t1, t2, Arrays.toString(attendees.toArray()), events.size());
                }
                readsCounter.incrementAndGet();
            }
        }

    }

}
